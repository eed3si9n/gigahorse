---
out: concepts.html
---

  [Future]: http://docs.scala-lang.org/overviews/core/futures.html

Basic concepts
--------------

### Gigahorse

`Gigahorse` is a helper object to create many useful things.

- For OkHttp backend, use `gigahorse.support.okhttp.Gigahorse`.
- For AHC backend, use `gigahorse.support.asynchttpclient.Gigahorse`.
- For Akka HTTP backend, `gigahorse.support.akkahttp.Gigahorse`.

### HttpClient

The `HttpClient` represents an HTTP client that's able to handle multiple requests.
When it's used it will spawn many threads, so the lifetime of an `HttpClient`
must be managed with care. Otherwise your program will run out of resources.

There are two ways of creating an `HttpClient`.
First is creating using `Gigahorse.http(Gigahourse.config)`.
If you use this with AHC, **you must close** the client yourself:

```console
scala> val http = Gigahorse.http(Gigahorse.config)
scala> http.close() // must call close()
```

Second way is using the loan pattern `Gigahorse.withHttp(config) { ... }`:

```console:new
scala> import gigahorse._, support.asynchttpclient.Gigahorse
scala> Gigahorse.withHttp(Gigahorse.config) { http =>
         // do something
       }
```

This will guarantee to close the `HttpClient`, but the drawback
is that it could close prematurely before HTTP process is done,
so you would have to block inside to wait for all the futures.

### Config

To create an `HttpClient` you need to pass in a `Config`.
`Gigahorse.config` will read from `application.conf` to configure
the settings if it exists. Otherwise, it will pick the default values.

```console
scala> Gigahorse.config
```

### Request

The `Request` is an immutable datatype that represents a single HTTP request.
Unlike `HttpClient` this is relativey cheap to create and keep around.

To construct a request, call `Gigahorse.url(...)` function:

```console
scala> val r = Gigahorse.url("http://api.duckduckgo.com").get.
         addQueryString(
           "q" -> "1 + 1",
           "format" -> "json"
         )
```

You can chain calls like the above, which keeps returning a new request value.

### http.run(r, f)

There are many methods on `HttpClient`, but probably the most useful one is
`http.run(r, f)` method:

```scala
abstract class HttpClient extends AutoCloseable {
  /** Runs the request and return a Future of A. Errors on non-OK response. */
  def run[A](request: Request, f: FullResponse => A): Future[A]

  ....
}
```

The first parameter take a `Request`, and the second parameter takes a function
from `FullResponse` to `A`. There's a built-in function called `Gigahorse.asString`
that returns the body content as a `String`.

Since this is a plain function, you can compose it with some other function
using `andThen`:

```console
scala> import scala.concurrent._, duration._
scala> Gigahorse.withHttp(Gigahorse.config) { http =>
         val r = Gigahorse.url("http://api.duckduckgo.com").get.
           addQueryString(
             "q" -> "1 + 1",
             "format" -> "json"
           )
         val f = http.run(r, Gigahorse.asString andThen {_.take(60)})
         Await.result(f, 120.seconds)
       }
```

**Note**: Using OkHttp or Akka HTTP, if you don't consume the response body,
you must call `close()` method on the `FullResponse` to let go of the resource.

### Future

Because `run` executes a request in a non-blocking fashion, it returns a `Future`.
Normally, you want to keep the `Future` value as long as you can,
but here, we will block it to see the value.

One motivation for keeping the `Future` value as long as you can
is working with multiple Futures (HTTP requests) in parallel.
See [Futures and Promises][Future] to learn more about Futures.

### http.runStream(r, f)

Instead of running on the full reponse,
Gigahorse can also treat the incoming response as a Reactive Stream,
and process them by chunk, for example line by line.
