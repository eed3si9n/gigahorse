/*
 * Copyright 2022 by Eugene Yokota
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
package support.apachehttp

import java.io.File
import java.net.URLEncoder
import java.nio.ByteBuffer
import shaded.apache.org.apache.http.{
  HttpEntityEnclosingRequest,
  HttpHost,
  HttpRequest => XRequest,
  HttpResponse => XResponse,
  HttpRequestInterceptor
}
import shaded.apache.org.apache.http.auth.{ AuthScope, UsernamePasswordCredentials }
import shaded.apache.org.apache.http.client.methods.HttpRequestWrapper
import shaded.apache.org.apache.http.concurrent.FutureCallback
import shaded.apache.org.apache.http.entity.ContentType
import shaded.apache.org.apache.http.impl.client.BasicCredentialsProvider
import shaded.apache.org.apache.http.impl.nio.client.{
  CloseableHttpAsyncClient => XClient,
  HttpAsyncClients,
  HttpAsyncClientBuilder,
}
import shaded.apache.org.apache.http.nio.IOControl
import shaded.apache.org.apache.http.nio.client.methods.{
  AsyncByteConsumer,
  HttpAsyncMethods,
  ZeroCopyConsumer,
}
import shaded.apache.org.apache.http.nio.protocol.HttpAsyncRequestProducer
import shaded.apache.org.apache.http.protocol.HttpContext
import shaded.apache.org.apache.http.util.EntityUtils
import scala.collection.concurrent.TrieMap
import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.util.control.NonFatal

class ApacheHttpClient(config: Config) extends HttpClient {
  private val clients: TrieMap[(Option[String], Option[Realm], Option[SignatureCalculator]), XClient] =
    TrieMap()
  private type CB = HttpAsyncClientBuilder

  override def underlying[A]: A = buildClient(None, None, None).asInstanceOf[A]

  override def close(): Unit = {
    clients.values.foreach(_.close())
  }

  /** Runs the request and return a Future of FullResponse. */
  override def run(request: Request): Future[FullResponse] =
    processFull(request, OkHandler[FullResponse](identity))

  /** Runs the request and return a Future of A. */
  override def run[A](request: Request, f: FullResponse => A): Future[A] =
    processFull(request, OkHandler[A](f))

  /** Runs the request and return a Future of Either a FullResponse or a Throwable. */
  override def run[A](request: Request, lifter: FutureLifter[A])(implicit ec: ExecutionContext): Future[Either[Throwable,A]] =
    lifter.run(run(request))

  // This creates an XRequest object, and potentially an XOkHttpClient with some overrides
  def buildRequest(request: Request): (XRequest, XClient) = {
    import request._
    val (producer, client) = buildRequestProducer(request)
    val r = producer.generateRequest
    if (headers.nonEmpty) {
      headers.foreach { case (k, vs) =>
        vs.foreach { v =>
          r.setHeader(k, v)
        }
      }
    }
    (r, client)
  }

  def buildRequestProducer(request: Request): (HttpAsyncRequestProducer, XClient) = {
    import request._
    val u =
      if (queryString.isEmpty) url
      else {
        val q = queryString.toSeq.flatMap { case (k, vs) =>
          vs.map { v =>
            val ek = URLEncoder.encode(k, "UTF-8")
            val ev = URLEncoder.encode(v, "UTF-8")
            s"$ek=$ev" }
        }.mkString("&")
        s"$url?$q"
      }
    def buildContentType(opt: Option[String], fallback: ContentType): ContentType =
      opt match {
        case Some(value) => ContentType.parse(value)
        case _           => fallback
      }
    def ct: ContentType =
      body match {
        case _: FileBody => buildContentType(contentType, ContentType.MULTIPART_FORM_DATA)
        case _           => buildContentType(contentType, ContentType.create("text/plain", "utf-8"))
      }
    def buildProducer(
        create: (String, Array[Byte], ContentType) => HttpAsyncRequestProducer,
        createZero: (String, File, ContentType) => HttpAsyncRequestProducer,
    ): HttpAsyncRequestProducer =
      body match {
        case _: EmptyBody    => create(u, Array[Byte](), ct)
        case b: FileBody     => createZero(u, b.file, ct)
        case b: InMemoryBody => create(u, b.bytes, ct)
      }

    // https://www.javadoc.io/static/org.apache.httpcomponents/httpcore/4.4.6/org/apache/http/entity/ContentType.html
    val producer = method match {
      case HttpVerbs.GET    => HttpAsyncMethods.createGet(u)
      case HttpVerbs.POST   => buildProducer(HttpAsyncMethods.createPost, HttpAsyncMethods.createZeroCopyPost)
      case HttpVerbs.PUT    => buildProducer(HttpAsyncMethods.createPut, HttpAsyncMethods.createZeroCopyPut)
      case HttpVerbs.DELETE => HttpAsyncMethods.createDelete(u)
      case HttpVerbs.HEAD   => HttpAsyncMethods.createHead(u)
      case HttpVerbs.PATCH  => sys.error(s"PATCH method is not supported")
    }
    val client =
      if (authOpt.isDefined || signatureOpt.isDefined)
        buildClient(authOpt, signatureOpt, Option(producer.getTarget))
      else buildClient(authOpt, signatureOpt, None)
    (producer, client)
  }

  def download(request: Request, file: File): Future[File] =
    processFile(request, file, OkHandler.zeroCopy { (_, _) => () })

  /** Executes the request and return a Future of FullResponse. Does not error on non-OK response. */
  override def processFull(request: Request): Future[FullResponse] =
    processFull(request, FunctionHandler[FullResponse](identity))

  /** Executes the request and return a Future of A. Does not error on non-OK response. */
  override def processFull[A](request: Request, f: FullResponse => A): Future[A] =
    processFull(request, FunctionHandler[A](f))

  /** Executes the request and return a Future of Either a Response or a Throwable. Does not error on non-OK response. */
  def processFull[A](request: Request, lifter: FutureLifter[A])(implicit ec: ExecutionContext): Future[Either[Throwable,A]] =
    lifter.run(processFull(request))

  /** Executes the request. Does not error on non-OK response. */
  def processFull[A](request: Request, handler: ApacheCompletionHandler[A]): Future[A] = {
    val result = Promise[A]()
    val (r, client) = buildRequest(request)
    client.start()
    client.execute(HttpRequestWrapper.wrap(r),
      new FutureCallback[XResponse] {
        def completed(response: XResponse): Unit =
          attempt(result) {
            handler.onStatusReceived(ApacheFullResponse.status(response))
            handler.onHeadersReceived(ApacheFullResponse.headers(response))
            result.success(handler.onCompleted(new ApacheFullResponse(response)))
          }
        def cancelled(): Unit = result.failure(new RuntimeException("cancelled"))
        def failed(e: Exception): Unit = result.failure(e)
      })
    result.future
  }

  // if anything happens, fail the promise
  def attempt[A](result: Promise[A])(f: => Unit): Unit =
    try {
      f
    } catch {
      case NonFatal(e) =>
        result.failure(e)
    }

  /** Executes the request. Does not error on non-OK response. */
  def processFile(request: Request, file: File, handler: ApacheZeroCopyHandler): Future[File] = {
    val result = Promise[File]()
    val (r, client) = buildRequestProducer(request)
    client.start()
    client.execute(
      r,
      new ZeroCopyConsumer[File](file) {
        override def process(response: XResponse, file: File, contentType: ContentType): File = {
          attempt(result) {
            handler.onStatusReceived(ApacheFullResponse.status(response))
            handler.onHeadersReceived(ApacheFullResponse.headers(response))
            handler.onFileReceived(file, contentType)
            result.success(file)
          }
          file
        }
      },
      None.orNull,
    )
    result.future
  }

  /** Executes the request. Does not error on non-OK response. */
  def processByteStream[A](request: Request, handler: ApacheByteStreamHandler[A]): Future[A] = {
    val result = Promise[A]()
    val (r, client) = buildRequestProducer(request)
    client.start()
    client.execute(
      r,
      new AsyncByteConsumer[Unit] {
        override def onResponseReceived(response: XResponse): Unit =
          attempt(result) {
            handler.onStatusReceived(ApacheFullResponse.status(response))
            handler.onHeadersReceived(ApacheFullResponse.headers(response))
          }
        override def onByteReceived(buf: ByteBuffer, ioControl: IOControl): Unit =
          attempt(result) {
            handler.onByteReceived(buf, ioControl)
          }
        override def buildResult(ctx: HttpContext): Unit =
          attempt(result) {
            result.completeWith(handler.buildResult)
          }
      },
      None.orNull,
    )
    result.future
  }

  def buildClient(
    authOpt: Option[Realm],
    signatureOpt: Option[SignatureCalculator],
    targetOpt: Option[HttpHost],
  ): XClient =
    clients.getOrElseUpdate(
      (targetOpt.map(_.toString), authOpt, signatureOpt),
      buildClient0(HttpAsyncClients.custom(), authOpt, signatureOpt, targetOpt)
    )

  // https://hc.apache.org/httpcomponents-asyncclient-4.1.x/current/httpasyncclient/apidocs/
  private def buildClient0(
    b0: CB,
    authOpt: Option[Realm],
    signatureOpt: Option[SignatureCalculator],
    targetOpt: Option[HttpHost],
  ): XClient = {
    val clientfs: List[CB => CB] = List[CB => CB](
      (b: CB) => if (config.maxConnections > 0) b.setMaxConnTotal(config.maxConnections) else b,
      (b: CB) => if (config.maxConnectionsPerHost > 0) b.setMaxConnPerRoute(config.maxConnectionsPerHost) else b,
    ) :::
    (authOpt match {
      case Some(auth) =>
        List[CB => CB]({ case b: CB =>
          b.setDefaultCredentialsProvider(buildCredentialProvider(auth))
        })
      case None => Nil
    }) :::
    ((signatureOpt, targetOpt) match {
      case (Some(signatureCalculator), Some(target)) =>
        List[CB => CB]({ case b: CB =>
          b.addInterceptorLast(buildInterceptor(signatureCalculator, target))
        })
      case _ => Nil
    })
    val b1 = clientfs.foldLeft(b0){ (b, f) => f(b) }
    val result = b1.build()
    result
  }

  // https://hc.apache.org/httpclient-legacy/apidocs/org/apache/commons/httpclient/auth/AuthScope.html
  def buildCredentialProvider(auth: Realm): BasicCredentialsProvider =
    auth.scheme match {
      case AuthScheme.Basic =>
        val p = new BasicCredentialsProvider
        val credentials = new UsernamePasswordCredentials(auth.username, auth.password)
        val scope = auth.realmNameOpt match {
          case Some(realm) => new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, realm)
          case _           => AuthScope.ANY
        }
        p.setCredentials(scope, credentials)
        p
      case _ =>
        sys.error(s"unsupported scheme: ${auth.scheme}")
    }

  def buildInterceptor(signatureCalculator: SignatureCalculator, target: HttpHost): HttpRequestInterceptor =
    (request: XRequest, context: HttpContext) => {
      val uri = target.toString + HttpRequestWrapper.wrap(request).getURI.toString
      val (contentType, content) = request match {
        case req: HttpEntityEnclosingRequest =>
          val entity = req.getEntity
          val contentType = Option(entity.getContentType).map(_.getValue)
          (contentType, EntityUtils.toByteArray(entity))
        case _ => (None, Array[Byte]())
      }
      val (name, value) = signatureCalculator.sign(uri, contentType, content)
      request.setHeader(name, value)
    }

  def websocket(request: Request)(handler: PartialFunction[WebSocketEvent, Unit]): Future[WebSocket] =
    ???
}
