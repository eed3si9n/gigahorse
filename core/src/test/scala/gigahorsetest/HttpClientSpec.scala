package gigahorsetest

import org.scalatest._
import scala.concurrent._
import scala.concurrent.duration._

class HttpClientSpec extends FlatSpec with Matchers {
  "HttpClient" should "retrieve a resource with run method" in {
    import gigahorse.Gigahorse
    Gigahorse.withHttp(Gigahorse.config) { http =>
      val r = Gigahorse.url("http://api.duckduckgo.com").
        withQueryString(Map(
          "q" -> List("1 + 1"),
          "format" -> List("json")
        )).get
      val f = http.run(r)
      val res = Await.result(f, 120.seconds)
      assert(res.body contains "2 (number)")
    }
  }
}
