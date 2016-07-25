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
        withQueryString(Map(
          "q" -> List("1 + 1"),
          "format" -> List("json")
        )).get
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
