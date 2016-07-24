package gigahorse

import org.scalatest._
import scala.concurrent._
import scala.concurrent.duration._

class HttpClientSpec extends FlatSpec with Matchers {
  "HttpClient" should "retrieve a resource with execute method" in {
    import gigahorse._
    Gigahorse.withHttp(Gigahorse.config) { http =>
      val r = Gigahorse.url("http://api.duckduckgo.com").
        withQueryString(Map(
          "q" -> List("1 + 1"),
          "format" -> List("json")
        ))
      val f = http.execute(r)
      val res = Await.result(f, 120.seconds)
      assert(res.body contains "2 (number)")
    }
  }
}
