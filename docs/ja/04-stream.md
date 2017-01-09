Reactive Stream を用いた非同期処理
--------------------------------

ここまでは、ボディーコンテンツの全てをメモリ上に受け取った
`FullResponse` の処理をみてきた。
コンテンツが比較的小さい場合はそれでもいいかもしれないが、
例えばファイルをダウンロードする場合などはコンテンツの
チャンクを受け取り次第に処理していきたい。

### ファイルのダウンロード

`http.download` メソッドを使ってファイルをダウンロードすることができる。

```console:new
scala> import gigahorse._, support.asynchttpclient.Gigahorse
scala> import scala.concurrent._, duration._
scala> import ExecutionContext.Implicits._
scala> import java.io.File
scala> Gigahorse.withHttp(Gigahorse.config) { http =>
         val file = new File(new File("target"), "Google_2015_logo.svg")
         val r = Gigahorse.url("https://upload.wikimedia.org/wikipedia/commons/2/2f/Google_2015_logo.svg")
         val f = http.download(r, file)
         Await.result(f, 120.seconds)
       }
```

これは `Future[File]` を返す。

### http.runStream(r, f)

`http.runStream(r, f)` を使うと返ってきたレスポンスを
Reactive Stream として取り扱って、パーツごとに処理することができる。


```scala
  /** Runs the request and return a Future of A. */
  def runStream[A](request: Request, f: StreamResponse => Future[A]): Future[A]
```

ここで注目してほしいのは、関数が `FullResponse` ではなくて `StreamResponse` を受け取ることだ。`FullResponse` と違って、`StreamResponse` はボディーコンテンツをまだ受け取っていない。

その代わりに `StreamResponse` は、コンテンツのパーツをオンデマンドで受け取る
`Stream[A]` を作ることができる。
出発点として、Gigahorse は `Gigahorse.asByteStream` と
`Gigahorse.asStringStream` を提供する。

`Stream[A]` はこのような実装になっている:

```scala
import org.reactivestreams.Publisher
import scala.concurrent.Future

abstract class Stream[A] {
  /**
   * @return The underlying Stream object.
   */
  def underlying[A]

  def toPublisher: Publisher[A]

  /** Runs f on each element received to the stream. */
  def foreach(f: A => Unit): Future[Unit]

  /** Runs f on each element received to the stream with its previous output. */
  def fold[B](zero: B)(f: (B, A) => B): Future[B]

  /** Similar to fold but uses first element as zero element. */
  def reduce(f: (A, A) => A): Future[A]
}
```

これを使えば比較的簡単にストリーム処理を行うことができる。
例えば、`download` は以下のように実装されている。

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

これはパーツが届くと `FileOutputStream` に書き込んでいる。

### 改行区切りのストリーム

Akka HTTP を使った例もみてみる。
`\$ python -m SimpleHTTPServer 8000` を実行してカレントディレクトリを
8000番ポートでサーブしているとして、
`README.markdown` の各行を表示したい。

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

うまくいった。これは JSON が入った無限ストリームを処理するのに使える。
