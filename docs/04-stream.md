Asynchronous processing with Reactive Stream
--------------------------------------------

Thus far we've been looking at processing `FullResponse`,
which already retrieved the entire body contents in-memory.
When the content is relatively small, it's fine,
but for things like downloading files, we would want
to process the content by chunks as we receive them.

### Downloading a file

A file can be downloaded using `http.download` method:

```console:new
scala> import gigahorse._, support.okhttp.Gigahorse
scala> import scala.concurrent._, duration._
scala> import ExecutionContext.Implicits._
scala> import java.io.File
scala> val http = Gigahorse.http(Gigahorse.config)
scala> {
         val file = new File(new File("target"), "Google_2015_logo.svg")
         val r = Gigahorse.url("https://upload.wikimedia.org/wikipedia/commons/2/2f/Google_2015_logo.svg")
         val f = http.download(r, file)
         Await.result(f, 120.seconds)
       }
```

This will return `Future[File]`.

### http.runStream(r, f)

We can treat the incoming response as a Reactive Stream,
and work on them part by part using `http.runStream(r, f)`.

```scala
  /** Runs the request and return a Future of A. */
  def runStream[A](request: Request, f: StreamResponse => Future[A]): Future[A]
```

Note that the function takes a `StreamResponse` instead of a `FullResponse`. Unlike the `FullResponse`, it does not have the body contents received yet.

Instead, `StreamResponse` can create `Stream[A]` that will retrieve the parts on-demand.
As a starting point, Gigahorse provides `Gigahorse.asByteStream` and `Gigahorse.asStringStream`.

Here's how `Stream[A]` looks like:

```scala
import org.reactivestreams.Publisher
import scala.concurrent.Future

abstract class Stream[A] {
  /**
   * @return The underlying Stream object.
   */
  def underlying[B]: B

  def toPublisher: Publisher[A]

  /** Runs f on each element received to the stream. */
  def foreach(f: A => Unit): Future[Unit]

  /** Runs f on each element received to the stream with its previous output. */
  def fold[B](zero: B)(f: (B, A) => B): Future[B]

  /** Similar to fold but uses first element as zero element. */
  def reduce(f: (A, A) => A): Future[A]
}
```

Using this, processing stream at relative ease.
For example, `download` is implementing as follows:

```scala
  def download(request: Request, file: File): Future[File] =
    runStream(request, asFile(file))

....

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.io.{ File, FileOutputStream }
import scala.concurrent.Future

object DownloadHandler {
  /** Function from `StreamResponse` to `Future[File]` */
  def asFile(file: File): StreamResponse => Future[File] = (response: StreamResponse) =>
    {
      val stream = response.byteBuffers
      val out = new FileOutputStream(file).getChannel
      stream.fold(file)((acc, bb) => {
        out.write(bb)
        acc
      })
    }
}
```

`stream.fold` will write into the `FileOutputStream` as the parts arrive.

### Newline delimited stream

Here's another example, this time using Akka HTTP.
Suppose we are running `\$ python -m SimpleHTTPServer 8000`, which serves the current directory over port 8000, and let's say we want to take `README.markdown` and print each line:

```scala
scala> import gigahorse._, support.akkahttp.Gigahorse
import gigahorse._
import support.akkahttp.Gigahorse

scala> import scala.concurrent._, duration._
import scala.concurrent._
import duration._

scala> Gigahorse.withHttp(Gigahorse.config) { http =>
         val r = Gigahorse.url("http://localhost:8000/README.markdown").get
         val f = http.runStream(r, Gigahorse.asStringStream andThen { xs =>
           xs.foreach { s => println(s) }
         })
         Await.result(f, 120.seconds)
       }
Gigahorse
==========

Gigahorse is an HTTP client for Scala with Async Http Client or Lightbend Akka HTTP underneath.
....
```

It worked. This could be used for process an infinite stream of JSON.
