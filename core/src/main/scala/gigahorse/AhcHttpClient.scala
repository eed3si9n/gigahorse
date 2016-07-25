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
import java.nio.charset.{ Charset, StandardCharsets }
import scala.concurrent.{ Future, Promise }
import com.ning.http.client.{ Response => XResponse, Request => XRequest, ProxyServer => XProxyServer, Realm => XRealm, _ }
import com.ning.http.client.AsyncHandler.{ STATE => XState }
import com.ning.http.util.AsyncHttpProviderUtils
import com.ning.http.client.Realm.{ RealmBuilder, AuthScheme => XAuthScheme }
import org.jboss.netty.handler.codec.http.{ HttpHeaders, QueryStringDecoder }

class AhcHttpClient(config: AsyncHttpClientConfig) extends HttpClient {
  import AhcHttpClient._
  private val asyncHttpClient = new AsyncHttpClient(config)
  def underlying[A]: A = asyncHttpClient.asInstanceOf[A]
  def close(): Unit = asyncHttpClient.close()
  override def toString: String =
    s"""AchHttpClient($config)"""

  def this(config: Config) =
    this(AhcConfig.buildConfig(config))

  def run(request: Request): Future[Response] = process(request, runHandler)
  private[gigahorse] val runHandler: CompletionHandler[Response] = new CompletionHandler[Response] {
    override def onCompleted(response: Response) = response
  }

  def download(request: Request, file: File): Future[File] =
    process(request, new CompletionHandler[File] {
      import java.io.FileOutputStream
      val out = new FileOutputStream(file)
      override def onBodyPartReceived(content: HttpResponseBodyPart): State = {
        content.writeTo(out)
        State.Continue
      }
      override def onCompleted(response: Response) = file
    })

  def process[A](request: Request, handler: CompletionHandler[A]): Future[A] =
    {
      import com.ning.http.client.AsyncCompletionHandler
      val result = Promise[A]()
      val xrequest = buildRequest(request)
      asyncHttpClient.executeRequest(xrequest, new AsyncHandler[XResponse]() {
        def onCompleted(response: XResponse): XResponse = {
          result.success(handler.onCompleted(new AhcResponse(response)))
          response
        }
        override def onCompleted(): XResponse = {
          onCompleted(handler.builder.build())
        }
        override def onThrowable(t: Throwable): Unit = {
          result.failure(t)
        }
        override def onBodyPartReceived(bodyPart: HttpResponseBodyPart): XState = {
          fromState(handler.onBodyPartReceived(bodyPart))
        }
        override def onStatusReceived(status: HttpResponseStatus): XState = {
          fromState(handler.onStatusReceived(status))
        }
        override def onHeadersReceived(headers: HttpResponseHeaders): XState = {
          fromState(handler.onHeadersReceived(headers))
        }
      })
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
    followRedirectsOpt.foreach(builder.setFollowRedirects)

    proxyServerOpt.foreach(p => builder.setProxyServer(buildProxy(p)))

    requestTimeoutOpt foreach { x =>
      builder.setRequestTimeout(AhcConfig.toMillis(x))
    }

    val (builderWithBody, updatedHeaders) = body match {
      case b: EmptyBody => (builder, request.headers)
      case b: FileBody =>
        import com.ning.http.client.generators.FileBodyGenerator
        val bodyGenerator = new FileBodyGenerator(b.file)
        builder.setBody(bodyGenerator)
        (builder, request.headers)
      case b: InMemoryBody =>
        val ct: String = contentType(request).getOrElse("text/plain")

        val h = try {
          // Only parse out the form body if we are doing the signature calculation.
          if (ct.contains(HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED) && signatureOpt.isDefined) {
            // If we are taking responsibility for setting the request body, we should block any
            // externally defined Content-Length field (see #5221 for the details)
            val filteredHeaders = request.headers.filterNot { case (k, v) => k.equalsIgnoreCase(HttpHeaders.Names.CONTENT_LENGTH) }

            // extract the content type and the charset
            val charset = Charset.forName(
              Option(AsyncHttpProviderUtils.parseCharset(ct)).getOrElse {
                // NingWSRequest modifies headers to include the charset, but this fails tests in Scala.
                //val contentTypeList = Seq(ct + "; charset=utf-8")
                //possiblyModifiedHeaders = this.headers.updated(HttpHeaders.Names.CONTENT_TYPE, contentTypeList)
                "utf-8"
              }
            )

            // Get the string body given the given charset...
            val stringBody = new String(b.bytes, charset)
            // The Ning signature calculator uses request.getFormParams() for calculation,
            // so we have to parse it out and add it rather than using setBody.

            val params = for {
              (key, values) <- new QueryStringDecoder("/?" + stringBody, charset).getParameters.asScala.toList // FormUrlEncodedParser.parse(stringBody).toSeq
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
      case signatureCalculator: com.ning.http.client.SignatureCalculator =>
        builderWithBody.setSignatureCalculator(signatureCalculator)
      case _ =>
        throw new IllegalStateException("Unknown signature calculator found: use a class that implements SignatureCalculator")
    }
    builderWithBody.build()
  }
}

object AhcHttpClient {
  val utf8 = Charset.forName("UTF-8")
  private[gigahorse] def setBodyString(request: Request, body: String, charset: Charset): Request =
    request.withBody(InMemoryBody(body.getBytes(charset)))

  private[gigahorse] def setBody[A: HttpWrite](request: Request, body: A): Request =
    {
      val w = implicitly[HttpWrite[A]]
      val r = request.withBody(InMemoryBody(w.toByteArray(body)))
      (w.contentType, contentType(r)) match {
        case (None, _)    => r
        case (_, Some(_)) => r
        case (Some(x), _) => r.withHeaders(r.headers.updated(HttpHeaders.Names.CONTENT_TYPE, x :: Nil))
      }
    }

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

  def contentType(request: Request): Option[String] =
    request.headers.find(p => p._1 == HttpHeaders.Names.CONTENT_TYPE).map {
      case (header, values) =>
        values.head
    }

  def buildRealm(auth: Realm): XRealm =
    {
      import com.ning.http.client.uri.Uri
      val builder = new RealmBuilder
      builder.setScheme(auth.scheme match {
        case AuthScheme.Digest   => XAuthScheme.DIGEST
        case AuthScheme.Basic    => XAuthScheme.BASIC
        case AuthScheme.NTLM     => XAuthScheme.NTLM
        case AuthScheme.SPNEGO   => XAuthScheme.SPNEGO
        case AuthScheme.Kerberos => XAuthScheme.KERBEROS
        case AuthScheme.None     => XAuthScheme.NONE
        case _ => throw new RuntimeException("Unknown scheme " + auth.scheme)
      })
      builder.setPrincipal(auth.username)
      builder.setPassword(auth.password)
      builder.setUsePreemptiveAuth(auth.usePreemptiveAuth)
      auth.realmNameOpt foreach { builder.setRealmName }
      auth.nonceOpt foreach { builder.setNonce }
      auth.algorithmOpt foreach { builder.setAlgorithm }
      auth.responseOpt foreach { builder.setResponse }
      auth.opaqueOpt foreach { builder.setOpaque }
      auth.qopOpt foreach { builder.setQop }
      auth.ncOpt foreach { builder.setNc }
      auth.uriOpt foreach { x => builder.setUri(Uri.create(x.toString)) }
      auth.methodNameOpt foreach { builder.setMethodName }
      auth.charsetOpt foreach { x => builder.setCharset(x) }
      auth.ntlmDomainOpt foreach { builder.setNtlmDomain }
      auth.ntlmHostOpt foreach { builder.setNtlmHost }
      builder.setUseAbsoluteURI(auth.useAbsoluteURI)
      builder.setOmitQuery(auth.omitQuery)
      builder.build()
    }

  def buildProxy(proxy: ProxyServer): XProxyServer =
    {
      import com.ning.http.client.ProxyServer.Protocol
      val protocol = (for {
        auth <- proxy.authOpt
        uri <- auth.uriOpt
        s <- Option(uri.getScheme)
      } yield s).getOrElse("http").toLowerCase match {
        case "http"     => Protocol.HTTP
        case "https"    => Protocol.HTTPS
        case "kerberos" => Protocol.KERBEROS
        case "ntlm"     => Protocol.NTLM
        case "spnego"   => Protocol.SPNEGO
        case s          => throw new RuntimeException("Unrecognized protocol " + s)
      }
      val p = new XProxyServer(protocol, proxy.host, proxy.port,
        proxy.authOpt.map(_.username).orNull,
        proxy.authOpt.map(_.password).orNull)
      proxy.nonProxyHosts foreach { h =>
        p.addNonProxyHost(h)
      }
      proxy.authOpt foreach { auth =>
        auth.ntlmDomainOpt foreach {p.setNtlmDomain}
        auth.ntlmHostOpt foreach { p.setNtlmHost }
        auth.charsetOpt foreach { p.setCharset }
      }
      p
    }
}
