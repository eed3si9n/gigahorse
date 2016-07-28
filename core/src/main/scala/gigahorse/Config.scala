/**
 * This code is generated using sbt-datatype.
 */

// DO NOT EDIT MANUALLY
package gigahorse
final class Config(
  /** The maximum time an `HttpClient` can wait when connecting to a remote host. (Default: 120s) */
  val connectTimeout: scala.concurrent.duration.Duration,
  /** The maximum time an `HttpClient` waits until the response is completed. (Default: 120s) */
  val requestTimeout: scala.concurrent.duration.Duration,
  /** The maximum time an `HttpClient` can stay idle. (Default: 120s) */
  val readTimeout: scala.concurrent.duration.Duration,
  /** Is HTTP redirect enabled. (Default: `true`) */
  val followRedirects: Boolean,
  /** The maximum number of redirects. (Default: 5) */
  val maxRedirects: Int,
  /** Is HTTP compression enforced. (Default: `false`) */
  val compressionEnforced: Boolean,
  /** The USER_AGENT header value */
  val userAgentOpt: Option[String],
  /** Set the authentication that will be used for all requests. */
  val authOpt: Option[Realm],
  /** The SSL configuration. */
  val ssl: com.typesafe.sslconfig.ssl.SSLConfig,
  /** The maximum number of times to retry a request if it fails. (Default: 5) */
  val maxRequestRetry: Int,
  /** Whether raw URL should be used. (Default: false) */
  val disableUrlEncoding: Boolean,
  /**
   * Sets whether `HttpClient` should use the default http.proxy* system properties
   * to obtain proxy information. (Default: `true`)
   * 
   * This differs from `useProxySelector(boolean)`
   * in that HttpClient will use its own logic to handle the system properties,
   * potentially supporting other protocols that the the JDK ProxySelector doesn't.
   * 
   * If useProxyProperties is set to `true` but `useProxySelector`
   * was also set to true, the latter is preferred.
   * 
   * See http://download.oracle.com/javase/1.4.2/docs/guide/net/properties.html
   */
  val useProxyProperties: Boolean,
  /** Whether connection pooling should be used. (Default: `true`) */
  val keepAlive: Boolean,
  /** The time after which a connection that has been idle in the pool should be closed. */
  val pooledConnectionIdleTimeout: scala.concurrent.duration.Duration,
  /** The maximum time that a connection should live for in the pool. */
  val connectionTtl: scala.concurrent.duration.Duration,
  /** The maximum total number of connections. -1 means no maximum. */
  val maxConnections: Int,
  /** The maximum number of connections to make per host. -1 means no maximum. */
  val maxConnectionsPerHost: Int) extends Serializable {
  def withUserAgent(userAgent: String): Config = copy(userAgentOpt = Some(userAgent))
  def withAuth(auth: Realm): Config = copy(authOpt = Some(auth))
  def withAuth(username: String, password: String): Config = copy(authOpt = Some(Realm(username = username, password = password)))
  def withAuth(username: String, password: String, scheme: AuthScheme): Config = copy(authOpt = Some(Realm(username = username, password = password, scheme = scheme)))
  def this() = this(ConfigDefaults.defaultConnectTimeout, ConfigDefaults.defaultRequestTimeout, ConfigDefaults.defaultReadTimeout, ConfigDefaults.defaultFollowRedirects, ConfigDefaults.defaultMaxRedirects, ConfigDefaults.defaultCompressionEnforced, ConfigDefaults.defaultUserAgentOpt, ConfigDefaults.defaultAuthOpt, ConfigDefaults.defaultSslConfig, ConfigDefaults.defaultMaxRequestRetry, ConfigDefaults.defaultDisableUrlEncoding, ConfigDefaults.defaultUseProxyProperties, ConfigDefaults.defaultKeepAlive, ConfigDefaults.defaultPooledConnectionIdleTimeout, ConfigDefaults.defaultConnectionTtl, ConfigDefaults.defaultMaxConnections, ConfigDefaults.defaultMaxConnectionsPerHost)
  
  override def equals(o: Any): Boolean = o match {
    case x: Config => (this.connectTimeout == x.connectTimeout) && (this.requestTimeout == x.requestTimeout) && (this.readTimeout == x.readTimeout) && (this.followRedirects == x.followRedirects) && (this.maxRedirects == x.maxRedirects) && (this.compressionEnforced == x.compressionEnforced) && (this.userAgentOpt == x.userAgentOpt) && (this.authOpt == x.authOpt) && (this.ssl == x.ssl) && (this.maxRequestRetry == x.maxRequestRetry) && (this.disableUrlEncoding == x.disableUrlEncoding) && (this.useProxyProperties == x.useProxyProperties) && (this.keepAlive == x.keepAlive) && (this.pooledConnectionIdleTimeout == x.pooledConnectionIdleTimeout) && (this.connectionTtl == x.connectionTtl) && (this.maxConnections == x.maxConnections) && (this.maxConnectionsPerHost == x.maxConnectionsPerHost)
    case _ => false
  }
  override def hashCode: Int = {
    37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (17 + connectTimeout.##) + requestTimeout.##) + readTimeout.##) + followRedirects.##) + maxRedirects.##) + compressionEnforced.##) + userAgentOpt.##) + authOpt.##) + ssl.##) + maxRequestRetry.##) + disableUrlEncoding.##) + useProxyProperties.##) + keepAlive.##) + pooledConnectionIdleTimeout.##) + connectionTtl.##) + maxConnections.##) + maxConnectionsPerHost.##)
  }
  override def toString: String = {
    "Config(" + connectTimeout + ", " + requestTimeout + ", " + readTimeout + ", " + followRedirects + ", " + maxRedirects + ", " + compressionEnforced + ", " + userAgentOpt + ", " + authOpt + ", " + ssl + ", " + maxRequestRetry + ", " + disableUrlEncoding + ", " + useProxyProperties + ", " + keepAlive + ", " + pooledConnectionIdleTimeout + ", " + connectionTtl + ", " + maxConnections + ", " + maxConnectionsPerHost + ")"
  }
  private[this] def copy(connectTimeout: scala.concurrent.duration.Duration = connectTimeout, requestTimeout: scala.concurrent.duration.Duration = requestTimeout, readTimeout: scala.concurrent.duration.Duration = readTimeout, followRedirects: Boolean = followRedirects, maxRedirects: Int = maxRedirects, compressionEnforced: Boolean = compressionEnforced, userAgentOpt: Option[String] = userAgentOpt, authOpt: Option[Realm] = authOpt, ssl: com.typesafe.sslconfig.ssl.SSLConfig = ssl, maxRequestRetry: Int = maxRequestRetry, disableUrlEncoding: Boolean = disableUrlEncoding, useProxyProperties: Boolean = useProxyProperties, keepAlive: Boolean = keepAlive, pooledConnectionIdleTimeout: scala.concurrent.duration.Duration = pooledConnectionIdleTimeout, connectionTtl: scala.concurrent.duration.Duration = connectionTtl, maxConnections: Int = maxConnections, maxConnectionsPerHost: Int = maxConnectionsPerHost): Config = {
    new Config(connectTimeout, requestTimeout, readTimeout, followRedirects, maxRedirects, compressionEnforced, userAgentOpt, authOpt, ssl, maxRequestRetry, disableUrlEncoding, useProxyProperties, keepAlive, pooledConnectionIdleTimeout, connectionTtl, maxConnections, maxConnectionsPerHost)
  }
  def withConnectTimeout(connectTimeout: scala.concurrent.duration.Duration): Config = {
    copy(connectTimeout = connectTimeout)
  }
  def withRequestTimeout(requestTimeout: scala.concurrent.duration.Duration): Config = {
    copy(requestTimeout = requestTimeout)
  }
  def withReadTimeout(readTimeout: scala.concurrent.duration.Duration): Config = {
    copy(readTimeout = readTimeout)
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
  def withAuthOpt(authOpt: Option[Realm]): Config = {
    copy(authOpt = authOpt)
  }
  def withSsl(ssl: com.typesafe.sslconfig.ssl.SSLConfig): Config = {
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
}
object Config {
  def apply(): Config = new Config(ConfigDefaults.defaultConnectTimeout, ConfigDefaults.defaultRequestTimeout, ConfigDefaults.defaultReadTimeout, ConfigDefaults.defaultFollowRedirects, ConfigDefaults.defaultMaxRedirects, ConfigDefaults.defaultCompressionEnforced, ConfigDefaults.defaultUserAgentOpt, ConfigDefaults.defaultAuthOpt, ConfigDefaults.defaultSslConfig, ConfigDefaults.defaultMaxRequestRetry, ConfigDefaults.defaultDisableUrlEncoding, ConfigDefaults.defaultUseProxyProperties, ConfigDefaults.defaultKeepAlive, ConfigDefaults.defaultPooledConnectionIdleTimeout, ConfigDefaults.defaultConnectionTtl, ConfigDefaults.defaultMaxConnections, ConfigDefaults.defaultMaxConnectionsPerHost)
  def apply(connectTimeout: scala.concurrent.duration.Duration, requestTimeout: scala.concurrent.duration.Duration, readTimeout: scala.concurrent.duration.Duration, followRedirects: Boolean, maxRedirects: Int, compressionEnforced: Boolean, userAgentOpt: Option[String], authOpt: Option[Realm], ssl: com.typesafe.sslconfig.ssl.SSLConfig, maxRequestRetry: Int, disableUrlEncoding: Boolean, useProxyProperties: Boolean, keepAlive: Boolean, pooledConnectionIdleTimeout: scala.concurrent.duration.Duration, connectionTtl: scala.concurrent.duration.Duration, maxConnections: Int, maxConnectionsPerHost: Int): Config = new Config(connectTimeout, requestTimeout, readTimeout, followRedirects, maxRedirects, compressionEnforced, userAgentOpt, authOpt, ssl, maxRequestRetry, disableUrlEncoding, useProxyProperties, keepAlive, pooledConnectionIdleTimeout, connectionTtl, maxConnections, maxConnectionsPerHost)
}
