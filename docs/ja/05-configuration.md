---
out: configuration.html
---

  [WsSSL]: https://www.playframework.com/documentation/ja/2.4.x/WsSSL
  [request]: request.html

Gigahorse の設定
---------------

`Gigahorse.config` は `application.conf` があればそこから設定を読み込む。

* `gigahorse.followRedirects`: 301、および、302 でのリダイレクトにクライアントが従うかを設定する。 *(デフォルトは **true**)*.
* `gigahorse.useProxyProperties`: JVM システムの HTTP プロキシ設定 (http.proxyHost, http.proxyPort) を使用するか設定する。 *(デフォルトは **true**)*.
* `gigahorse.userAgent`: User-Agent ヘッダーフィールドを設定する。
* `gigahorse.compressionEnforced`: このプロパティが true の場合 gzip/deflater によるエンコーディングを行う。 *(デフォルトは **false**)*.

### SSL 関連の設定

HTTP over SSL/TLS (HTTPS) に関する Gigahorse の設定については、 Play WS の [WS SSLの設定]を参照してほしい。
設定値は `gigahorse.ssl` 内で行う:

```
gigahorse.ssl {
  trustManager = {
    stores = [
      { type = "JKS", path = "exampletrust.jks" }
    ]
  }
}
```

### タイムアウトの設定

Gigahorse には 3つの異なるタイムアウトがある。タイムアウトになると、リクエストは中断される。

* `gigahorse.connectTimeout`: リモートホストとの接続を行う最大の時間 *(デフォルトは **120 秒**)*.
* `gigahorse.requestTimeout`: リクエストにかかる全体の時間 (リモートホストがデータを送信中であっても、中断する可能性がある) *(デフォルトは **120 秒**)*.
* `gigahorse.readTimeout`: アイドル状態 (コネクションは確立したが、データを待っている状態) を保持する最大の時間 *(デフォルトは **120 秒**)*.

個々のリクエストのタイムアウトは `withRequestTimeout()` を使用することで上書き可能だ。 ([Request 値の構築][request]を参照。)

### その他の設定

他に以下のような設定がある。

詳しくは [AsyncHttpClientConfig のドキュメント](http://static.javadoc.io/org.asynchttpclient/async-http-client/2.0.0/org/asynchttpclient/DefaultAsyncHttpClientConfig.Builder.html)を参照してほしい。

* `gigahorse.maxRedirects`: リダイレクトの最大数 *(デフォルトは **5**)*.
* `gigahorse.maxRequestRetry`: 失敗時の再試行の最大数 *(デフォルトは **5**)*.
* `gigahorse.disableUrlEncoding`: URL エンコーディングせずに生の URL を使うべきかどうか *(デフォルトは**false**)*.
* `gigahorse.keepAlive`: コネクションプーリングを行うかどうか *(デフォルトは **true**)*.
* `gigahorse.pooledConnectionIdleTimeout`: プール内でアイドル状態が続いた時に接続が閉じられる時間。
* `gigahorse.connectionTtl`: プール内で接続が生き続ける最大の時間。
* `gigahorse.maxConnections`: 最大接続数。無限の場合は -1。
* `gigahorse.maxConnectionsPerHost`: ホストあたりの最大接続数。無限の場合は -1。
