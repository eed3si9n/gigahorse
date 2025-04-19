/*
 * Copyright 2025 by Eugene Yokota
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

import unfiltered.netty.Server
import scala.concurrent.{ Await, Promise }
import scala.concurrent.duration.Duration

trait TestHttpServer {
  val port: Int = unfiltered.util.Port.any
  def testUrl: String = s"http://localhost:$port/"
  def getServer = setup(Server.http(port))
  def setup: Server => Server = {
    _.chunked(1024 * 1024).handler(TestPlan.testPlan)
  }
}

object TestApp extends App with TestHttpServer {
  val server = getServer
  println(server.toString)
  server.start()
  val p = Promise[Int]()
  Await.result(p.future, Duration.Inf)
}
