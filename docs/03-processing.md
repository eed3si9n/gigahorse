---
out:processing.html
---

  [concepts]: concepts.html

Processing the FullResponse
---------------------------

Once you build a `Request` value, you can pass it to `HttpClient`
to execute the request using `run`, `download`, `processFull`, `runStream` methods.

### http.run(r, f)

There are many methods on `HttpClient`, but probably the most useful one is
`http.run(r, f)` method. As we saw in [Basic Concepts][concepts] page
this take a `Request` value, and a function `FullResponse => A`.

Gigahorse provides `Gigahorse.asString` function to return `Future[String]`,
but we can imagine this could be expanded to do more.

Another thing to note is that `run` method will only accept HTTP 2XX statuses,
and fail the future value otherwise. (By default 3XX redirects are handled automatically)

### Post-processing a Future

In addition to passing in a function, a `Future` can easily be post-processed
by mapping inside it.

```console:new
scala> import gigahorse._, support.asynchttpclient.Gigahorse
scala> import scala.concurrent._, duration._
scala> import ExecutionContext.Implicits._
scala> Gigahorse.withHttp(Gigahorse.config) { http =>
         val r = Gigahorse.url("http://api.duckduckgo.com").get.
           addQueryString(
             "q" -> "1 + 1",
             "format" -> "json"
           )
         val f0: Future[FullResponse] = http.run(r, identity)
         val f: Future[String] = f0 map { Gigahorse.asString andThen (_.take(60)) }
         Await.result(f, 120.seconds)
       }
```

Whenever an operation is done on a `Future`, an implicit execution context must be available
-- this declares which thread pool the callback to the future should run in.

For convenience there's an overload of `run` that takes only the `Request` parameter.

### Lifting the FullResponse to Either

One of the common processing when dealing with a Future that can fail is to
lift the inner `A` value to `Either[Throwable, A]`.

There's a convenient website called <http://getstatuscode.com/>
that can emulate HTTP statuses. Here's what happens when we await on a failed Future.

```console:error
scala> Gigahorse.withHttp(Gigahorse.config) { http =>
         val r = Gigahorse.url("http://getstatuscode.com/500")
         val f = http.run(r, Gigahorse.asString)
         Await.result(f, 120.seconds)
       }
```

Gigahorse provides a mechanism called `Gigahorse.asEither` to
lift the inner `A` value to `Either[Throwable, A]` as follows:

```console
scala> Gigahorse.withHttp(Gigahorse.config) { http =>
         val r = Gigahorse.url("http://getstatuscode.com/500")
         val f = http.run(r, Gigahorse.asEither)
         Await.result(f, 120.seconds)
       }
```

`asEither` can be mapped over as a right-biased `Either`.

```console
scala> Gigahorse.withHttp(Gigahorse.config) { http =>
         val r = Gigahorse.url("http://getstatuscode.com/200")
         val f = http.run(r, Gigahorse.asEither map {
           Gigahorse.asString andThen (_.take(60)) })
         Await.result(f, 120.seconds)
       }
```

### http.processFull(r, f)

If you do not wish to throw an error on non-2XX responses, and for example
read the body text of a 500 response, use `processFull` method.

```console
scala> Gigahorse.withHttp(Gigahorse.config) { http =>
         val r = Gigahorse.url("http://getstatuscode.com/500")
         val f = http.processFull(r, Gigahorse.asString andThen (_.take(60)))
         Await.result(f, 120.seconds)
       }
```
