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

import scala.collection.JavaConverters._
import java.io.{ File, UnsupportedEncodingException }
import java.nio.charset.{ Charset, StandardCharsets }
import scala.concurrent.{ Future, Promise, ExecutionContext }
import akka.actor.{ Actor, ActorSystem }
import akka.http.scaladsl.{ Http => AkkaHttp }
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, Uri }
import akka.stream.Materializer

class AkkaHttpClient(config: Config, system: ActorSystem)(implicit fm: Materializer) extends HttpClient {
  private val akkaHttp = AkkaHttp(system)

  def underlying[A]: A = akkaHttp.asInstanceOf[A]

  /** Closes this client, and releases underlying resources. */
  def close(): Unit = ()

  /** Runs the request and return a Future of Response. Errors on non-OK response. */
  def run(request: Request): Future[Response] =
    {
      implicit val ec = system.dispatcher
      akkaHttp.singleRequest(buildRequest(request)).map {buildResponse}
    }

  /** Runs the request and return a Future of A. Errors on non-OK response. */
  def run[A](request: Request, f: Response => A): Future[A] = ???

  /** Runs the request and return a Future of Either a Response or a Throwable. Errors on non-OK response. */
  def run[A](request: Request, lifter: FutureLifter[A])(implicit ec: ExecutionContext): Future[Either[Throwable, A]] = ???

  /** Downloads the request to the file. Errors on non-OK response. */
  def download(request: Request, file: File): Future[File] = ???

  /** Executes the request and return a Future of Response. Does not error on non-OK response. */
  def process(request: Request): Future[Response] = ???

  /** Executes the request and return a Future of A. Does not error on non-OK response. */
  def process[A](request: Request, f: Response => A): Future[A] = ???

  /** Executes the request and return a Future of Either a Response or a Throwable. Does not error on non-OK response. */
  def process[A](request: Request, lifter: FutureLifter[A])(implicit ec: ExecutionContext): Future[Either[Throwable, A]] = ???

  /** Executes the request. Does not error on non-OK response. */
  def process[A](request: Request, handler: CompletionHandler[A]): Future[A] = ???

  def buildRequest(request: Request): HttpRequest =
    {
      HttpRequest(uri = Uri(request.url))
    }

  def buildResponse(response: HttpResponse)(implicit ec: ExecutionContext): Response = new AkkaHttpResponse(response)
}
