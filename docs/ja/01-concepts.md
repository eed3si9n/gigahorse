---
out: concepts.html
---

  [Future]: http://docs.scala-lang.org/ja/overviews/core/futures.html

基本的な概念
-----------

### Gigahorse

`Gigahorse`　は、色々便利なものを作るためのヘルパーオブジェクトだ。

- Apache HTTP をバックエンドに使う場合は、`gigahorse.support.apachehttp.Gigahorse`。
- AHC をバックエンドに使う場合は、`gigahorse.support.asynchttpclient.Gigahorse`。
- OkHttp をバックエンドに使う場合は、`gigahorse.support.okhttp.Gigahorse`。
- Akka HTTP をバックエンドに使う場合は、`gigahorse.support.akkahttp.Gigahorse`。

### HttpClient

`HttpClient` は、複数のリクエストを処理することのできる HTTP クライアントを表す。
使用時には多くのスレッドを生成するので、`HttpClient` のライフタイム管理に気を付ける必要がある。
これに無頓着だと、プログラムはリソース枯渇に陥る可能性がある。

`HttpClient` を作るには 2つの方法がある。
第一の方法としては、`Gigahorse.http(Gigahourse.config)` を使って
`HttpClient` を作ることだ。Apache HTTP や AHC を使ってこの方法を取った場合、**必ずクライアントを閉じる**必要がある。

```console
scala> import gigahorse._, support.apachehttp.Gigahorse
scala> val http = Gigahorse.http(Gigahorse.config)
scala> http.close() // must call close()
```

第二の方法は loan パターン `Gigahorse.withHttp(config) { ... }` を使うことだ:

```scala
import gigahorse._, support.apachehttp.Gigahorse
Gigahorse.withHttp(Gigahorse.config) { http =>
  // do something
}
```

これは、`HttpClient` を閉じることを保証するけども、短所としては
全ての HTTP 処理が完了する前に閉じてしまう可能性があるので、
中で全ての `Future` をブロックさせる必要がある。

### Config

`HttpClient` を作るには `Config` を渡す必要がある。
`Gigahorse.config` は `application.conf` があればそこから設定を読み込んで、
無ければデフォルト値を選んでくれる。

```console
scala> Gigahorse.config
```

### Request

`Request` は、単一の HTTP リクエストを表す不変 (immutable) なデータ型だ。
`HttpClient` と違って、これは比較的に気軽に作ったり、取り回したりすることができる。

リクエストを構築するには、`Gigahorse.url(...)` 関数を呼ぶ:

```console
scala> val r = Gigahorse.url("https://api.duckduckgo.com").get.
         addQueryString(
           "q" -> "1 + 1",
           "format" -> "json"
         )
```

上記のように、新しいリクエスト値を返す呼び出しを連鎖させることができる。

### http.run(r, f)

`HttpClient` には多くのメソッドが定義されているが、おそらく最も便利なのは
`http.run(r, f)` メソッドだ:

```scala
abstract class HttpClient extends AutoCloseable {
  /** Runs the request and return a Future of A. Errors on non-OK response. */
  def run[A](request: Request, f: FullResponse => A): Future[A]

  ....
}
```

第一パラメータは `Request` を受け取り、第二パラメータは `FullResponse` から `A` への関数を受け取る。
`Gigahorse.asString` という関数が予めついてきて、これはボディーのコンテンツを `String` で返す。

さらに、これはただの関数なので `andThen` を使って他の関数と合成することができる:

```console
scala> import gigahorse._, support.apachehttp.Gigahorse
scala> import scala.concurrent._, duration._
scala> val http = Gigahorse.http(Gigahorse.config)
scala> val r = Gigahorse.url("https://api.duckduckgo.com").get.
         addQueryString(
           "q" -> "1 + 1"
         )
scala> val f = http.run(r, Gigahorse.asString andThen {_.take(60)})
scala> Await.result(f, 120.seconds)
scala> http.close()
```

**注意**: OkHttp もしくは Akka HTTP を用いてレスポンスのボディーを消費しない場合は、
リソースを解放するために `close()` メソッドを呼び出す必要がある。

### Future

`run` はリクエストを非同期に実行するため、これは `Future` を返す。
普通は、`Future` をできる限り `Future` のままで保っていると思うが、
ここでは値を見るために敢えてブロックしている。

`Future` の中に値をできる限り保っておく動機として、並列に複数の
`Future` (この場合 HTTP リクエスト) を取り扱うことができるということが挙げられる。
Future に関する詳細は [Futures and Promises][Future] を参照してほしい。

### http.runStream(r, f)

Gigahorse は、全レスポンスに対して `run` するのだけではなく、
返ってきた部分レスポンスを Reactive Stream として扱って、
例えば行ごとに処理するということも可能だ。
