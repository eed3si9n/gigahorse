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

import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap
import org.scalatest.*
import scala.concurrent.Future
import gigahorse.FileUtil
import gigahorse.shaded.apache.org.apache.hc.core5
import core5.http.{ EntityDetails, HttpHeaders, HttpRequest, Message, URIScheme }
import core5.http.message.BasicHttpResponse
import core5.http.nio.{ AsyncRequestConsumer, AsyncServerRequestHandler }
import core5.http.nio.entity.BasicAsyncEntityConsumer
import core5.http.nio.support.{ BasicRequestConsumer, BasicResponseProducer }
import core5.http.protocol.HttpContext
import core5.http2.HttpVersionPolicy as XHttpVersionPolicy
import core5.http2.impl.nio.bootstrap.H2ServerBootstrap
import core5.io.CloseMode

class ApacheHttpClientSpec extends BaseHttpClientSpec {
  import gigahorse.support.apachehttp.Gigahorse

  test("withFollowRedirects(false) should disable redirects") {
    withHttp { http =>
      val request = Gigahorse.url(s"${testUrl}redirect").withFollowRedirects(false)
      http.processFull(request).map { response =>
        assert(response.status == 307)
      }
    }
  }

  test("client configuration should disable redirects") {
    withHttpConfig(Gigahorse.config.withFollowRedirects(false)) { http =>
      val request = Gigahorse.url(s"${testUrl}redirect")
      http.processFull(request).map { response =>
        assert(response.status == 307)
      }
    }
  }

  test("request configuration should override client redirect configuration") {
    withHttpConfig(Gigahorse.config.withFollowRedirects(false)) { http =>
      val request = Gigahorse.url(s"${testUrl}redirect").withFollowRedirects(true)
      http.processFull(request).map { response =>
        assert(response.bodyAsString == "redirect ok")
      }
    }
  }

  test("httpVersionPolicy of Http1_1 should still complete a request") {
    withHttpConfig(Gigahorse.config.withHttpVersionPolicy(gigahorse.HttpVersionPolicy.Http1_1)) {
      http =>
        val r = Gigahorse.url(s"${testUrl}auth")
        http.processFull(r).map { response =>
          assert(response.status == 401)
        }
    }
  }

  // The test server speaks HTTP/1.1 only, so forcing HTTP/2 must fail. If the policy were being
  // dropped on the floor the request would quietly succeed over HTTP/1.1 instead.
  test("httpVersionPolicy of Http2 should be applied to the connection") {
    withHttpConfig(Gigahorse.config.withHttpVersionPolicy(gigahorse.HttpVersionPolicy.Http2)) {
      http =>
        val r = Gigahorse.url(s"${testUrl}auth")
        http.processFull(r).failed.map { e =>
          assert(e ne null)
        }
    }
  }

  // HttpCore leaves Content-Length out on HTTP/2, and servers such as AWS CodeArtifact answer an
  // upload without it with 411 Length Required.
  test("uploads over HTTP/2 should send Content-Length") {
    withH2Server { (port, seen) =>
      val http = Gigahorse.http(
        Gigahorse.config.withHttpVersionPolicy(gigahorse.HttpVersionPolicy.Http2)
      )
      val dir = Files.createTempDirectory("gigahorse-h2")
      val file = dir.resolve("a.json").toFile
      FileUtil.write(file, "{}")
      val url = s"http://localhost:$port/"
      val uploads = for {
        f <- http.processFull(Gigahorse.url(url + "file").put(file))
        s <- http.processFull(Gigahorse.url(url + "string").put("hello", StandardCharsets.UTF_8))
      } yield (f.status, s.status)
      complete {
        uploads.map { statuses =>
          assert(seen.get("/file") == "2")
          assert(seen.get("/string") == "5")
          assert(statuses == ((201, 201)))
        }
      } lastly {
        http.close()
        file.delete()
        Files.delete(dir)
      }
    }
  }

  test("client configuration should supply credentials preemptively") {
    withHttpConfig(Gigahorse.config.withAuth("admin", "***")) { http =>
      val r = Gigahorse.url(s"${testUrl}preemptive-auth")
      http.run(r, Gigahorse.asString).map { s =>
        assert(s contains "auth ok")
      }
    }
  }

  test("client configuration should supply credentials to answer a challenge") {
    val auth = gigahorse
      .Realm(username = "admin", password = "***")
      .withUsePreemptiveAuth(false)
    withHttpConfig(Gigahorse.config.withAuth(auth)) { http =>
      val r = Gigahorse.url(s"${testUrl}auth")
      http.run(r, Gigahorse.asString).map { s =>
        assert(s contains "auth ok")
      }
    }
  }

  test("request credentials should override client configuration") {
    withHttpConfig(Gigahorse.config.withAuth("wrong-user", "***")) { http =>
      val r = Gigahorse.url(s"${testUrl}preemptive-auth").withAuth("admin", "***")
      http.run(r, Gigahorse.asString).map { s =>
        assert(s contains "auth ok")
      }
    }
  }

  test("credentials configured for one realm should not answer another realm's challenge") {
    withHttp { http =>
      // the /auth-realm-mismatch route challenges with `Basic realm="some-other-realm"`
      val r = Gigahorse.url(s"${testUrl}auth-realm-mismatch")
      val auth = gigahorse
        .Realm(username = "admin", password = "***")
        .withRealmName("configured-realm")
        .withUsePreemptiveAuth(false)
      http.processFull(r.withAuth(auth)).map { response =>
        assert(response.status == 401)
      }
    }
  }

  test("credentials should not be sent to a redirect target on a different host") {
    withHttp { http =>
      val r = Gigahorse.url(s"${testUrl}redirect-cross-host")
      http.processFull(r.withAuth("admin", "***")).map { response =>
        assert(response.status == 401)
      }
    }
  }

  // custom loan pattern
  override def withHttp(testCode: gigahorse.HttpClient => Future[Assertion]): Future[Assertion] =
    withHttpConfig(Gigahorse.config)(testCode)

  private def withHttpConfig(config: gigahorse.Config)(
      testCode: gigahorse.HttpClient => Future[Assertion]
  ): Future[Assertion] = {
    val server = getServer
    server.start()
    val wsServer = getWsServer
    wsServer.start()
    val http = Gigahorse.http(config)
    complete {
      testCode(http)
    } lastly {
      http.close()
      wsServer.stop()
      wsServer.destroy()
      server.stop()
      server.destroy()
    }
  }

  // An HTTP/2-only server (h2c, prior knowledge) that records each request's Content-Length by path.
  private def withH2Server(
      testCode: (Int, ConcurrentHashMap[String, String]) => Future[Assertion]
  ): Future[Assertion] = {
    val seen = new ConcurrentHashMap[String, String]()
    val handler = new AsyncServerRequestHandler[Message[HttpRequest, Array[Byte]]] {
      override def prepare(
          request: HttpRequest,
          entityDetails: EntityDetails,
          context: HttpContext
      ): AsyncRequestConsumer[Message[HttpRequest, Array[Byte]]] = {
        val length = request.getFirstHeader(HttpHeaders.CONTENT_LENGTH)
        seen.put(request.getPath, if (length == null) "missing" else length.getValue)
        new BasicRequestConsumer[Array[Byte]](new BasicAsyncEntityConsumer())
      }
      override def handle(
          message: Message[HttpRequest, Array[Byte]],
          responseTrigger: AsyncServerRequestHandler.ResponseTrigger,
          context: HttpContext
      ): Unit =
        responseTrigger.submitResponse(
          new BasicResponseProducer(new BasicHttpResponse(201)),
          context
        )
    }
    val server = H2ServerBootstrap
      .bootstrap()
      .setVersionPolicy(XHttpVersionPolicy.FORCE_HTTP_2)
      // Otherwise the server answers requests for "localhost" with 421 on hosts with another name.
      .setCanonicalHostName("localhost")
      .register("*", handler)
      .create()
    server.start()
    val endpoint = server.listen(new InetSocketAddress("localhost", 0), URIScheme.HTTP).get()
    val port = endpoint.getAddress.asInstanceOf[InetSocketAddress].getPort
    complete {
      testCode(port, seen)
    } lastly {
      server.close(CloseMode.GRACEFUL)
    }
  }

  override def isWebSocketSupported: Boolean = false
}
