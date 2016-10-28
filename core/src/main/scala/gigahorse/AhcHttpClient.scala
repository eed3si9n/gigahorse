/*
 * Original implementation (C) 2009-2016 Lightbend Inc. (https://www.lightbend.com).
 * Adapted and extended in 2016 by Eugene Yokota
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gigahorse

import scala.collection.JavaConverters._
import java.io.{ File, UnsupportedEncodingException }
import java.nio.charset.Charset
import scala.concurrent.{ Future, Promise, ExecutionContext }
import org.asynchttpclient.{ Response => XResponse, Request => XRequest, Realm => XRealm, _ }
import org.asynchttpclient.AsyncHandler.{ State => XState }
import org.asynchttpclient.proxy.{ ProxyServer => XProxyServer }
import org.asynchttpclient.util.HttpUtils
import org.asynchttpclient.Realm.{ AuthScheme => XAuthScheme }
import io.netty.handler.codec.http.QueryStringDecoder
import org.asynchttpclient.ws.WebSocketUpgradeHandler

class AhcHttpClient(config: AsyncHttpClientConfig) extends HttpClient {
  import AhcHttpClient._
  private val asyncHttpClient = new DefaultAsyncHttpClient(config)
  def underlying[A]: A = asyncHttpClient.asInstanceOf[A]
  def close(): Unit = asyncHttpClient.close()
  override def toString: String =
    s"""AchHttpClient($config)"""

  def this(config: Config) =
    this(AhcConfig.buildConfig(config))

  /** Runs the request and return a Future of Response. */
  def run(request: Request): Future[Response] =
    process(request, OkHandler[Response](identity))

  /** Runs the request and return a Future of A. */
  def run[A](request: Request, f: Response => A): Future[A] =
    process(request, OkHandler[A](f))

  /** Runs the request and return a Future of Either a Response or a Throwable. */
  def run[A](request: Request, lifter: FutureLifter[A])(implicit ec: ExecutionContext): Future[Either[Throwable, A]] =
    lifter.run(run(request))

  def download(request: Request, file: File): Future[File] =
    process(request, new OkHandler[File](_ => ???) {
      import java.io.FileOutputStream
      val out = new FileOutputStream(file).getChannel
      override def onBodyPartReceived(content: HttpResponseBodyPart): State = {
        out.write(content.getBodyByteBuffer)
        State.Continue
      }
      override def onCompleted(response: Response) = {
        out.close()
        file
      }
    })

  /** Executes the request and return a Future of Response. Does not error on non-OK response. */
  def process(request: Request): Future[Response] =
    process(request, FunctionHandler[Response](identity))

  /** Executes the request and return a Future of A. Does not error on non-OK response. */
  def process[A](request: Request, f: Response => A): Future[A] =
    process(request, FunctionHandler[A](f))

  /** Executes the request and return a Future of Either a Response or a Throwable. Does not error on non-OK response. */
  def process[A](request: Request, lifter: FutureLifter[A])(implicit ec: ExecutionContext): Future[Either[Throwable, A]] =
    lifter.run(process(request))

  /** Executes the request. Does not error on non-OK response. */
  def process[A](request: Request, handler: CompletionHandler[A]): Future[A] =
    {
      val result = Promise[A]()
      val xrequest = buildRequest(request)
      asyncHttpClient.executeRequest(xrequest, new AsyncHandler[XResponse]() {
        override def onStatusReceived(status: HttpResponseStatus): XState = {
          fromState(handler.onStatusReceived(status))
        }
        override def onHeadersReceived(headers: HttpResponseHeaders): XState = {
          fromState(handler.onHeadersReceived(headers))
        }
        override def onBodyPartReceived(bodyPart: HttpResponseBodyPart): XState = {
          fromState(handler.onBodyPartReceived(bodyPart))
        }
        override def onCompleted(): XResponse = {
          onCompleted(handler.builder.build())
        }
        def onCompleted(response: XResponse): XResponse = {
          result.success(handler.onCompleted(new AhcResponse(response)))
          response
        }
        override def onThrowable(t: Throwable): Unit = {
          result.failure(t)
        }
      })
      result.future
    }

  /** Open a websocket connection. */
  def websocket(request: Request)(handler: PartialFunction[WebSocketEvent, Unit]): Future[WebSocket] =
    {
      val result = Promise[WebSocket]()
      val xrequest = buildRequest(request)
      val upgradeHandler = new WebSocketUpgradeHandler.Builder()
      asyncHttpClient.executeRequest(xrequest, upgradeHandler.addWebSocketListener(new WebSocketListener(handler, result)).build())
      result.future
    }

  /**
   * Creates and returns an AHC request, running all operations on it.
   */
  def buildRequest(request: Request): XRequest = {
    import request._
    // The builder has a bunch of mutable state and is VERY fiddly, so
    // should not be exposed to the outside world.

    val disableUrlEncoding: Option[Boolean] = None
    val builder = disableUrlEncoding.map { disableEncodingFlag =>
      new RequestBuilder(method, disableEncodingFlag)
    }.getOrElse {
      new RequestBuilder(method)
    }

    // Set the URL.
    builder.setUrl(url)

    // auth
    authOpt.foreach { data =>
      val realm = buildRealm(data)
      builder.setRealm(realm)
    }

    // queries
    for {
      (key, values) <- queryString
      value <- values
    } builder.addQueryParam(key, value)

    // Configuration settings on the builder, if applicable
    virtualHostOpt.foreach(builder.setVirtualHost)
    followRedirectsOpt.foreach(builder.setFollowRedirect)

    proxyServerOpt.foreach(p => builder.setProxyServer(buildProxy(p)))

    requestTimeoutOpt foreach { x =>
      builder.setRequestTimeout(AhcConfig.toMillis(x))
    }

    val (builderWithBody, updatedHeaders) = body match {
      case b: EmptyBody => (builder, request.headers)
      case b: FileBody =>
        import org.asynchttpclient.request.body.generator.FileBodyGenerator
        val bodyGenerator = new FileBodyGenerator(b.file)
        builder.setBody(bodyGenerator)
        (builder, request.headers)
      case b: InMemoryBody =>
        val ct: String = contentType.getOrElse("text/plain")

        val h = try {
          // Only parse out the form body if we are doing the signature calculation.
          if (ct.contains(ContentTypes.FORM) && signatureOpt.isDefined) {
            // If we are taking responsibility for setting the request body, we should block any
            // externally defined Content-Length field (see #5221 for the details)
            val filteredHeaders = request.headers.filterNot { case (k, v) => k.equalsIgnoreCase(HeaderNames.CONTENT_LENGTH) }

            // extract the content type and the charset
            val charset =
              Option(HttpUtils.parseCharset(ct)).getOrElse {
                // NingWSRequest modifies headers to include the charset, but this fails tests in Scala.
                //val contentTypeList = Seq(ct + "; charset=utf-8")
                //possiblyModifiedHeaders = this.headers.updated(HeaderNames.CONTENT_TYPE, contentTypeList)
                Charset.forName("utf-8")
              }

            // Get the string body given the given charset...
            val stringBody = new String(b.bytes, charset)
            // The Ning signature calculator uses request.getFormParams() for calculation,
            // so we have to parse it out and add it rather than using setBody.

            val params = for {
              (key, values) <- new QueryStringDecoder("/?" + stringBody, charset).parameters.asScala.toList // FormUrlEncodedParser.parse(stringBody).toSeq
              value <- values.asScala.toList
            } yield new Param(key, value)
            builder.setFormParams(params.asJava)
            filteredHeaders
          } else {
            builder.setBody(b.bytes)
            request.headers
          }
        } catch {
          case e: UnsupportedEncodingException =>
            throw new RuntimeException(e)
        }

        (builder, h)
      // case StreamedBody(bytes) =>
      //  (builder, request.headers)
    }

    // headers
    for {
      header <- updatedHeaders
      value <- header._2
    } builder.addHeader(header._1, value)

    // Set the signature calculator.
    signatureOpt.map {
      case signatureCalculator: org.asynchttpclient.SignatureCalculator =>
        builderWithBody.setSignatureCalculator(signatureCalculator)
      case _ =>
        throw new IllegalStateException("Unknown signature calculator found: use a class that implements SignatureCalculator")
    }
    builderWithBody.build()
  }
}

object AhcHttpClient {
  def toState(x: XState): State =
    x match {
      case XState.CONTINUE => State.Continue
      case XState.ABORT    => State.Abort
      case XState.UPGRADE  => State.Upgrade
    }

  def fromState(state: State): XState =
    state match {
      case State.Continue => XState.CONTINUE
      case State.Abort    => XState.ABORT
      case State.Upgrade  => XState.UPGRADE
    }

  def buildRealm(auth: Realm): XRealm =
    {
      import org.asynchttpclient.uri.Uri
      val builder = new XRealm.Builder(auth.username, auth.password)
      builder.setScheme(auth.scheme match {
        case AuthScheme.Digest   => XAuthScheme.DIGEST
        case AuthScheme.Basic    => XAuthScheme.BASIC
        case AuthScheme.NTLM     => XAuthScheme.NTLM
        case AuthScheme.SPNEGO   => XAuthScheme.SPNEGO
        case AuthScheme.Kerberos => XAuthScheme.KERBEROS
        case _ => throw new RuntimeException("Unknown scheme " + auth.scheme)
      })
      builder.setUsePreemptiveAuth(auth.usePreemptiveAuth)
      auth.realmNameOpt  foreach { builder.setRealmName }
      auth.nonceOpt      foreach { builder.setNonce }
      auth.algorithmOpt  foreach { builder.setAlgorithm }
      auth.responseOpt   foreach { builder.setResponse }
      auth.opaqueOpt     foreach { builder.setOpaque }
      auth.qopOpt        foreach { builder.setQop }
      auth.ncOpt         foreach { builder.setNc }
      auth.uriOpt        foreach { x => builder.setUri(Uri.create(x.toString)) }
      auth.methodNameOpt foreach { builder.setMethodName }
      auth.charsetOpt    foreach { x => builder.setCharset(x) }
      auth.ntlmDomainOpt foreach { builder.setNtlmDomain }
      auth.ntlmHostOpt   foreach { builder.setNtlmHost }
      builder.setUseAbsoluteURI(auth.useAbsoluteURI)
      builder.setOmitQuery(auth.omitQuery)
      builder.build()
    }

  def buildProxy(proxy: ProxyServer): XProxyServer =
    {
      new XProxyServer(proxy.host, proxy.port, proxy.securedPort.getOrElse(proxy.port),
        proxy.authOpt.map(buildRealm).getOrElse(null),
        proxy.nonProxyHosts.asJava)
    }
}
