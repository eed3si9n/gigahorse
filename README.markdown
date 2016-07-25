Gigahorse
==========

Gigahorse is an HTTP client for Scala with Async Http Client undernieth.

The code is adopted from Play WS API, except it doesn't depend on anything other than Scala and AHC.


usage
-----

```scala
import scala.concurrent._
import scala.concurrent.duration._
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
```

license
-------

Apache 2.0
