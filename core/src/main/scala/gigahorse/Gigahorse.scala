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

abstract class Gigahorse {
  /**
   * Generates a request.
   *
   * @param url The base URL to make HTTP requests to.
   * @return Request
   */
  def url(url: String): Request =
    Request(url)
      /*, Map(), Map(), None, None, None, None, None, None, None */

  def withHttp[A](config: Config)(f: HttpClient => A): A =
    {
      val client: HttpClient = http(config)
      try {
        f(client)
      }
      finally {
        client.close()
      }
    }
  def withHttp[A](f: HttpClient => A): A =
    withHttp(config)(f)

  def config: Config = Config()

  /** Returns HttpClient. You must call `close` when you're done. */
  def http(config: Config): HttpClient = new AhcHttpClient(config)
}

object Gigahorse extends Gigahorse
