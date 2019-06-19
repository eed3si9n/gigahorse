---
out:processing.html
---

  [concepts]: concepts.html

Response の処理
--------------

`Request` 値が構築できたら、次に `HttpClient` に渡して、
`run`、`download`、`processFull`, `runStream`  といったメソッドを使って実行することができる。

### http.run(r, f)

`HttpClient` には多くのメソッドが定義されているが、おそらく最も便利なのは
`http.run(r, f)` メソッドだ。[基本的な概念][concepts]のページで見たようにこれは、
`Request` 値と `FullResponse => A` の関数を受け取る。

Gigahorse は、`Future[String]` を返すために `Gigahorse.asString` という関数を提供するが、
これを拡張して他の型に応用できることは想像に難くない。

一つ注意するべきなのは、`run` メソッドは HTTP 2XX 番台のステータスのみを受け付け、
その他の場合は `Future` を失敗させるということだ。(デフォルトの設定では、3XX のリダイレクトは自動的に処理される)

### Future の後処理

関数を渡すのに加え、中の値を map することで簡単に `Future` を後付けで処理することができる。

```scala
import gigahorse._, support.okhttp.Gigahorse
import scala.concurrent._, duration._
import ExecutionContext.Implicits._
val http = Gigahorse.http(Gigahorse.config)

val r = Gigahorse.url("https://api.duckduckgo.com").get.
  addQueryString(
    "q" -> "1 + 1"
  )
val f0: Future[FullResponse] = http.run(r, identity)
val f: Future[String] = f0 map { Gigahorse.asString andThen (_.take(60)) }
Await.result(f, 120.seconds)
```

`Future` に対して何らかの演算を行うときは、implicit な実行コンテキストが必要となる。
実行コンテキストは、`Future` のコールバックがどのスレッドプールで実行されるかを宣言するものだ。

便宜上、`Request` のみを受け取る `run` のオーバーロードもある。

### FullResponse を Either に持ち上げる

`Future` が失敗する場合があるときによく行われる処理として、内部の `A` を
`Either[Throwable, A]` に持ち上げるということが行われる。

<http://getstatuscode.com/> という便利なウェブサイトがあって、これは
任意の HTTP ステータスを返すことができる。失敗した `Future` に対してブロックするとどうなるかをみてみよう。

```scala
val r = Gigahorse.url("http://getstatuscode.com/500")
val f = http.run(r, Gigahorse.asString)
Await.result(f, 120.seconds)
```

 `Gigahorse.asEither` という機構を使って `A` を `Either[Throwable, A]` に持ち上げることができる。

```scala
val r = Gigahorse.url("http://getstatuscode.com/500")
val f = http.run(r, Gigahorse.asEither)
Await.result(f, 120.seconds)
```

`asEither` は右バイアスのかかった `Either` として `map` することもできる。

```scala
val r = Gigahorse.url("http://getstatuscode.com/200")
val f = http.run(r, Gigahorse.asEither map {
          Gigahorse.asString andThen (_.take(60)) })
Await.result(f, 120.seconds)
```

### http.processFull(r, f)

non-2XX レスポンスでエラーを投げたくなくて、例えば 500 レスポンスのボディーテキストを
読み込みたい場合は `processFull` メソッドを使う。

```scala
val r = Gigahorse.url("http://getstatuscode.com/500")
val f = http.processFull(r, Gigahorse.asString andThen (_.take(60)))
Await.result(f, 120.seconds)
```
