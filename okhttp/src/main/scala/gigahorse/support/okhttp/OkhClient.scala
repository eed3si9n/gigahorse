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

import java.io.{ByteArrayOutputStream, File, IOException}

import okhttp3.{OkHttpClient => XOkHttpClient, Request => XRequest, Response => XResponse}
import okhttp3.{Authenticator, Cache, Call, Callback, Credentials, HttpUrl, Interceptor, MediaType, RequestBody, Route}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.control.NonFatal
import okio.Okio
import com.typesafe.sslconfig.ssl._
import javax.net.ssl.{SSLContext, TrustManager, X509TrustManager}
import java.security.SecureRandom

class OkhClient(config: Config) extends HttpClient {
  private type HB = HttpUrl.Builder
  private type RB = XRequest.Builder
  private type CB = XOkHttpClient.Builder
  private val client0: XOkHttpClient = buildClient

  def underlying[A]: A = client0.asInstanceOf[A]

  // According to http://square.github.io/okhttp/3.x/okhttp/okhttp3/OkHttpClient.html
  // shutdown isn't necessary
  def close(): Unit = {
    // This is provided in case we do need to clean up threads etc.
    client0.dispatcher.executorService.shutdown
    client0.connectionPool.evictAll
    Option(client0.cache) foreach { _.close }
  }

  /** Runs the request and return a Future of FullResponse. */
  def run(request: Request): Future[FullResponse] =
    processFull(request, OkHandler[FullResponse](identity))

  /** Runs the request and return a Future of A. */
  def run[A](request: Request, f: FullResponse => A): Future[A] =
    processFull(request, OkHandler[A](f))

  // This creates an XRequest object, and potentially an XOkHttpClient with some overrides
  def buildRequest(request: Request): (XRequest, XOkHttpClient) =
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
            val builder = urlfs.foldLeft(b0){ (b, f) => f(b) }
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
      val builder = requestfs.foldLeft(b0){ (b, f) => f(b) }
      val result = builder.build()

      val client = if (authOpt.isDefined || signatureOpt.isDefined)
        buildClient(client0.newBuilder, authOpt, signatureOpt)
      else
        client0
      (result, client)
    }

  /** Runs the request and return a Future of Either a FullResponse or a Throwable. */
  def run[A](request: Request, lifter: FutureLifter[A])(implicit ec: ExecutionContext): Future[Either[Throwable,A]] =
    lifter.run(run(request))

  def download(request: Request, file: File): Future[File] =
    processStream(request, OkHandler.stream { (res: FullResponse) =>
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
      val (r, client) = buildRequest(request)
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
      val (r, client) = buildRequest(request)
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
      val (xrequest, client) = buildRequest(request)
      val listener = new OkhWebSocketListener(handler, result)
      client.newWebSocket(xrequest, listener)
      result.future
    }

  def buildClient: XOkHttpClient =
    {
      buildClient(new XOkHttpClient.Builder(), config.authOpt, None)
    }

  def buildClient(b0: CB, authOpt: Option[Realm], signatureOpt: Option[SignatureCalculator]): XOkHttpClient =
    {
      import java.util.concurrent.TimeUnit
      val clientfs: List[CB => CB] = List[CB => CB](
        { case b: CB => b.connectTimeout(config.connectTimeout.toMillis, TimeUnit.MILLISECONDS) },
        { case b: CB => b.readTimeout(config.readTimeout.toMillis, TimeUnit.MILLISECONDS) },
        { case b: CB => b.writeTimeout(config.readTimeout.toMillis, TimeUnit.MILLISECONDS) },
        { case b: CB => b.followRedirects(config.followRedirects) }
      ) :::
      (config.cacheDirectory match {
        case Some(dir) =>
          List[CB => CB]({ case b: CB => b.cache(new Cache(dir, config.maxCacheSize.bytes)) })
        case None      => Nil
      }) :::
      (authOpt match {
        case Some(auth) =>
          List[CB => CB]({ case b: CB => b.authenticator(buildAuthenticator(auth)) })
        case None       => Nil
      })
      val b1 = clientfs.foldLeft(b0){ (b, f) => f(b) }
      val b2 = configureSsl(config.ssl, b1)
      val b3 = signatureOpt match {
        case Some(signatureCalculator) =>
          configureSignatureCalculator(signatureCalculator, b2)
        case None => b2
      }
      val result = b3.build()
      result
    }

  def buildAuthenticator(auth: Realm): Authenticator =
    new Authenticator {
      override def authenticate(route: Route, response: XResponse): XRequest =
        {
          if (responseCount(response) >= 3) {
              return null; // If we've failed 3 times, give up.
          }
          val credential = auth.scheme match {
            case AuthScheme.Basic => Credentials.basic(auth.username, auth.password)
            case _                => sys.error(s"Unsupported scheme: ${auth.scheme}")
          }
          response.request.newBuilder.header("Authorization", credential).build
        }
      private def responseCount(response: XResponse): Int =
        {
          def doResponseCount(x: XResponse, n: Int): Int =
            Option(x.priorResponse) match {
              case Some(p) => doResponseCount(p, n + 1)
              case None    => n
            }
          doResponseCount(response, 1)
        }
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

  def configureSignatureCalculator(signatureCalculator: SignatureCalculator, b1: XOkHttpClient.Builder): XOkHttpClient.Builder =
    {
      b1.addNetworkInterceptor(new Interceptor {
        override def intercept(chain: Interceptor.Chain): XResponse = {
          val request = chain.request()
          val (contentType, content) = Option(request.body()) match {
            case Some(body) =>
              val baos = new ByteArrayOutputStream()
              val sink = Okio.buffer(Okio.sink(baos))
              body.writeTo(sink)
              sink.flush()
              (Option(body.contentType()).map(_.toString), baos.toByteArray)
            case None => (None, Array.emptyByteArray)
          }
          val (name, value) = signatureCalculator.sign(request.url().toString, contentType, content)
          val signedRequest = request.newBuilder.header(name, value).method(request.method, request.body).build
          chain.proceed(signedRequest)
        }
      })
    }
}
