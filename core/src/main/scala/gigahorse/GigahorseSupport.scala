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

import java.nio.charset.Charset
import scala.concurrent.Future

/** Common interface for Gigahorse backends.
 */
abstract class GigahorseSupport {
  /**
   * Generates a request.
   *
   * @param url The base URL to make HTTP requests to.
   * @return Request
   */
  def url(url: String): Request = Request(url)

  /** Returns default configuration using `application.conf` if present. */
  def config: Config =
    {
      import com.typesafe.config.ConfigFactory
      val c = ConfigFactory.load
      if (c.hasPath(ConfigParser.rootPath)) ConfigParser.parse(c)
      else Config()
    }

  /** Function from `Response` to `String` */
  lazy val asString: FullResponse => String = _.bodyAsString

  /** Lifts Future[Reponse] result to Future[Either[Throwable, Reponse]] */
  lazy val asEither: FutureLifter[FullResponse] = FutureLifter.asEither

  /** UTF-8. */
  val utf8 = Charset.forName("UTF-8")
}

object GigahorseSupport extends GigahorseSupport
