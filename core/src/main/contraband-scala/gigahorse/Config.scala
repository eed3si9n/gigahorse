/**
 * This code is generated using [[https://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package gigahorse
/**
 * @param connectTimeout The maximum time an `HttpClient` can wait when connecting to a remote host. (Default: 120s)
 * @param requestTimeout The maximum time an `HttpClient` waits until the response is completed. (Default: 120s)
 * @param readTimeout The maximum time an `HttpClient` can stay idle. (Default: 120s)
 * @param frameTimeout The maximum time an `HttpClient` waits until the stream is framed. (Default: 200ms)
 * @param followRedirects Is HTTP redirect enabled. (Default: `true`)
 * @param maxRedirects The maximum number of redirects. (Default: 5)
 * @param compressionEnforced Is HTTP compression enforced. (Default: `false`)
 * @param userAgentOpt The USER_AGENT header value
 * @param authOpt Set the authentication that will be used for all requests.
 * @param ssl The SSL configuration.
 * @param maxRequestRetry The maximum number of times to retry a request if it fails. (Default: 5)
 * @param disableUrlEncoding Whether raw URL should be used. (Default: false)
 * @param useProxyProperties Sets whether `HttpClient` should use the default http.proxy* system properties
                             to obtain proxy information. (Default: `true`)
                             
                             This differs from `useProxySelector(boolean)`
                             in that HttpClient will use its own logic to handle the system properties,
                             potentially supporting other protocols that the the JDK ProxySelector doesn't.
                             
                             If useProxyProperties is set to `true` but `useProxySelector`
                             was also set to true, the latter is preferred.
                             
                             See http://download.oracle.com/javase/1.4.2/docs/guide/net/properties.html
 * @param keepAlive Whether connection pooling should be used. (Default: `true`)
 * @param pooledConnectionIdleTimeout The time after which a connection that has been idle in the pool should be closed.
 * @param connectionTtl The maximum time that a connection should live for in the pool.
 * @param maxConnections The maximum total number of connections. -1 means no maximum.
 * @param maxConnectionsPerHost The maximum number of connections to make per host. -1 means no maximum.
 * @param maxFrameSize The maximum size of a stream fragment. (Default: 1M)
 * @param webSocketMaxFrameSize The maximum accepted size of a websocket message fragment. (Default: 1M)
 * @param cacheDirectory The directory to be used for caching.
 * @param maxCacheSize The maximum size of the cache.
 */
final class Config private (
  val connectTimeout: scala.concurrent.duration.FiniteDuration,
  val requestTimeout: scala.concurrent.duration.FiniteDuration,
  val readTimeout: scala.concurrent.duration.Duration,
  val frameTimeout: scala.concurrent.duration.FiniteDuration,
  val followRedirects: Boolean,
  val maxRedirects: Int,
  val compressionEnforced: Boolean,
  val userAgentOpt: Option[String],
  val authOpt: Option[Realm],
  val ssl: com.typesafe.sslconfig.ssl.SSLConfigSettings,
  val maxRequestRetry: Int,
  val disableUrlEncoding: Boolean,
  val useProxyProperties: Boolean,
  val keepAlive: Boolean,
  val pooledConnectionIdleTimeout: scala.concurrent.duration.Duration,
  val connectionTtl: scala.concurrent.duration.Duration,
  val maxConnections: Int,
  val maxConnectionsPerHost: Int,
  val maxFrameSize: gigahorse.ConfigMemorySize,
  val webSocketMaxFrameSize: gigahorse.ConfigMemorySize,
  val cacheDirectory: Option[java.io.File],
  val maxCacheSize: gigahorse.ConfigMemorySize) extends Serializable {
  def withUserAgent(userAgent: String): Config = copy(userAgentOpt = Some(userAgent))
  def withAuth(auth: Realm): Config = copy(authOpt = Some(auth))
  def withAuth(username: String, password: String): Config = copy(authOpt = Some(Realm(username = username, password = password)))
  def withAuth(username: String, password: String, scheme: AuthScheme): Config = copy(authOpt = Some(Realm(username = username, password = password, scheme = scheme)))
  private def this() = this(ConfigDefaults.defaultConnectTimeout, ConfigDefaults.defaultRequestTimeout, ConfigDefaults.defaultReadTimeout, ConfigDefaults.defaultFrameTimeout, ConfigDefaults.defaultFollowRedirects, ConfigDefaults.defaultMaxRedirects, ConfigDefaults.defaultCompressionEnforced, ConfigDefaults.defaultUserAgentOpt, ConfigDefaults.defaultAuthOpt, ConfigDefaults.defaultSslConfig, ConfigDefaults.defaultMaxRequestRetry, ConfigDefaults.defaultDisableUrlEncoding, ConfigDefaults.defaultUseProxyProperties, ConfigDefaults.defaultKeepAlive, ConfigDefaults.defaultPooledConnectionIdleTimeout, ConfigDefaults.defaultConnectionTtl, ConfigDefaults.defaultMaxConnections, ConfigDefaults.defaultMaxConnectionsPerHost, ConfigDefaults.defaultMaxFrameSize, ConfigDefaults.defaultWebSocketMaxFrameSize, None, ConfigDefaults.defaultMaxCacheSize)
  
  override def equals(o: Any): Boolean = o match {
    case x: Config => (this.connectTimeout == x.connectTimeout) && (this.requestTimeout == x.requestTimeout) && (this.readTimeout == x.readTimeout) && (this.frameTimeout == x.frameTimeout) && (this.followRedirects == x.followRedirects) && (this.maxRedirects == x.maxRedirects) && (this.compressionEnforced == x.compressionEnforced) && (this.userAgentOpt == x.userAgentOpt) && (this.authOpt == x.authOpt) && (this.ssl == x.ssl) && (this.maxRequestRetry == x.maxRequestRetry) && (this.disableUrlEncoding == x.disableUrlEncoding) && (this.useProxyProperties == x.useProxyProperties) && (this.keepAlive == x.keepAlive) && (this.pooledConnectionIdleTimeout == x.pooledConnectionIdleTimeout) && (this.connectionTtl == x.connectionTtl) && (this.maxConnections == x.maxConnections) && (this.maxConnectionsPerHost == x.maxConnectionsPerHost) && (this.maxFrameSize == x.maxFrameSize) && (this.webSocketMaxFrameSize == x.webSocketMaxFrameSize) && (this.cacheDirectory == x.cacheDirectory) && (this.maxCacheSize == x.maxCacheSize)
    case _ => false
  }
  override def hashCode: Int = {
    37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (17 + "gigahorse.Config".##) + connectTimeout.##) + requestTimeout.##) + readTimeout.##) + frameTimeout.##) + followRedirects.##) + maxRedirects.##) + compressionEnforced.##) + userAgentOpt.##) + authOpt.##) + ssl.##) + maxRequestRetry.##) + disableUrlEncoding.##) + useProxyProperties.##) + keepAlive.##) + pooledConnectionIdleTimeout.##) + connectionTtl.##) + maxConnections.##) + maxConnectionsPerHost.##) + maxFrameSize.##) + webSocketMaxFrameSize.##) + cacheDirectory.##) + maxCacheSize.##)
  }
  override def toString: String = {
    "Config(" + connectTimeout + ", " + requestTimeout + ", " + readTimeout + ", " + frameTimeout + ", " + followRedirects + ", " + maxRedirects + ", " + compressionEnforced + ", " + userAgentOpt + ", " + authOpt + ", " + ssl + ", " + maxRequestRetry + ", " + disableUrlEncoding + ", " + useProxyProperties + ", " + keepAlive + ", " + pooledConnectionIdleTimeout + ", " + connectionTtl + ", " + maxConnections + ", " + maxConnectionsPerHost + ", " + maxFrameSize + ", " + webSocketMaxFrameSize + ", " + cacheDirectory + ", " + maxCacheSize + ")"
  }
  private[this] def copy(connectTimeout: scala.concurrent.duration.FiniteDuration = connectTimeout, requestTimeout: scala.concurrent.duration.FiniteDuration = requestTimeout, readTimeout: scala.concurrent.duration.Duration = readTimeout, frameTimeout: scala.concurrent.duration.FiniteDuration = frameTimeout, followRedirects: Boolean = followRedirects, maxRedirects: Int = maxRedirects, compressionEnforced: Boolean = compressionEnforced, userAgentOpt: Option[String] = userAgentOpt, authOpt: Option[Realm] = authOpt, ssl: com.typesafe.sslconfig.ssl.SSLConfigSettings = ssl, maxRequestRetry: Int = maxRequestRetry, disableUrlEncoding: Boolean = disableUrlEncoding, useProxyProperties: Boolean = useProxyProperties, keepAlive: Boolean = keepAlive, pooledConnectionIdleTimeout: scala.concurrent.duration.Duration = pooledConnectionIdleTimeout, connectionTtl: scala.concurrent.duration.Duration = connectionTtl, maxConnections: Int = maxConnections, maxConnectionsPerHost: Int = maxConnectionsPerHost, maxFrameSize: gigahorse.ConfigMemorySize = maxFrameSize, webSocketMaxFrameSize: gigahorse.ConfigMemorySize = webSocketMaxFrameSize, cacheDirectory: Option[java.io.File] = cacheDirectory, maxCacheSize: gigahorse.ConfigMemorySize = maxCacheSize): Config = {
    new Config(connectTimeout, requestTimeout, readTimeout, frameTimeout, followRedirects, maxRedirects, compressionEnforced, userAgentOpt, authOpt, ssl, maxRequestRetry, disableUrlEncoding, useProxyProperties, keepAlive, pooledConnectionIdleTimeout, connectionTtl, maxConnections, maxConnectionsPerHost, maxFrameSize, webSocketMaxFrameSize, cacheDirectory, maxCacheSize)
  }
  def withConnectTimeout(connectTimeout: scala.concurrent.duration.FiniteDuration): Config = {
    copy(connectTimeout = connectTimeout)
  }
  def withRequestTimeout(requestTimeout: scala.concurrent.duration.FiniteDuration): Config = {
    copy(requestTimeout = requestTimeout)
  }
  def withReadTimeout(readTimeout: scala.concurrent.duration.Duration): Config = {
    copy(readTimeout = readTimeout)
  }
  def withFrameTimeout(frameTimeout: scala.concurrent.duration.FiniteDuration): Config = {
    copy(frameTimeout = frameTimeout)
  }
  def withFollowRedirects(followRedirects: Boolean): Config = {
    copy(followRedirects = followRedirects)
  }
  def withMaxRedirects(maxRedirects: Int): Config = {
    copy(maxRedirects = maxRedirects)
  }
  def withCompressionEnforced(compressionEnforced: Boolean): Config = {
    copy(compressionEnforced = compressionEnforced)
  }
  def withUserAgentOpt(userAgentOpt: Option[String]): Config = {
    copy(userAgentOpt = userAgentOpt)
  }
  def withUserAgentOpt(userAgentOpt: String): Config = {
    copy(userAgentOpt = Option(userAgentOpt))
  }
  def withAuthOpt(authOpt: Option[Realm]): Config = {
    copy(authOpt = authOpt)
  }
  def withAuthOpt(authOpt: Realm): Config = {
    copy(authOpt = Option(authOpt))
  }
  def withSsl(ssl: com.typesafe.sslconfig.ssl.SSLConfigSettings): Config = {
    copy(ssl = ssl)
  }
  def withMaxRequestRetry(maxRequestRetry: Int): Config = {
    copy(maxRequestRetry = maxRequestRetry)
  }
  def withDisableUrlEncoding(disableUrlEncoding: Boolean): Config = {
    copy(disableUrlEncoding = disableUrlEncoding)
  }
  def withUseProxyProperties(useProxyProperties: Boolean): Config = {
    copy(useProxyProperties = useProxyProperties)
  }
  def withKeepAlive(keepAlive: Boolean): Config = {
    copy(keepAlive = keepAlive)
  }
  def withPooledConnectionIdleTimeout(pooledConnectionIdleTimeout: scala.concurrent.duration.Duration): Config = {
    copy(pooledConnectionIdleTimeout = pooledConnectionIdleTimeout)
  }
  def withConnectionTtl(connectionTtl: scala.concurrent.duration.Duration): Config = {
    copy(connectionTtl = connectionTtl)
  }
  def withMaxConnections(maxConnections: Int): Config = {
    copy(maxConnections = maxConnections)
  }
  def withMaxConnectionsPerHost(maxConnectionsPerHost: Int): Config = {
    copy(maxConnectionsPerHost = maxConnectionsPerHost)
  }
  def withMaxFrameSize(maxFrameSize: gigahorse.ConfigMemorySize): Config = {
    copy(maxFrameSize = maxFrameSize)
  }
  def withWebSocketMaxFrameSize(webSocketMaxFrameSize: gigahorse.ConfigMemorySize): Config = {
    copy(webSocketMaxFrameSize = webSocketMaxFrameSize)
  }
  def withCacheDirectory(cacheDirectory: Option[java.io.File]): Config = {
    copy(cacheDirectory = cacheDirectory)
  }
  def withCacheDirectory(cacheDirectory: java.io.File): Config = {
    copy(cacheDirectory = Option(cacheDirectory))
  }
  def withMaxCacheSize(maxCacheSize: gigahorse.ConfigMemorySize): Config = {
    copy(maxCacheSize = maxCacheSize)
  }
}
object Config {
  
  def apply(): Config = new Config()
  def apply(connectTimeout: scala.concurrent.duration.FiniteDuration, requestTimeout: scala.concurrent.duration.FiniteDuration, readTimeout: scala.concurrent.duration.Duration, frameTimeout: scala.concurrent.duration.FiniteDuration, followRedirects: Boolean, maxRedirects: Int, compressionEnforced: Boolean, userAgentOpt: Option[String], authOpt: Option[Realm], ssl: com.typesafe.sslconfig.ssl.SSLConfigSettings, maxRequestRetry: Int, disableUrlEncoding: Boolean, useProxyProperties: Boolean, keepAlive: Boolean, pooledConnectionIdleTimeout: scala.concurrent.duration.Duration, connectionTtl: scala.concurrent.duration.Duration, maxConnections: Int, maxConnectionsPerHost: Int, maxFrameSize: gigahorse.ConfigMemorySize, webSocketMaxFrameSize: gigahorse.ConfigMemorySize, cacheDirectory: Option[java.io.File], maxCacheSize: gigahorse.ConfigMemorySize): Config = new Config(connectTimeout, requestTimeout, readTimeout, frameTimeout, followRedirects, maxRedirects, compressionEnforced, userAgentOpt, authOpt, ssl, maxRequestRetry, disableUrlEncoding, useProxyProperties, keepAlive, pooledConnectionIdleTimeout, connectionTtl, maxConnections, maxConnectionsPerHost, maxFrameSize, webSocketMaxFrameSize, cacheDirectory, maxCacheSize)
  def apply(connectTimeout: scala.concurrent.duration.FiniteDuration, requestTimeout: scala.concurrent.duration.FiniteDuration, readTimeout: scala.concurrent.duration.Duration, frameTimeout: scala.concurrent.duration.FiniteDuration, followRedirects: Boolean, maxRedirects: Int, compressionEnforced: Boolean, userAgentOpt: String, authOpt: Realm, ssl: com.typesafe.sslconfig.ssl.SSLConfigSettings, maxRequestRetry: Int, disableUrlEncoding: Boolean, useProxyProperties: Boolean, keepAlive: Boolean, pooledConnectionIdleTimeout: scala.concurrent.duration.Duration, connectionTtl: scala.concurrent.duration.Duration, maxConnections: Int, maxConnectionsPerHost: Int, maxFrameSize: gigahorse.ConfigMemorySize, webSocketMaxFrameSize: gigahorse.ConfigMemorySize, cacheDirectory: java.io.File, maxCacheSize: gigahorse.ConfigMemorySize): Config = new Config(connectTimeout, requestTimeout, readTimeout, frameTimeout, followRedirects, maxRedirects, compressionEnforced, Option(userAgentOpt), Option(authOpt), ssl, maxRequestRetry, disableUrlEncoding, useProxyProperties, keepAlive, pooledConnectionIdleTimeout, connectionTtl, maxConnections, maxConnectionsPerHost, maxFrameSize, webSocketMaxFrameSize, Option(cacheDirectory), maxCacheSize)
}
