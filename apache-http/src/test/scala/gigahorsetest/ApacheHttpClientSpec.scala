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

import org.scalatest.*
import scala.concurrent.Future

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

  override def isWebSocketSupported: Boolean = false
}
