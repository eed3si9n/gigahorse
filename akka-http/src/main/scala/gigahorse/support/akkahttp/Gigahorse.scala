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

import java.nio.charset.Charset
import akka.actor.{ Actor, ActorSystem }
import akka.stream.{ Materializer, ActorMaterializer }

abstract class Gigahorse {
  /**
   * Generates a request.
   *
   * @param url The base URL to make HTTP requests to.
   * @return Request
   */
  def url(url: String): Request = Request(url)

  def withHttp[A](config: Config)(f: HttpClient => A): A =
    {
      implicit val system = ActorSystem("gigahorse-akka-http")
      implicit val materializer = ActorMaterializer()
      val client: HttpClient = http(config, system)
      try {
        f(client)
      }
      finally {
        system.shutdown()
      }
    }
  def withHttp[A](f: HttpClient => A): A =
    withHttp(config)(f)

  /** Returns default configuration using `application.conf` if present. */
  def config: Config =
    {
      import com.typesafe.config.ConfigFactory
      val c = ConfigFactory.load
      if (c.hasPath(ConfigParser.rootPath)) ConfigParser.parse(c)
      else Config()
    }

  /** Returns HttpClient. You must call `close` when you're done. */
  def http(config: Config, system: ActorSystem)(implicit fm: Materializer): HttpClient = new AkkaHttpClient(config, system)

  /** Function from `Response` to `String` */
  lazy val asString: Response => String = _.body

  /** Lifts Future[Reponse] result to Future[Either[Throwable, Reponse]] */
  lazy val asEither: FutureLifter[Response] = FutureLifter.asEither

  /** UTF-8. */
  val utf8 = Charset.forName("UTF-8")
}

object Gigahorse extends Gigahorse
