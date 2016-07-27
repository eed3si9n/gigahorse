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
  "http.run(r)" should "retrieve a resource" in {
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

  "http.run(r, Gigahorse.toEither)" should "retrieve a resource and convert to Right" in {
    import gigahorse.Gigahorse
    import scala.concurrent.ExecutionContext.Implicits._
    Gigahorse.withHttp(Gigahorse.config) { http =>
      val r = Gigahorse.url("http://api.duckduckgo.com").
        addQueryString(
          "q" -> "1 + 1",
          "format" -> "json"
        ).get
      val f = http.run(r, Gigahorse.toEither map { r => r.body })
      val res = Await.result(f, 120.seconds)
      assert(res.toString contains "2 (number)")
    }
  }

  it should "retrieve a resource and convert to Left given 500" in {
    import gigahorse.Gigahorse
    import scala.concurrent.ExecutionContext.Implicits._
    Gigahorse.withHttp(Gigahorse.config) { http =>
      val r = Gigahorse.url("http://getstatuscode.com/500")
      val f = http.run(r, Gigahorse.toEither)
      val res = Await.result(f, 120.seconds)
      assert(res.left.get.toString contains "Unexpected status: 500")
    }
  }

  "http.download" should "download a resource" in {
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

  "http.process(r)" should "preserve an error response" in {
    import gigahorse.Gigahorse
    import scala.concurrent.ExecutionContext.Implicits._
    Gigahorse.withHttp(Gigahorse.config) { http =>
      val r = Gigahorse.url("http://getstatuscode.com/500")
      val f = http.process(r)
      val res = Await.result(f, 120.seconds)
      assert(res.body contains "500 HTTP Status Code")
    }
  }

  "http.process(r, Gigahorse.toEither)" should "preserve an error response and convert to Right given 404" in {
    import gigahorse.Gigahorse
    import scala.concurrent.ExecutionContext.Implicits._
    Gigahorse.withHttp(Gigahorse.config) { http =>
      val r = Gigahorse.url("http://getstatuscode.com/404")
      val f = http.process(r, Gigahorse.toEither)
      val res = Await.result(f, 120.seconds)
      assert(res.right.get.body contains "404 HTTP Status Code")
    }
  }
}
