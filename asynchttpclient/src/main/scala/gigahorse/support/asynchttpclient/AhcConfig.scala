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
package support.asynchttpclient

import shaded.ahc.org.asynchttpclient._
import scala.concurrent.duration._
import com.typesafe.sslconfig.ssl._

import shaded.ahc.io.netty.handler.ssl.SslContextBuilder
import shaded.ahc.io.netty.handler.ssl.util.InsecureTrustManagerFactory
import shaded.ahc.org.asynchttpclient.netty.ssl.JsseSslEngineFactory

object AhcConfig {
  import AhcHttpClient.buildRealm

  /** Build `AsyncHttpClientConfig` */
  def buildConfig(config: Config): AsyncHttpClientConfig =
    {
      val builder = new DefaultAsyncHttpClientConfig.Builder()
      // timeouts
      builder.setConnectTimeout(toMillis(config.connectTimeout))
      builder.setRequestTimeout(toMillis(config.requestTimeout))
      builder.setReadTimeout(toMillis(config.readTimeout))
      // builder.setWebSocketTimeout(toMillis(config.webSocketIdleTimeout))

      // http
      builder.setFollowRedirect(config.followRedirects)
      builder.setMaxRedirects(config.maxRedirects)
      builder.setCompressionEnforced(config.compressionEnforced)
      config.userAgentOpt foreach { builder.setUserAgent }
      config.authOpt foreach { x => builder.setRealm(buildRealm(x)) }
      builder.setMaxRequestRetry(config.maxRequestRetry)
      builder.setDisableUrlEncodingForBoundRequests(config.disableUrlEncoding)
      builder.setUseProxyProperties(config.useProxyProperties)

      // keep-alive
      builder.setKeepAlive(config.keepAlive)
      builder.setPooledConnectionIdleTimeout(toMillis(config.pooledConnectionIdleTimeout))
      builder.setConnectionTtl(toMillis(config.connectionTtl))
      builder.setMaxConnectionsPerHost(config.maxConnectionsPerHost)
      builder.setMaxConnections(config.maxConnections)
      configureSsl(config.ssl, builder)

      // websocket

      builder.setWebSocketMaxFrameSize(config.webSocketMaxFrameSize.bytes.toInt)

      builder.build()
    }

  def toMillis(duration: Duration): Int =
    if (duration.isFinite) duration.toMillis.toInt
    else -1

  def configureSsl(sslConfig: SSLConfigSettings, builder: DefaultAsyncHttpClientConfig.Builder): Unit =
    {
      // context!
      val (sslContext, _) = SSL.buildContext(sslConfig)

      // protocols!
      val defaultParams = sslContext.getDefaultSSLParameters
      val defaultProtocols = defaultParams.getProtocols
      val protocols = configureProtocols(defaultProtocols, sslConfig)
      defaultParams.setProtocols(protocols)
      builder.setEnabledProtocols(protocols)

      // ciphers!
      val defaultCiphers = defaultParams.getCipherSuites
      builder.setEnabledCipherSuites(defaultCiphers)

      builder.setAcceptAnyCertificate(sslConfig.loose.acceptAnyCertificate)

      // If you wan't to accept any certificate you also want to use a loose netty based loose SslContext
      // Never use this in production.
      if (sslConfig.loose.acceptAnyCertificate) {
        builder.setSslContext(SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build())
      } else {
        builder.setSslEngineFactory(new JsseSslEngineFactory(sslContext))
      }
    }

  def configureProtocols(existingProtocols: Array[String], sslConfig: SSLConfigSettings): Array[String] =
    {
      val definedProtocols = sslConfig.enabledProtocols match {
        case Some(configuredProtocols) =>
          // If we are given a specific list of protocols, then return it in exactly that order,
          // assuming that it's actually possible in the SSL context.
          configuredProtocols.filter(existingProtocols.contains).toArray

        case None =>
          // Otherwise, we return the default protocols in the given list.
          Protocols.recommendedProtocols.filter(existingProtocols.contains).toArray
      }

      definedProtocols
    }
}
