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
import scala.concurrent._
import scala.concurrent.duration._
import java.io.File
import sbt.io.IO
import sbt.io.syntax._

class AkkaHttpClientSpec extends FlatSpec with Matchers {
  "http.run(r)" should "retrieve a resource" in {
    import gigahorse.support.akkahttp.Gigahorse
    Gigahorse.withHttp(Gigahorse.config) { http =>
      val r = Gigahorse.url("http://api.duckduckgo.com").
        addQueryString(
          "q" -> "1 + 1",
          "format" -> "json"
        ).get
      val f = http.run(r)
      val res = Await.result(f, 120.seconds)
      assert(res.body contains "2 (number)")
    }
  }
}
