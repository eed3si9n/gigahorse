package gigahorse

import org.scalatest._
import scala.concurrent._
import scala.concurrent.duration._

class HttpClientSpec extends FlatSpec with Matchers {
  "HttpClient" should "retrieve a resource" in {
    import gigahorse._
    Gigahorse.withHttp(Gigahorse.config) { http =>
      val r = http.url("http://api.duckduckgo.com?q=1%20%2B%201&format=json")
      val f = http.execute(r)
      val res = Await.result(f, 120.seconds)
      assert(res.body contains "2 (number)")
    }
  }
}
