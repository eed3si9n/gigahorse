Gigahorse
==========

Gigahorse is an HTTP client for Scala with Async Http Client undernieth.

![Giga Horse](docs/files/gigahorse.jpg)

dependencies
------------

The code is adopted from Play WS API, except it no longer depends on Play.
Gigahorse depends on Scala, [AHC 1.9][ahc], which brings in [Netty 3][netty],
[Lightbend SSL Config][sslconfig], and [Lightbend Config][config].

usage
-----

```scala
import scala.concurrent._
import scala.concurrent.duration._
import gigahorse.Gigahorse

Gigahorse.withHttp(Gigahorse.config) { http =>
  val r = Gigahorse.url("http://api.duckduckgo.com").
    addQueryString(
      "q"      -> "1 + 1",
      "format" -> "json"
    ).get
  val f = http.run(r)
  val res = Await.result(f, 120.seconds)
  assert(res.body contains "2 (number)")
}
```

license
-------

Apache 2.0

  [ahc]: https://github.com/AsyncHttpClient/async-http-client/tree/1.9.x
  [netty]: http://netty.io
  [sslconfig]: https://github.com/typesafehub/ssl-config
  [config]: https://github.com/typesafehub/config
