---
out: request.html
---

Request 値の構築
---------------

`Request` 値の構築を構築するには `Gigahorse.url(...)` 関数を呼び出す:

```console:new
scala> import gigahorse._, support.okhttp.Gigahorse
scala> val url = "http://api.duckduckgo.com"
scala> val r = Gigahorse.url(url)
```

次に、`Request` のメソッドをつなげていくことで新しい `Request` 値を作っていく。

### HTTP verb

HTTP verb (GET, POST, PATCH, PUT, DELETE, HEAD, OPTIONS) それぞれに対してメソッドがある。

```console
scala> import java.io.File
scala> Gigahorse.url(url).get
scala> Gigahorse.url(url).post("")
scala> Gigahorse.url(url).post(new File("something.txt"))
```

`post(...)`、`put(...)`、`patch(...)` メソッドは `A: HttpWrite` という context bound のある
型パラメータ `A` を受け取るオーバーロードがあるため、あらゆるカスタム型に対応できるように拡張することができる。

```scala
  /** Uses GET method. */
  def get: Request                                   = this.withMethod(HttpVerbs.GET)
  /** Uses POST method with the given body. */
  def post[A: HttpWrite](body: A): Request           = this.withMethod(HttpVerbs.POST).withBody(body)
  /** Uses POST method with the given body. */
  def post(body: String, charset: Charset): Request  = this.withMethod(HttpVerbs.POST).withBody(EncodedString(body, charset))
  /** Uses POST method with the given file. */
  def post(file: File): Request                      = this.withMethod(HttpVerbs.POST).withBody(FileBody(file))
```

### Request に認証を付ける

HTTP 認証を使う必要がある場合は、ユーザ名、パスワード、`AuthScheme` を用いて `Request` で指定することができる。
有効な `AuthScheme` の値は `Basic`、 `Digest`、  `NTLM`、  `Kerberos`、 `SPNEGO` となっている。

```console
scala> Gigahorse.url(url).get.withAuth("username", "password", AuthScheme.Basic)
```

`withAuth(...)` には `Realm` 値を受け取るオーバーロードもあって、それはより細かい設定をすることができる。

### Request にクエリパラメータを付ける

パラーメータはキーと値のタプルで設定できる。

```console
scala> Gigahorse.url(url).get.
         addQueryString(
           "q" -> "1 + 1",
           "format" -> "json"
         )
```

### Request にコンテンツタイプを付ける

テキストを POST する場合、`Content-Type` ヘッダを指定するべきだ。

```console
scala> import java.nio.charset.Charset
scala> Gigahorse.url(url).post("some text").
         withContentType(MimeTypes.TEXT, Gigahorse.utf8)
```

### Request にその他のヘッダを付ける

ヘッダはキーと値のタプルで設定できる。

```console
scala> Gigahorse.url(url).get.
         addHeaders(
           HeaderNames.AUTHORIZATION -> "bearer ****"
         )
```

### Request にバーチャルホストを付ける

バーチャルホストは文字列で設定できる。

```console
scala> Gigahorse.url(url).get.withVirtualHost("192.168.1.1")
```

### Request にタイムアウトを付ける

`Config` で指定された値を上書きしてリクエストタイムアウトを指定したい場合は、
`withRequestTimeout` を使って設定できる。無期限を指定する場合は `Duration.Inf` を渡す。

```console
scala> import scala.concurrent._, duration._
scala> Gigahorse.url(url).get.withRequestTimeout(5000.millis)
```

### フォームデータ

フォームエンコードされたデータを POST で送信する `Request` を作るには
`post` に `Map[String, List[String]]` を渡す。

```console
scala> val r = Gigahorse.url("http://www.freeformatter.com/json-validator.html").
         post(Map("inputString" -> List("{}")))
```

### ファイル

`post`、`put`、`patch` メソッドを使ってファイル送信のための `Request` 値を作ることができる。

```
scala> Gigahorse.url(url).post(new File("something.txt"))
```
