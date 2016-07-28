---
out: configuration.html
---

  [WsSSL]: https://www.playframework.com/documentation/2.4.x/WsSSL
  [request]: request.html

Configuring Gigahorse
---------------------

`Gigahorse.config` will read from `application.conf` to configure
the settings.

* `gigahorse.followRedirects`: Configures the client to follow 301 and 302 redirects *(default is **true**)*.
* `gigahorse.useProxyProperties`: To use the JVM system's HTTP proxy settings (http.proxyHost, http.proxyPort) *(default is **true**)*.
* `gigahorse.userAgent`: To configure the User-Agent header field.
* `gigahorse.compressionEnforced`: Set it to true to use gzip/deflater encoding *(default is **false**)*.

### Configuring Gigahorse with SSL

To configure Gigahorse for use with HTTP over SSL/TLS (HTTPS), see Play WS's [Configuring WS SSL][WsSSL]. Just place the configuration under `gigahorse.ssl`:

```
gigahorse.ssl {
  trustManager = {
    stores = [
      { type = "JKS", path = "exampletrust.jks" }
    ]
  }
}
```

### Configuring Timeouts

There are 3 different timeouts in Gigahorse. Reaching a timeout causes the request to interrupt.

* `gigahorse.connectTimeout`: The maximum time to wait when connecting to the remote host *(default is **120 seconds**)*.
* `gigahorse.requestTimeout`: The total time you accept a request to take (it will be interrupted even if the remote host is still sending data) *(default is **120 seconds**)*.
* `gigahorse.readTimeout`: The maximum time the request can stay idle (connection is established but waiting for more data) *(default is **120 seconds**)*.

The request timeout can be overridden for a specific connection with `withRequestTimeout()` (see [Building a Request value][request]).

### Advanced configuration

The following advanced settings can be configured.

Refer to the [AsyncHttpClientConfig Documentation](http://static.javadoc.io/org.asynchttpclient/async-http-client/2.0.0/org/asynchttpclient/DefaultAsyncHttpClientConfig.Builder.html) for more information.

* `gigahorse.maxRedirects`: The maximum number of redirects *(default: **5**)*.
* `gigahorse.maxRequestRetry`: The maximum number of times to retry a request if it fails *(default: **5**)*.
* `gigahorse.disableUrlEncoding`: Whether raw URL should be used *(default: **false**)*.
* `gigahorse.keepAlive`: Whether connection pooling should be used *(default: **true**)*.
* `gigahorse.pooledConnectionIdleTimeout`: The time after which a connection that has been idle in the pool should be closed.
* `gigahorse.connectionTtl`: The maximum time that a connection should live for in the pool.
* `gigahorse.maxConnections`: The maximum total number of connections. -1 means no maximum.
* `gigahorse.maxConnectionsPerHost`: The maximum number of connections to make per host. -1 means no maximum.
