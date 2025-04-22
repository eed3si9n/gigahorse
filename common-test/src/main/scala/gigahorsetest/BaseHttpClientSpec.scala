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

import gigahorse.{
  FileUtil,
  FormPart,
  HeaderNames,
  MimeTypes,
  MultipartFormBody,
  SignatureCalculator,
  WebSocketEvent,
}
import org.scalatest.Assertion
import org.scalatest.funsuite.AsyncFunSuite
import unfiltered.netty.Server

import java.io.File
import java.nio.charset.Charset
import scala.concurrent.*
import scala.util.Success

abstract class BaseHttpClientSpec extends AsyncFunSuite with TestHttpServer {
  val wsPort = unfiltered.util.Port.any
  def wsTestUrl: String = s"ws://localhost:$wsPort"
  def getWsServer = wsSetup(Server.local(wsPort))
  def wsSetup: Server => Server = {
    _.handler(WsTestPlan.testPlan)
  }
  def isWebSocketSupported: Boolean = true
  def uploadEndpoint = "upload"

  // custom loan pattern
  def withHttp(testCode: gigahorse.HttpClient => Future[Assertion]): Future[Assertion]
  private[this] val Gigahorse = gigahorse.GigahorseSupport

  test("http.run(r) should retrieve a resource from Wikipedia") {
    withHttp { http =>
      val r = Gigahorse
        .url("https://en.wikipedia.org/w/api.php")
        .addQueryString(
          "action" -> "query",
          "format" -> "json",
          "titles" -> "Mad_Max"
        )
        .get
        .addHeaders(
          HeaderNames.ACCEPT -> "application/json"
        )
      val f = http.run(r)
      f map { res =>
        assert(res.bodyAsString contains "Mad Max")
      }
    }
  }

  /*
  test("it should retrieve a resource from Duckduckgo.com") {
    withHttp { http =>
      val r = Gigahorse.url("http://duckduckgo.com").
        addQueryString(
          "q" -> "1 + 1",
          "format" -> "json"
        ).get
      val f = http.run(r)
      f map { res =>
        assert(res.bodyAsString contains "2 (number)")
      }
    }
  }
   */

  test("http.run(r, Gigahorse.asString) should retrieve a resource as String") {
    withHttp { http =>
      val r = Gigahorse
        .url("https://en.wikipedia.org/w/api.php")
        .addQueryString(
          "action" -> "query",
          "format" -> "json",
          "titles" -> "Mad_Max"
        )
        .get
      val f = http.run(r, Gigahorse.asString)
      f map { s =>
        assert(s contains "Mad Max")
      }
    }
  }

  test("http.run(r.withAuth(\"***\", \"***\"), Gigahorse.asString) should retrieve a resource as String") {
    withHttp { http =>
      val r = Gigahorse.url(s"${testUrl}auth")
      val f = http.run(r.withAuth("admin", "***"), Gigahorse.asString)
      f map { s =>
        assert(s contains "auth ok")
      }
    }
  }

  test("http.run(r.withHeaders(...), f) should post with headers") {
    withHttp { http =>
      val r = Gigahorse.url(s"${testUrl}bearer")
        .withHeaders("Authorization" -> "Bearer token123")
      val f = http.run(
        r.post("hello world"),
        Gigahorse.asString
      )
      f map { s =>
        assert(s === "Bearer token123")
      }
    }
  }

  test("http.run(r.post(Map(\"inputString\" -> List(\"{}\"))), f) should post url-form-encoded data") {
    withHttp { http =>
      val r = Gigahorse.url(s"${testUrl}form")
      val f = http.run(r.post(Map("arg1" -> List("{}"))), Gigahorse.asString)
      f map { s =>
        assert(s === "{}")
      }
    }
  }

  test("http.run(r.withContentType(MimeTypes.Text, ISO-8859-1), f) should parse and post with correct content type") {
    withHttp { http =>
      val r = Gigahorse.url(s"${testUrl}charset")
      val f = http.run(
        r.withContentType(MimeTypes.TEXT, Charset.forName("ISO-8859-1"))
          .post("hello world", Charset.forName("ISO-8859-1")),
        Gigahorse.asString
      )
      f map { s =>
        assert(s === "text/plain;charset=ISO-8859-1")
      }
    }
  }

  test("http.run(r.get.withSignatureOpt(...), Gigahorse.asString) should add a signature header") {
    withHttp { http =>
      val r = Gigahorse.url(s"${testUrl}sign").addQueryString("query" -> "param1")
      val sc = new SignatureCalculator {
        override def sign(
            url: String,
            contentType: Option[String],
            content: Array[Byte]
        ): (String, String) =
          ("X-Signature", s"$url:${new String(content, "UTF-8")}:${contentType.getOrElse("")}")
      }
      val f = http.run(r.withSignatureOpt(sc).get, Gigahorse.asString)
      f map { s =>
        assert(s == s"${testUrl}sign?query=param1:::param1")
      }
    }
  }

  test("http.run(r.post.withSignatureOpt(...), Gigahorse.asString) should add a signature header and keep content") {
    withHttp { http =>
      val r = Gigahorse.url(s"${testUrl}sign").addQueryString("query" -> "param1")
      val sc = new SignatureCalculator {
        override def sign(
            url: String,
            contentType: Option[String],
            content: Array[Byte]
        ): (String, String) =
          ("X-Signature", s"$url:${new String(content, "UTF-8")}:${contentType.getOrElse("")}")
      }
      val f =
        http.run(r.withSignatureOpt(sc).post(Map("content" -> List("param2"))), Gigahorse.asString)
      f map { s =>
        assert(
          s == s"${testUrl}sign?query=param1:content=param2:application/x-www-form-urlencoded:param1:param2"
        )
      }
    }
  }

  test("http.websocket(r) should open a websocket connection and exchange messages") {
    (if (isWebSocketSupported)
       withHttp { http =>
         import WebSocketEvent.*
         val r = Gigahorse.url(wsTestUrl).get
         val p = Promise[String]()
         val m = "Hello World!"
         val h: PartialFunction[WebSocketEvent, Unit] = { case TextMessage(ws, message) =>
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
     else cancel())
  }

  /*
  test("http.run(r, Gigahorse.asEither) should retrieve a resource and convert to Right") {
    withHttp { http =>
      val r = Gigahorse.url("http://duckduckgo.com").
        addQueryString(
          "q" -> "1 + 1",
          "format" -> "json"
        ).get
      val f = http.run(r, Gigahorse.asEither map Gigahorse.asString)
      f map { either =>
        assert(either.right.get.toString contains "2 (number)")
      }
    }
  }
   */

  test("it should retrieve a resource and convert to Left given 500") {
    withHttp { http =>
      val r = Gigahorse.url(s"${testUrl}500")
      val f = http.run(r, Gigahorse.asEither)
      f map { either =>
        assert(either.left.get.toString contains "Unexpected status: 500")
      }
    }
  }

  test("http.download should download a resource") {
    withHttp { http =>
      withTemporaryDirectory { dir =>
        val file = new File(dir, "a.json")
        val r = Gigahorse.url(s"${testUrl}download")
        val f = http.download(r, file)
        f.map { (x) =>
          val s = FileUtil.read(file)
          assert(s == """{
  "a": null
}""")
        }
      }
    }
  }

  test("http.processFull(r) should preserve an error response") {
    withHttp { http =>
      val r = Gigahorse.url(s"${testUrl}500")
      for {
        res <- http.processFull(r)
      } yield assert(res.bodyAsString contains "500 HTTP Status Code")
    }
  }

  test("http.processFull(r, Gigahorse.asEither) should preserve an error response and convert to Right given 404") {
    withHttp { http =>
      val r = Gigahorse.url(s"${testUrl}404")
      val f = http.processFull(r, Gigahorse.asEither)
      f map { either =>
        assert(either.right.get.bodyAsString contains "404 HTTP Status Code")
      }
    }
  }

  test("http.processFull(r) should upload files") {
    withHttp { http =>
      withTemporaryDirectory { dir =>
        val file = new File(dir, "a.json")
        val content = """{
    "b": null
  }"""
        FileUtil.write(file, content)
        val r = Gigahorse
          .url(s"${testUrl}${uploadEndpoint}")
          .post(file)
          .withContentType("application/json")
        for {
          res <- http.processFull(r)
        } yield assert(res.bodyAsString == content)
      }
    }
  }

  test("http.processFull(r) should upload multipart form") {
    withHttp { http =>
      withTemporaryDirectory { dir =>
        val file = new File(dir, "a.json")
        val content = """{
    "a": 1
  }"""
        FileUtil.write(file, content)
        val content2 = "bbb"
        val r = Gigahorse
          .url(s"${testUrl}multipart")
          .post(
            MultipartFormBody(
              FormPart("a", content2, "text/plain"),
              FormPart("a.json", file, "application/json")
            )
          )
        for {
          res <- http.processFull(r)
        } yield assert(res.bodyAsString == content + "\n" + content2)
      }
    }
  }

  /** The maximum number of times a unique temporary filename is attempted to be created. */
  private[this] val MaximumTries = 10
  private[this] val random = new java.util.Random
  private[this] val temporaryDirectory = new File(System.getProperty("java.io.tmpdir"))
  def withTemporaryDirectory(testCode: File => Future[Assertion]): Future[Assertion] = {
    val dir = createUniqueDirectory(temporaryDirectory)
    complete {
      testCode(dir)
    } lastly {
      dir.delete()
    }
  }
  private[this] def createUniqueDirectory(baseDirectory: File): File = {
    def create(tries: Int): File = {
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
