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

package gigahorsetest

import org.scalatest._
import scala.concurrent.Future
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

class AkkaHttpClientSpec extends BaseHttpClientSpec {
  // custom loan pattern
  override def withHttp(testCode: gigahorse.HttpClient => Future[Assertion]): Future[Assertion] =
    {
      import gigahorse.support.akkahttp.Gigahorse
      implicit val system = ActorSystem("gigahorse-akka-http")
      implicit val materializer = ActorMaterializer()
      val server = getServer
      server.start
      val wsServer = getWsServer
      wsServer.start
      val http: gigahorse.HttpClient = Gigahorse.http(Gigahorse.config, system)
      complete {
        testCode(http)
      } lastly {
        http.close
        wsServer.stop()
        wsServer.destroy()
        server.stop()
        server.destroy()
        system.terminate()
      }
    }
}
