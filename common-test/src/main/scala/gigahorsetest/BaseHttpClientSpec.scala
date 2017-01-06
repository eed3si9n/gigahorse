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
import scala.util.{ Failure, Success }
import scala.concurrent._
import scala.concurrent.duration._
import java.io.File
import gigahorse.{ HeaderNames, WebSocketEvent }

abstract class BaseHttpClientSpec extends AsyncFlatSpec with Matchers {
  // custom loan pattern
  def withHttp(testCode: gigahorse.HttpClient => Future[Assertion]): Future[Assertion]
  private[this] val Gigahorse = gigahorse.GigahorseSupport

  "http.run(r)" should "retrieve a resource from Wikipedia" in {
    withHttp { http =>
      val r = Gigahorse.url("https://en.wikipedia.org/w/api.php").
        addQueryString(
          "action" -> "query",
          "format" -> "json",
          "titles" -> "Mad_Max"
        ).get.
        addHeaders(
          HeaderNames.ACCEPT -> "application/json"
        )
      val f = http.run(r)
      f map { res =>
        assert(res.bodyAsString contains "Mad Max")
      }
    }
  }

  it should "retrieve a resource from Duckduckgo.com" in
    withHttp { http =>
      val r = Gigahorse.url("http://api.duckduckgo.com").
        addQueryString(
          "q" -> "1 + 1",
          "format" -> "json"
        ).get
      val f = http.run(r)
      f map { res =>
        assert(res.bodyAsString contains "2 (number)")
      }
    }

  "http.run(r, Gigahorse.asString)" should "retrieve a resource as String" in
    withHttp { http =>
      val r = Gigahorse.url("https://en.wikipedia.org/w/api.php").
        addQueryString(
          "action" -> "query",
          "format" -> "json",
          "titles" -> "Mad_Max"
        ).get
      val f = http.run(r, Gigahorse.asString)
      f map { s =>
        assert(s contains "Mad Max")
      }
    }

  "http.websocket(r)" should "open a websocket connection and exchange messages" in
    withHttp { http =>
      import WebSocketEvent._
      val r = Gigahorse.url("ws://echo.websocket.org").get
      val p = Promise[String]()
      val m = "Hello World!"
      val h: PartialFunction[WebSocketEvent, Unit] = {
        case TextMessage(ws, message) =>
          p.complete(Success(message))
          ws.close()
      }
      val f = http.websocket(r)(h) flatMap { ws =>
        ws.sendMessage(m)
        p.future
      }
      f map { s =>
        assert(s === m)
      }
    }

  "http.run(r, Gigahorse.asEither)" should "retrieve a resource and convert to Right" in
    withHttp { http =>
      val r = Gigahorse.url("http://api.duckduckgo.com").
        addQueryString(
          "q" -> "1 + 1",
          "format" -> "json"
        ).get
      val f = http.run(r, Gigahorse.asEither map Gigahorse.asString)
      f map { either =>
        assert(either.right.get.toString contains "2 (number)")
      }
    }

  it should "retrieve a resource and convert to Left given 500" in
    withHttp { http =>
      val r = Gigahorse.url("http://getstatuscode.com/500")
      val f = http.run(r, Gigahorse.asEither)
      f map { either =>
        assert(either.left.get.toString contains "Unexpected status: 500")
      }
    }

  "http.download" should "download a resource" in
    withHttp { http =>
      withTemporaryDirectory{ dir =>
        val file = new File(dir, "Google_2015_logo.svg")
        val r = Gigahorse.url("https://upload.wikimedia.org/wikipedia/commons/2/2f/Google_2015_logo.svg")
        val f = http.download(r, file)
        f map { x =>
          assert(file.exists)
        }
      }
    }

  "http.process(r)" should "preserve an error response" in
    withHttp { http =>
      val r = Gigahorse.url("http://getstatuscode.com/500")
      for {
        res  <- http.process(r)
      } yield assert(res.bodyAsString contains "500 HTTP Status Code")
    }

  "http.process(r, Gigahorse.asEither)" should "preserve an error response and convert to Right given 404" in
    withHttp { http =>
      val r = Gigahorse.url("http://getstatuscode.com/404")
      val f = http.process(r, Gigahorse.asEither)
      f map { either =>
        assert(either.right.get.bodyAsString contains "404 HTTP Status Code")
      }
    }

  /** The maximum number of times a unique temporary filename is attempted to be created.*/
  private[this] val MaximumTries = 10
  private[this] val random = new java.util.Random
  private[this] val temporaryDirectory = new File(System.getProperty("java.io.tmpdir"))
  def withTemporaryDirectory(testCode: File => Future[Assertion]): Future[Assertion] =
    {
      val dir = createUniqueDirectory(temporaryDirectory)
      complete {
        testCode(dir)
      } lastly {
        dir.delete()
      }
    }
  private[this] def createUniqueDirectory(baseDirectory: File): File =
    {
      def create(tries: Int): File =
        {
          if (tries > MaximumTries)
            sys.error("Could not create temporary directory.")
          else {
            val randomName = "sbt_" + java.lang.Integer.toHexString(random.nextInt)
            val f = new File(baseDirectory, randomName)

            try { f.mkdirs; f }
            catch { case e: Exception => create(tries + 1) }
          }
        }
      create(0)
    }
}
