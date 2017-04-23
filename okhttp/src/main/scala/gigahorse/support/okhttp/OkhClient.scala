/*
 * Copyright 2017 by Eugene Yokota
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
package support.okhttp

import java.io.{ IOException, File }
import okhttp3.{ OkHttpClient => XOkHttpClient, Request => XRequest, Response => XResponse }
import okhttp3.{ HttpUrl, RequestBody, MediaType, Call, Callback, Cache }
import scala.concurrent.{ Promise, Future, ExecutionContext }
import scala.util.control.NonFatal
import okio.Okio
import com.typesafe.sslconfig.ssl._
import javax.net.ssl.{ X509TrustManager, SSLContext, TrustManager }
import java.security.SecureRandom

class OkhClient(config: Config) extends HttpClient {
  private type HB = HttpUrl.Builder
  private type RB = XRequest.Builder
  private val client: XOkHttpClient = buildClient

  def underlying[A]: A = client.asInstanceOf[A]

  // According to http://square.github.io/okhttp/3.x/okhttp/okhttp3/OkHttpClient.html
  // shutdown isn't necessary
  def close(): Unit = {
    // This is provided in case we do need to clean up threads etc.
    client.dispatcher.executorService.shutdown
    client.connectionPool.evictAll
    Option(client.cache) foreach { _.close }
  }

  /** Runs the request and return a Future of FullResponse. */
  def run(request: Request): Future[FullResponse] =
    processFull(request, OkHandler[FullResponse](identity))

  /** Runs the request and return a Future of A. */
  def run[A](request: Request, f: FullResponse => A): Future[A] =
    processFull(request, OkHandler[A](f))

  def buildRequest(request: Request): XRequest =
    {
      import request._

      // set the URL
      val httpUrl = url.replaceFirst("^ws:", "http:").replaceFirst("^wss:", "https:")
      val u0 = Option(HttpUrl.parse(httpUrl)).getOrElse { sys.error(s"'$url' is not a well-formed URL") }

      // queries
      val u: HttpUrl =
        queryString match {
          case qs if qs.isEmpty => u0
          case _ =>
            val b0 = u0.newBuilder
            val urlfs: List[HB => HB] =
              for {
                (key, values) <- queryString.toList
                value <- values
              } yield ({ case b: HB => b.addQueryParameter(key, value) }: HB => HB)
            val builder = (b0 /: urlfs) { case (b, f) => f(b) }
            builder.build
        }

      // body
      def bdy = body match {
        case _: EmptyBody    =>
          RequestBody.create(MediaType.parse(contentType.getOrElse("text/plain; charset=utf-8")), "")
        case b: FileBody     =>
          RequestBody.create(MediaType.parse(contentType.getOrElse("multipart/form-data")), b.file)
        case b: InMemoryBody =>
          RequestBody.create(MediaType.parse(contentType.getOrElse("text/plain; charset=utf-8")), b.bytes)
      }

      // headers
      def headerfs: List[RB => RB] =
        (for {
          (key, values) <- headers.toList
          value <- values
        } yield ({ case b: RB => b.header(key, value) }: RB => RB))

      val requestfs: List[RB => RB] = List[RB => RB](
        { case b: RB => b.url(u) },
        // methods
        { case b: RB =>
            method match {
              case HttpVerbs.GET    => b.get()
              case HttpVerbs.PATCH  => b.patch(bdy)
              case HttpVerbs.POST   => b.post(bdy)
              case HttpVerbs.PUT    => b.put(bdy)
              case HttpVerbs.DELETE => b.delete()
              case HttpVerbs.HEAD   => b.head()
            }
        }) ++ headerfs

      val b0 = new XRequest.Builder()
      val builder = (b0 /: requestfs) { case (b, f) => f(b) }
      builder.build()
    }

  /** Runs the request and return a Future of Either a FullResponse or a Throwable. */
  def run[A](request: Request, lifter: FutureLifter[A])(implicit ec: ExecutionContext): Future[Either[Throwable,A]] =
    lifter.run(run(request))

  def download(request: Request, file: File): Future[File] =
    processStream(request, OkHandler.stream { res: FullResponse =>
      Future.successful {
        val from = res.underlying[XResponse].body.source
        val to = Okio.buffer(Okio.sink(file))
        try {
          try {
            to.writeAll(from)
          } finally {
            to.close
          }
        } finally {
          from.close
        }
        file
      }
    })

  /** Executes the request and return a Future of FullResponse. Does not error on non-OK response. */
  def processFull(request: Request): Future[FullResponse] =
    processFull(request, FunctionHandler[FullResponse](identity))

  /** Executes the request and return a Future of A. Does not error on non-OK response. */
  def processFull[A](request: Request, f: FullResponse => A): Future[A] =
    processFull(request, FunctionHandler[A](f))

  /** Executes the request and return a Future of Either a Response or a Throwable. Does not error on non-OK response. */
  def processFull[A](request: Request, lifter: FutureLifter[A])(implicit ec: ExecutionContext): Future[Either[Throwable,A]] =
    lifter.run(processFull(request))

  /** Executes the request. Does not error on non-OK response. */
  def processFull[A](request: Request, handler: OkhCompletionHandler[A]): Future[A] =
    {
      val result = Promise[A]()
      val r = buildRequest(request)
      client.newCall(r).enqueue(new Callback {
        def onResponse(call: Call, res: XResponse): Unit =
          try {
            handler.onStatusReceived(res.code)
            handler.onHeadersReceived(res.headers)
            result.success(handler.onCompleted(new OkhFullResponse(res)))
          } catch {
            case NonFatal(e) =>
              result.failure(e)
              res.close
          }
        def onFailure(call: Call, e: IOException): Unit = result.failure(e)
      })
      result.future
    }

  /** Executes the request and return a Future of A. Does not error on non-OK response. */
  def processStream[A](request: Request, handler: OkhStreamHandler[A]): Future[A] =
    {
      val result = Promise[A]()
      val r = buildRequest(request)
      client.newCall(r).enqueue(new Callback {
        def onResponse(call: Call, res: XResponse): Unit =
          try {
            handler.onStatusReceived(res.code)
            handler.onHeadersReceived(res.headers)
            result.completeWith(handler.onStream(new OkhFullResponse(res)))
          } catch {
            case NonFatal(e) =>
              result.failure(e)
              res.close
          }
        def onFailure(call: Call, e: IOException): Unit = result.failure(e)
      })
      result.future
    }

  def websocket(request: Request)(handler: PartialFunction[WebSocketEvent, Unit]): Future[WebSocket] =
    {
      val result = Promise[WebSocket]()
      val xrequest = buildRequest(request)
      val listener = new OkhWebSocketListener(handler, result)
      client.newWebSocket(xrequest, listener)
      result.future
    }

  def buildClient: XOkHttpClient =
    {
      import java.util.concurrent.TimeUnit
      val b0 = new XOkHttpClient.Builder()
      val b1 = b0
        .connectTimeout(config.connectTimeout.toMillis, TimeUnit.MILLISECONDS)
        .readTimeout(config.readTimeout.toMillis, TimeUnit.MILLISECONDS)
        .writeTimeout(config.readTimeout.toMillis, TimeUnit.MILLISECONDS)
        .followRedirects(config.followRedirects)
      config.cacheDirectory match {
        case Some(dir) => b1.cache(new Cache(dir, config.maxCacheSize.bytes))
        case None      => b1
      }

      val result = configureSsl(config.ssl, b1).build()
      result
    }

  def configureSsl(sslConfig: SSLConfigSettings, b1: XOkHttpClient.Builder): XOkHttpClient.Builder =
    {
      // context!
      if (sslConfig.default) b1
      else {
        val b2 =
          if (sslConfig.loose.acceptAnyCertificate) {
            val tm = SSL.insecureTrustManager
            val c = SSLContext.getInstance("TLS")
            c.init(null, Array[TrustManager](tm), new SecureRandom)
            val socketFactory = c.getSocketFactory
            b1.sslSocketFactory(socketFactory, tm)
          }
          else {
            val (sslContext, trustManagerOpt) = SSL.buildContext(sslConfig)
            val socketFactory = sslContext.getSocketFactory
            b1.sslSocketFactory(socketFactory, trustManagerOpt match {
              case Some(tm: X509TrustManager) => tm
              case _                          => sys.error(s"Unexpected trust manager: $trustManagerOpt")
            })
          }
        val b3 =
          if (sslConfig.loose.disableHostnameVerification) b2.hostnameVerifier(SSL.insecureHostnameVerifier)
          else b2
        b3
      }
    }
}
