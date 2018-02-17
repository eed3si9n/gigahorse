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
package support.asynchttpclient

import scala.collection.JavaConverters._
import java.io.{ File, UnsupportedEncodingException }
import java.nio.charset.Charset
import scala.concurrent.{ Future, Promise, ExecutionContext }
import shaded.ahc.org.asynchttpclient.{ Response => XResponse, Request => XRequest, Realm => XRealm, SignatureCalculator => XSignatureCalculator, _ }
import shaded.ahc.org.asynchttpclient.AsyncHandler.{ State => XState }
import shaded.ahc.org.asynchttpclient.handler.StreamedAsyncHandler
import shaded.ahc.org.asynchttpclient.proxy.{ ProxyServer => XProxyServer }
import shaded.ahc.org.asynchttpclient.util.HttpUtils
import shaded.ahc.org.asynchttpclient.Realm.{ AuthScheme => XAuthScheme }
import shaded.ahc.io.netty.handler.codec.http.QueryStringDecoder
import shaded.ahc.org.asynchttpclient.ws.WebSocketUpgradeHandler
import org.reactivestreams.Publisher
import DownloadHandler.asFile

class AhcHttpClient(config: AsyncHttpClientConfig) extends ReactiveHttpClient {
  import AhcHttpClient._
  private val asyncHttpClient = new DefaultAsyncHttpClient(config)
  def underlying[A]: A = asyncHttpClient.asInstanceOf[A]
  def close(): Unit = asyncHttpClient.close()
  override def toString: String =
    s"""AchHttpClient($config)"""

  def this(config: Config) =
    this(AhcConfig.buildConfig(config))

  /** Runs the request and return a Future of FullResponse. */
  def run(request: Request): Future[FullResponse] =
    processFull(request, OkHandler[FullResponse](identity))

  /** Runs the request and return a Future of A. */
  def run[A](request: Request, f: FullResponse => A): Future[A] =
    processFull(request, OkHandler[A](f))

  /** Runs the request and return a Future of Either a FullResponse or a Throwable. */
  def run[A](request: Request, lifter: FutureLifter[A])(implicit ec: ExecutionContext): Future[Either[Throwable, A]] =
    lifter.run(run(request))

  /** Executes the request and return a Future of FullResponse. Does not error on non-OK response. */
  def processFull(request: Request): Future[FullResponse] =
    processFull(request, FunctionHandler[FullResponse](identity))

  /** Executes the request and return a Future of A. Does not error on non-OK response. */
  def processFull[A](request: Request, f: FullResponse => A): Future[A] =
    processFull(request, FunctionHandler[A](f))

  /** Executes the request and return a Future of Either a Response or a Throwable. Does not error on non-OK response. */
  def processFull[A](request: Request, lifter: FutureLifter[A])(implicit ec: ExecutionContext): Future[Either[Throwable, A]] =
    lifter.run(processFull(request))

  /** Executes the request. Does not error on non-OK response. */
  def processFull[A](request: Request, handler: AhcCompletionHandler[A]): Future[A] =
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
          result.success(handler.onCompleted(new AhcFullResponse(response)))
          response
        }
        override def onThrowable(t: Throwable): Unit = {
          result.failure(t)
        }
      })
      result.future
    }

  /** Runs the request and return a Future of StreamResponse. */
  def runStream(request: Request): Future[StreamResponse] =
    processStream(request, OkHandler.stream[StreamResponse](Future.successful))

  /** Runs the request and return a Future of A. */
  def runStream[A](request: Request, f: StreamResponse => Future[A]): Future[A] =
    processStream(request, OkHandler.stream[A](f))

  def download(request: Request, file: File): Future[File] =
    runStream(request, asFile(file))

  /** Executes the request and return a Future of StreamResponse. Does not error on non-OK response. */
  def processStream(request: Request): Future[StreamResponse] =
    processStream(request, FunctionHandler.stream[StreamResponse](Future.successful))

  /** Executes the request and return a Future of A. Does not error on non-OK response. */
  def processStream[A](request: Request, f: StreamResponse => Future[A]): Future[A] =
    processStream(request, FunctionHandler.stream[A](f))

  /** Executes the request and return a Future of A. Does not error on non-OK response. */
  def processStream[A](request: Request, handler: AhcStreamHandler[A]): Future[A] =
    {
      val result = Promise[A]()
      val xrequest = buildRequest(request)
      asyncHttpClient.executeRequest(xrequest, new StreamedAsyncHandler[XResponse]() {
        override def onStatusReceived(status: HttpResponseStatus): XState = {
          fromState(handler.onStatusReceived(status))
        }
        override def onHeadersReceived(headers: HttpResponseHeaders): XState = {
          fromState(handler.onHeadersReceived(headers))
        }
        override def onStream(publisher: Publisher[HttpResponseBodyPart]): XState = {
          val partialResponse = handler.builder.build()
          result.completeWith(handler.onStream(new AhcStreamResponse(partialResponse, publisher)))
          XState.CONTINUE
        }
        override def onBodyPartReceived(bodyPart: HttpResponseBodyPart): XState = XState.CONTINUE
        override def onCompleted(): XResponse = ???
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
      asyncHttpClient.executeRequest(xrequest, upgradeHandler.addWebSocketListener(
        new WebSocketListener(handler, result)).build())
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
        import shaded.ahc.org.asynchttpclient.request.body.generator.FileBodyGenerator
        val bodyGenerator = new FileBodyGenerator(b.file)
        builder.setBody(bodyGenerator)
        (builder, request.headers)
      case b: InMemoryBody =>
        builder.setBody(b.bytes)
        (builder, request.headers)
      // case StreamedBody(bytes) =>
      //  (builder, request.headers)
    }

    // headers
    for {
      header <- updatedHeaders
      value <- header._2
    } builder.addHeader(header._1, value)

    // Set the signature calculator.
    signatureOpt.foreach { signatureCalculator =>
      builderWithBody.setSignatureCalculator(new XSignatureCalculator {
        override def calculateAndAddSignature(request: XRequest, requestBuilder: RequestBuilderBase[_]): Unit = {
          val (name, value) = signatureCalculator.sign(request.getUrl, Option(request.getHeaders.get(HeaderNames.CONTENT_TYPE)), Option(request.getByteData).getOrElse(Array.emptyByteArray))
          requestBuilder.addHeader(name, value)
        }
      })
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
      import shaded.ahc.org.asynchttpclient.uri.Uri
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
