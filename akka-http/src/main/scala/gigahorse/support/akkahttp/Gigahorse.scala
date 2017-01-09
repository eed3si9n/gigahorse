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

import akka.actor.ActorSystem
import akka.stream.{ Materializer, ActorMaterializer }
import scala.concurrent.Await
import scala.concurrent.duration.Duration

abstract class Gigahorse extends GigahorseSupport {
  /** Returns HttpClient. You must call `close` when you're done. */
  def http(config: Config, system: ActorSystem)(implicit fm: Materializer): HttpClient = new AkkaHttpClient(config, system)

  def withHttp[A](config: Config)(f: HttpClient => A): A =
    {
      implicit val system = ActorSystem("gigahorse-akka-http")
      implicit val materializer = ActorMaterializer()
      val client: HttpClient = http(config, system)
      try {
        f(client)
      }
      finally {
        client.close()
        Await.result(system.terminate(), Duration.Inf)
      }
    }
  def withHttp[A](f: HttpClient => A): A =
    withHttp(config)(f)
}

object Gigahorse extends Gigahorse
