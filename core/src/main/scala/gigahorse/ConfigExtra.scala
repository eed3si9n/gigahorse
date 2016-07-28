/*
 * Original implementation (C) 2009-2016 Lightbend Inc. (https://www.lightbend.com).
 * Adapted and extended in 2016 by Eugene Yokota
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gigahorse

import java.nio.charset.Charset
import java.net.URI
import com.typesafe.config.{ Config => XConfig }
import scala.concurrent.duration.Duration
import com.typesafe.sslconfig.ssl.{ SSLConfigSettings, SSLConfigFactory }

object ConfigParser {
  import ConfigDefaults._
  implicit def toRichXConfig(config: XConfig): RichXConfig = new RichXConfig(config)

  val rootPath: String = "gigahorse"

  def parse(config0: XConfig): Config =
    {
      val config = config0.getConfig(rootPath)
      val sslConfig =
        if (config.hasPath("ssl")) SSLConfigFactory.parse(config.getConfig("ssl"))
        else SSLConfigFactory.defaultConfig
      val authOpt =
        if (config.hasPath("auth")) Some(parseRealm(config.getConfig("auth")))
        else defaultAuthOpt
      Config(
        connectTimeout        = config.getDuration("connectTimeout", defaultConnectTimeout),
        requestTimeout        = config.getDuration("requestTimeout", defaultRequestTimeout),
        readTimeout           = config.getDuration("readTimeout", defaultReadTimeout),
        followRedirects       = config.getBoolean("followRedirects", defaultFollowRedirects),
        maxRedirects          = config.getInt("maxRedirects", defaultMaxRedirects),
        compressionEnforced   = config.getBoolean("compressionEnforced", defaultCompressionEnforced),
        userAgentOpt          = config.getStringOption("userAgent", defaultUserAgentOpt),
        authOpt               = authOpt,
        ssl                   = sslConfig,
        maxRequestRetry       = config.getInt("maxRequestRetry", defaultMaxRequestRetry),
        disableUrlEncoding    = config.getBoolean("disableUrlEncoding", defaultDisableUrlEncoding),
        useProxyProperties    = config.getBoolean("useProxyProperties", defaultUseProxyProperties),
        keepAlive             = config.getBoolean("keepAlive", defaultKeepAlive),
        pooledConnectionIdleTimeout = config.getDuration("pooledConnectionIdleTimeout", defaultPooledConnectionIdleTimeout),
        connectionTtl         = config.getDuration("connectionTtl", defaultConnectionTtl),
        maxConnections        = config.getInt("maxConnections", defaultMaxConnections),
        maxConnectionsPerHost = config.getInt("maxConnectionsPerHost", defaultMaxConnectionsPerHost)
      )
    }

  def parseRealm(config: XConfig): Realm =
    Realm(
      username          = config.getString("username"),
      password          = config.getString("password"),
      scheme            = parseScheme(config.getString("scheme", "Basic")),
      usePreemptiveAuth = config.getBoolean("usePreemptiveAuth", true),
      realmNameOpt      = config.getStringOption("realmName", None),
      nonceOpt          = config.getStringOption("nonce", None),
      algorithmOpt      = config.getStringOption("algorithm", None),
      responseOpt       = config.getStringOption("response", None),
      opaqueOpt         = config.getStringOption("opaque", None),
      qopOpt            = config.getStringOption("qop", None),
      ncOpt             = config.getStringOption("nc", None),
      uriOpt            = config.getStringOption("uri", None) map { new URI(_) },
      methodNameOpt     = config.getStringOption("methodName", None),
      charsetOpt        = config.getStringOption("charset", None) map { Charset.forName },
      ntlmDomainOpt     = config.getStringOption("ntlmDomain", None),
      ntlmHostOpt       = config.getStringOption("ntlmHost", None),
      useAbsoluteURI    = config.getBoolean("useAbsoluteURI", false),
      omitQuery         = config.getBoolean("omitQuery", false)
    )

  def parseScheme(s: String): AuthScheme =
    s.toLowerCase match {
      case "none"     => AuthScheme.None
      case "basic"    => AuthScheme.Basic
      case "digest"   => AuthScheme.Digest
      case "ntlm"     => AuthScheme.NTLM
      case "spnego"   => AuthScheme.SPNEGO
      case "kerberos" => AuthScheme.Kerberos
      case _          => sys.error("Invalid scheme: " + s)
    }
}

object ConfigDefaults {
  val defaultConnectTimeout        = Duration("120s")
  val defaultRequestTimeout        = Duration("120s")
  val defaultReadTimeout           = Duration("120s")
  val defaultFollowRedirects       = true
  val defaultMaxRedirects          = 5
  val defaultCompressionEnforced   = false
  val defaultUserAgentOpt          = None
  val defaultAuthOpt               = None
  val defaultSslConfig             = SSLConfigFactory.defaultConfig
  val defaultMaxRequestRetry       = 5
  val defaultDisableUrlEncoding    = false
  val defaultUseProxyProperties    = true
  val defaultKeepAlive             = true
  val defaultPooledConnectionIdleTimeout = Duration("60s")
  val defaultConnectionTtl         = Duration.Inf
  val defaultMaxConnections        = -1
  val defaultMaxConnectionsPerHost = -1
}

class RichXConfig(config: XConfig) {
  def getStringOption(path: String, fallback: Option[String]): Option[String] =
    if (config.hasPath(path)) Some(config.getString(path))
    else fallback
  def getString(path: String, fallback: String): String =
    if (config.hasPath(path)) config.getString(path)
    else fallback
  def getInt(path: String, fallback: Int): Int =
    if (config.hasPath(path)) config.getInt(path)
    else fallback
  def getBoolean(path: String, fallback: Boolean): Boolean =
    if (config.hasPath(path)) config.getBoolean(path)
    else fallback
  def getDuration(path: String, fallback: Duration): Duration =
    if (config.hasPath(path)) Duration(config.getString(path))
    else fallback
}
