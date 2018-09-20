/*
 * Copyright 2016 by Eugene Yokota
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
package support.akkahttp

import java.io.File

import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.concurrent.duration.Duration
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.http.scaladsl.{Http, HttpExt}
import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpHeader, HttpMethod, HttpMethods, HttpRequest, HttpResponse, RequestEntity, StatusCodes, Uri}
import akka.http.scaladsl.model.ws.WebSocketRequest
import akka.util.ByteString
import DownloadHandler.asFile
import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok

class AkkaHttpClient(config: Config, system: ActorSystem)(implicit fm: Materializer) extends ReactiveHttpClient {
  private val akkaHttp: HttpExt = Http(system)

  def underlying[A]: A = akkaHttp.asInstanceOf[A]

  /** Closes this client, and releases underlying resources. */
  def close(): Unit =
    {
      val x = akkaHttp.shutdownAllConnectionPools
      Await.result(x, Duration.Inf)
    }

  /** Runs the request and return a Future of FullResponse. Errors on non-OK response. */
  def run(request: Request): Future[FullResponse] = run(request, identity)

  /** Runs the request and return a Future of A. Errors on non-OK response. */
  def run[A](request: Request, f: FullResponse => A): Future[A] = process(request, OkHandler(f))

  /** Runs the request and return a Future of Either a FullResponse or a Throwable. Errors on non-OK response. */
  def run[A](request: Request, lifter: FutureLifter[A])(implicit ec: ExecutionContext): Future[Either[Throwable, A]] =
    lifter.run(run(request))

  /** Executes the request and return a Future of FullResponse. Does not error on non-OK response. */
  def processFull(request: Request): Future[FullResponse] = processFull(request, identity[FullResponse] _)

  /** Executes the request and return a Future of A. Does not error on non-OK response. */
  def processFull[A](request: Request, f: FullResponse => A): Future[A] =
    process(request, FunctionHandler(f))

  /** Executes the request and return a Future of Either a FullResponse or a Throwable. Does not error on non-OK response. */
  def processFull[A](request: Request, lifter: FutureLifter[A])(implicit ec: ExecutionContext): Future[Either[Throwable, A]] =
    lifter.run(processFull(request))

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
    processStream(request, FunctionHandler.stream(f))

  /** Executes the request. Does not error on non-OK response. */
  def processStream[A](request: Request, handler: AkkaHttpStreamHandler[A]): Future[A] =
    process(request, handler)

  /** Executes the request. Does not error on non-OK response. */
  def process[A](request: Request, handler: AkkaHttpCompletionHandler[A]): Future[A] =
    {
      implicit val ec = system.dispatcher
      def processInitialResponse(response: HttpResponse): Future[Unit] =
        {
          val p = Promise[Unit]()
          val s1 = handler.onStatusReceived(response.status)
          if (s1 == State.Abort) {
            response.entity.discardBytes(fm)
            p.failure { StatusError(response.status.intValue) }
          }
          else p.success(())
          p.future
        }
      for {
        response <- akkaHttp.singleRequest(buildRequest(request))
        _        <- processInitialResponse(response)
        result   <- handler.onPartialResponse(response, config)
      } yield result
    }

  def buildRequest(request: Request): HttpRequest = {
    val httpReq = HttpRequest(method = buildMethod(request),
      uri = buildUri(request),
      headers = buildHeaders(request),
      entity = buildEntity(request))
    request.signatureOpt match {
      case Some(signatureCalculator) =>
        val body = request.body match {
          case b: InMemoryBody => b.bytes
          case _ => Array.emptyByteArray
        }
        val (name, value) = signatureCalculator.sign(httpReq.uri.toString(), request.contentType, body)
        HttpHeader.parse(name, value) match {
          case Ok(header, _) => httpReq.withHeaders(header)
          case _ => sys.error(s"Invalid header: ${name} = ${value}")
        }
      case None => httpReq
    }
  }

  def buildWsRequest(request: Request): WebSocketRequest =
    WebSocketRequest(uri = buildUri(request),
      extraHeaders = buildHeaders(request))

  private def buildMethod(request: Request): HttpMethod =
    request.method match {
      case HttpVerbs.GET     => HttpMethods.GET
      case HttpVerbs.POST    => HttpMethods.POST
      case HttpVerbs.PUT     => HttpMethods.PUT
      case HttpVerbs.PATCH   => HttpMethods.PATCH
      case HttpVerbs.DELETE  => HttpMethods.DELETE
      case HttpVerbs.HEAD    => HttpMethods.HEAD
      case HttpVerbs.OPTIONS => HttpMethods.OPTIONS
    }

  private def buildHeaders(request: Request): List[HttpHeader] =
    {
      import akka.http.scaladsl.model.headers.{ Authorization, BasicHttpCredentials }
      val authHeaders = (request.authOpt orElse config.authOpt) match {
        case Some(auth) =>
          auth.scheme match {
            case AuthScheme.Basic =>
              List(Authorization(BasicHttpCredentials(auth.username, auth.password)))
            case _                => sys.error(s"Unsupported scheme: ${auth.scheme}")
          }
        case None       => Nil
      }
      val headers0 = for {
        (k, vs) <- request.headers.toList
        v       <- vs.toList
        x       <- HttpHeader.parse(k, v) match {
          case HttpHeader.ParsingResult.Ok(header, _) => List(header)
          case _                                      => Nil
        }
      } yield x
      headers0 ::: authHeaders
    }

  private def buildUri(request: Request): Uri =
    {
      import request._
      // queries
      val qs = for {
        (key, values) <- queryString
        value <- values
      } yield (key, value)
      Uri(url).withQuery(Uri.Query(qs))
    }

  private def buildEntity(request: Request): RequestEntity =
    request.body match {
      case _: EmptyBody => HttpEntity.Empty
      case b: FileBody => ??? // https://gist.github.com/jrudolph/08d0d28e1eddcd64dbd0
      case b: InMemoryBody =>
        val ct = ContentType.parse(request.contentType.getOrElse("text/plain; charset=utf-8")) match {
          case Right(x) => x
          case Left(xs) => sys.error(xs.toString)
        }
        HttpEntity.Strict(ct, ByteString(b.bytes))
    }

  /** Open a websocket connection. */
  def websocket(request: Request)(handler: PartialFunction[WebSocketEvent, Unit]): Future[WebSocket] =
    {
      implicit val ec = system.dispatcher
      // http://doc.akka.io/docs/akka-http/current/scala/http/client-side/websocket-support.html
      val xrequest = buildWsRequest(request)
      import akka.stream.scaladsl._
      import akka.Done
      import akka.http.scaladsl.model.ws.Message
      val listener = new WebSocketListener(handler, system)
      val wsSink: Sink[Message, Future[Done]] = listener.sink
      val wsSource = listener.source
      val flow: Flow[Message, Message, Future[Done]] =
        Flow.fromSinkAndSourceMat(wsSink, wsSource)(Keep.left)
      // upgradeResponse is a Future[WebSocketUpgradeResponse] that
      // completes or fails when the connection succeeds or fails
      // and closed is a Future[Done] representing the stream completion from above
      val (upgradeResponse, _) = akkaHttp.singleWebSocketRequest(xrequest, flow)
      val _ = upgradeResponse.map { upgrade =>
        // just like a regular http request we can access response status which is available via upgrade.response.status
        // status code 101 (Switching Protocols) indicates that server support WebSockets
        if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
          Done
        } else {
          throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
        }
      }
      val result = listener.result
      result.future
    }
}
