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
import java.net.URI
import java.nio.{
  ByteBuffer,
  CharBuffer,
}
import java.nio.channels.{ FileChannel, Channels }
import java.nio.file.{ Files, StandardOpenOption }
import shaded.apache.org.apache.hc.client5
import shaded.apache.org.apache.hc.core5
import client5.http.async.methods.{
  AbstractBinResponseConsumer,
  AbstractCharResponseConsumer,
  SimpleHttpResponse,
  SimpleRequestBuilder,
  SimpleRequestProducer,
  SimpleResponseConsumer,
}
import client5.http.auth.{
  AuthScope,
  CredentialsProvider,
  UsernamePasswordCredentials,
}
import client5.http.impl.async.{
  CloseableHttpAsyncClient as XClient,
  HttpAsyncClientBuilder,
  HttpAsyncClients,
}
import client5.http.impl.auth.CredentialsProviderBuilder
import client5.http.impl.nio.PoolingAsyncClientConnectionManager
import core5.concurrent.FutureCallback
import core5.http.{
  ContentType,
  EntityDetails,
  Header as XHeader,
  HttpEntity,
  HttpRequest as XRequest,
  HttpRequestInterceptor,
  HttpResponse as XResponse,
}
import core5.http.nio.{
  AsyncEntityProducer,
  AsyncRequestProducer,
  DataStreamChannel,
}
import core5.http.nio.entity.FileEntityProducer
import core5.http.nio.support.BasicRequestProducer
import core5.http.protocol.HttpContext
import core5.reactor.IOReactorConfig
import core5.util.Timeout

import scala.collection.concurrent.TrieMap
import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.util.control.NonFatal

class ApacheHttpClient(config: Config) extends HttpClient {

  private val clients: TrieMap[(Option[Realm], Option[SignatureCalculator], Option[String]), XClient] =
    TrieMap()

  private type CB = HttpAsyncClientBuilder

  val ioReactorConfig = IOReactorConfig.custom()
    .setSoTimeout(Timeout.ofSeconds(5))
    .build()

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

  // Returns a AsyncRequestProducer.
  // SimpleHttpRequest maps to our idea of the request, and that would work for empty body request.
  // However, for file upload, we need to customize the producer.
  private def buildRequestProducer(request: Request): AsyncRequestProducer = {
    import request.*
    val builder = buildRequestBuilder(request)
    def ct: ContentType =
      body match {
        case _: FileBody => buildContentType(contentType, ContentType.MULTIPART_FORM_DATA)
        case _           => buildContentType(contentType, ContentType.create("text/plain", "utf-8"))
      }
    body match {
      case _: EmptyBody =>
        val r = builder.build()
        SimpleRequestProducer.create(r)
      case b: InMemoryBody =>
        builder.setBody(b.bytes, ct)
        val r = builder.build()
        SimpleRequestProducer.create(r)
      case b: FileBody =>
        val r = builder.build()
        val entity = new FileEntityProducer(b.file, ct)
        new BasicRequestProducer(r, entity)
    }
  }
  private def buildContentType(opt: Option[String], fallback: ContentType): ContentType =
    opt match {
      case Some(value) => ContentType.parse(value)
      case _           => fallback
    }
  private def buildRequestBuilder(request: Request): SimpleRequestBuilder = {
    import request.*
    val uri = new URI(url)
    val builder = method match {
      case HttpVerbs.GET    => SimpleRequestBuilder.get(uri)
      case HttpVerbs.POST   => SimpleRequestBuilder.post(uri)
      case HttpVerbs.PUT    => SimpleRequestBuilder.put(uri)
      case HttpVerbs.DELETE => SimpleRequestBuilder.delete(uri)
      case HttpVerbs.HEAD   => SimpleRequestBuilder.head(uri)
      case HttpVerbs.PATCH  => SimpleRequestBuilder.patch(uri)
    }
    queryString.toSeq.foreach { case (k, vs) =>
      vs.foreach { (v: String) =>
        builder.addParameter(k, v)
      }
    }
    builder
  }

  def download(request: Request, file: File): Future[File] =
    processByteStream(request, new ApacheByteStreamHandler[File] {
      val temp = Files.createTempFile("temp", ".tmp")
      val c = FileChannel.open(temp, StandardOpenOption.WRITE)
      override def onByteReceived(buf: ByteBuffer): Unit = {
        c.write(buf)
        ()
      }
      override def onCompleted(): File = {
        c.force(true)
        c.close()
        Files.move(temp, file.toPath()).toFile()
      }
    })

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
    val client = buildClient(request)
    val result = Promise[A]()
    client.start()
    // https://github.com/apache/httpcomponents-client/blob/5.4.x/httpclient5/src/test/java/org/apache/hc/client5/http/examples/AsyncClientHttpExchange.java
    client.execute(
      buildRequestProducer(request),
      SimpleResponseConsumer.create(),
      new FutureCallback[SimpleHttpResponse] {
        def completed(response: SimpleHttpResponse): Unit =
          attempt(result) {
            handler.onStatusReceived(ApacheFullResponse.status(response))
            handler.onHeadersReceived(ApacheFullResponse.headers(response))
            result.success(handler.onCompleted(ApacheFullResponse(response)))
          }
        def cancelled(): Unit = result.failure(new RuntimeException("cancelled"))
        def failed(e: Exception): Unit = result.failure(e)
      }
    )
    result.future
  }

  // if anything happens, fail the promise
  private def attempt[A](result: Promise[A])(f: => Unit): Unit =
    try {
      f
    } catch {
      case NonFatal(e) =>
        result.failure(e)
    }

  /** Executes the request. Does not error on non-OK response. */
  def processByteStream[A](request: Request, handler: ApacheByteStreamHandler[A]): Future[A] = {
    val client = buildClient(request)
    val result = Promise[A]()
    client.start()
    // https://hc.apache.org/httpcomponents-client-5.4.x/current/httpclient5/apidocs/org/apache/hc/client5/http/impl/async/CloseableHttpAsyncClient.html
    client.execute(
      buildRequestProducer(request),
      new AbstractBinResponseConsumer[Unit] {
        // Triggered to signal the beginning of response processing.
        override def start(response: XResponse, contentType: ContentType): Unit =
          attempt(result) {
            handler.onStatusReceived(response.getCode())
          }
        // Triggered to obtain the capacity increment.
        override def capacityIncrement(): Int = handler.onCapacityIncrement
        override def data(buf: ByteBuffer, endOfStream: Boolean): Unit =
          attempt(result) {
            handler.onByteReceived(buf)
          }
        override def buildResult(): Unit = ()
        override def releaseResources(): Unit = ()
      },
      new FutureCallback[Unit] {
        def completed(u: Unit): Unit =
          attempt(result) {
            result.success(handler.onCompleted())
          }
        def cancelled(): Unit = result.failure(new RuntimeException("cancelled"))
        def failed(e: Exception): Unit = result.failure(e)
      }
    )
    result.future
  }

  /** Executes the request. Does not error on non-OK response. */
  def processCharStream[A](request: Request, handler: ApacheCharStreamHandler[A]): Future[A] = {
    val client = buildClient(request)
    val result = Promise[A]()
    client.start()
    // https://hc.apache.org/httpcomponents-client-5.4.x/current/httpclient5/apidocs/org/apache/hc/client5/http/impl/async/CloseableHttpAsyncClient.html
    // https://github.com/apache/httpcomponents-client/blob/5.4.x/httpclient5/src/test/java/org/apache/hc/client5/http/examples/AsyncClientHttpExchangeStreaming.java
    client.execute(
      buildRequestProducer(request),
      new AbstractCharResponseConsumer[Unit] {
        // Triggered to signal the beginning of response processing.
        override def start(response: XResponse, contentType: ContentType): Unit =
          attempt(result) {
            handler.onStatusReceived(response.getCode())
          }
        // Triggered to obtain the capacity increment.
        override def capacityIncrement(): Int = handler.onCapacityIncrement
        override def data(buf: CharBuffer, endOfStream: Boolean): Unit =
          attempt(result) {
            handler.onCharReceived(buf)
          }
        override def buildResult(): Unit = ()
        override def releaseResources(): Unit = ()
      },
      new FutureCallback[Unit] {
        def completed(u: Unit): Unit =
          attempt(result) {
            result.success(handler.onCompleted())
          }
        def cancelled(): Unit = result.failure(new RuntimeException("cancelled"))
        def failed(e: Exception): Unit = result.failure(e)
      }
    )
    result.future
  }

  def buildClient(request: Request): XClient = {
    val u = new URI(request.url)
    if (request.authOpt.isDefined || request.signatureOpt.isDefined)
      buildClient(request.authOpt, request.signatureOpt, Option(u.getHost()))
    else buildClient(request.authOpt, request.signatureOpt, Option(u.getHost()))
  }

  def buildClient(
    authOpt: Option[Realm],
    signatureOpt: Option[SignatureCalculator],
    targetOpt: Option[String],
  ): XClient =
    clients.getOrElseUpdate(
      (authOpt, signatureOpt, targetOpt),
      buildClient0(HttpAsyncClients.custom(), authOpt, signatureOpt, targetOpt)
    )

  // https://hc.apache.org/httpcomponents-client-5.4.x/current/httpclient5/apidocs/org/apache/hc/client5/http/impl/async/HttpAsyncClientBuilder.html
  private def buildClient0(
    b0: CB,
    authOpt: Option[Realm],
    signatureOpt: Option[SignatureCalculator],
    targetOpt: Option[String],
  ): XClient = {
    val clientfs: List[CB => CB] = List[CB => CB](
      (b: CB) => if (config.maxConnections > 0 || config.maxConnectionsPerHost > 0) {
        val manager = new PoolingAsyncClientConnectionManager()
        if (config.maxConnections > 0) {
          manager.setMaxTotal(config.maxConnections)
        }
        if (config.maxConnectionsPerHost > 0) {
          manager.setDefaultMaxPerRoute(config.maxConnectionsPerHost)
        }
        b.setConnectionManager(manager)
      } else b
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
          b.addRequestInterceptorLast(buildInterceptor(signatureCalculator, target))
        })
      case _ => Nil
    })
    val b1 = clientfs.foldLeft(b0){ (b, f) => f(b) }
    val result = b1.build()
    result
  }

  // https://hc.apache.org/httpcomponents-client-5.4.x/current/httpclient5/apidocs/org/apache/hc/client5/http/auth/AuthScope.html
  def buildCredentialProvider(auth: Realm): CredentialsProvider =
    auth.scheme match {
      case AuthScheme.Basic =>
        val scope = auth.realmNameOpt match {
          case Some(realm) => new AuthScope(null, null, -1, realm, null)
          case _           => new AuthScope(null, null, -1, null, null)
        }
        val credentials = new UsernamePasswordCredentials(auth.username, auth.password.toCharArray())
        val p = CredentialsProviderBuilder.create()
        p.add(scope, credentials)
        p.build()
      case _ =>
        sys.error(s"unsupported scheme: ${auth.scheme}")
    }

  def buildInterceptor(signatureCalculator: SignatureCalculator, target: String): HttpRequestInterceptor =
    (request: XRequest, entity: EntityDetails, context: HttpContext) => {
      val uri = request.getUri().toString
      val contentType = Option(entity).map(_.getContentType())
      val contentLength = Option(entity) match {
        case Some(en) => en.getContentLength().toInt
        case _        => 0
      }
      val b = ByteBuffer.allocate(contentLength)
      Option(entity) match {
        case Some(h: HttpEntity) =>
          val c = Channels.newChannel(h.getContent())
          while (c.read(b) > 0) ()
        case Some(p: AsyncEntityProducer) =>
          p.produce(new DataStreamChannel {
            def endStream(): Unit = ()
            def endStream(headers: java.util.List[_ <: XHeader]): Unit = ()
            def requestOutput(): Unit = ()
            def write(bf: ByteBuffer): Int = {
              b.put(bf.array())
              1
            }
          })
        case Some(entity) => sys.error(s"unsupported entity: $entity")
        case None => ()
      }
      val contents = b.array()
      val (name, value) = signatureCalculator.sign(uri, contentType, contents)
      request.setHeader(name, value)
    }

  def websocket(request: Request)(handler: PartialFunction[WebSocketEvent, Unit]): Future[WebSocket] =
    ???
}
