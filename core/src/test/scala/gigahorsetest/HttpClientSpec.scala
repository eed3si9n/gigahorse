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

class HttpClientSpec extends FlatSpec with Matchers {
  "HttpClient" should "retrieve a resource with run method" in {
    import gigahorse.Gigahorse
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

  it should "download a resource with download method" in {
    import gigahorse.Gigahorse
    Gigahorse.withHttp(Gigahorse.config) { http =>
      IO.withTemporaryDirectory{ dir =>
        val file = dir / "Google_2015_logo.svg"
        val r = Gigahorse.url("https://upload.wikimedia.org/wikipedia/commons/2/2f/Google_2015_logo.svg")
        val f = http.download(r, file)
        val res = Await.result(f, 120.seconds)
        assert(file.exists)
      }
    }
  }
}
