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

import com.ning.http.client._
import scala.concurrent.duration._
import com.typesafe.sslconfig.ssl._
import com.typesafe.sslconfig.util.NoopLogger
import org.slf4j.LoggerFactory
import javax.net.ssl._
import java.security.KeyStore
import java.security.cert.CertPathValidatorException

object AhcConfig {
  import AhcHttpClient.buildRealm

  /** Build `AsyncHttpClientConfig` */
  def buildConfig(config: Config): AsyncHttpClientConfig =
    {
      val builder = new AsyncHttpClientConfig.Builder()
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
      builder.setDisableUrlEncodingForBoundedRequests(config.disableUrlEncoding)
      builder.setUseProxyProperties(config.useProxyProperties)

      // keep-alive
      // Using config.keepAlive, which is the new name in AHC 2.0
      builder.setAllowPoolingConnections(config.keepAlive)
      builder.setAllowPoolingSslConnections(config.keepAlive)
      builder.setPooledConnectionIdleTimeout(toMillis(config.pooledConnectionIdleTimeout))
      builder.setConnectionTTL(toMillis(config.connectionTtl))
      builder.setMaxConnectionsPerHost(config.maxConnectionsPerHost)
      builder.setMaxConnections(config.maxConnections)
      configureSsl(config.ssl, builder)

      builder.build()
    }

  def toMillis(duration: Duration): Int =
    if (duration.isFinite()) duration.toMillis.toInt
    else -1

  // https://github.com/playframework/playframework/blob/2.4.x/framework/src/play-ws/src/main/scala/play/api/libs/ws/ning/NingConfig.scala
  def configureSsl(sslConfig: SSLConfig, builder: AsyncHttpClientConfig.Builder): Unit =
    {
      // context!
      val sslContext = if (sslConfig.default) {
        // logger.info("buildSSLContext: ws.ssl.default is true, using default SSLContext")
        validateDefaultTrustManager(sslConfig)
        SSLContext.getDefault
      } else {
        // break out the static methods as much as we can...
        val keyManagerFactory = buildKeyManagerFactory(sslConfig)
        val trustManagerFactory = buildTrustManagerFactory(sslConfig)
        new ConfigSSLContextBuilder(NoopLogger.factory(), sslConfig, keyManagerFactory, trustManagerFactory).build()
      }

      // protocols!
      val defaultParams = sslContext.getDefaultSSLParameters
      val defaultProtocols = defaultParams.getProtocols
      val protocols = configureProtocols(defaultProtocols, sslConfig)
      defaultParams.setProtocols(protocols)
      builder.setEnabledProtocols(protocols)

      // ciphers!
      val defaultCiphers = defaultParams.getCipherSuites
      val cipherSuites = configureCipherSuites(defaultCiphers, sslConfig)
      defaultParams.setCipherSuites(cipherSuites)
      builder.setEnabledCipherSuites(cipherSuites)

      builder.setAcceptAnyCertificate(sslConfig.loose.acceptAnyCertificate)

      // Hostname Processing
      if (!sslConfig.loose.disableHostnameVerification) {
        val hostnameVerifier = buildHostnameVerifier(sslConfig)
        builder.setHostnameVerifier(hostnameVerifier)
      } else {
        // logger.warn("buildHostnameVerifier: disabling hostname verification")
        val disabledHostnameVerifier = new DisabledComplainingHostnameVerifier(NoopLogger.factory())
        builder.setHostnameVerifier(disabledHostnameVerifier)
      }

      builder.setSSLContext(sslContext)
    }

  def validateDefaultTrustManager(sslConfig: SSLConfig): Unit =
    {
      // If we are using a default SSL context, we can't filter out certificates with weak algorithms
      // We ALSO don't have access to the trust manager from the SSLContext without doing horrible things
      // with reflection.
      //
      // However, given that the default SSLContextImpl will call out to the TrustManagerFactory and any
      // configuration with system properties will also apply with the factory, we can use the factory
      // method to recreate the trust manager and validate the trust certificates that way.
      //
      // This is really a last ditch attempt to satisfy https://wiki.mozilla.org/CA:MD5and1024 on root certificates.
      //
      // http://grepcode.com/file/repository.grepcode.com/java/root/jdk/openjdk/7-b147/sun/security/ssl/SSLContextImpl.java#79

      val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
      tmf.init(null.asInstanceOf[KeyStore])
      val trustManager: X509TrustManager = tmf.getTrustManagers()(0).asInstanceOf[X509TrustManager]

      val constraints = sslConfig.disabledKeyAlgorithms.map(a => AlgorithmConstraintsParser.parseAll(AlgorithmConstraintsParser.expression, a).get).toSet
      val algorithmChecker = new AlgorithmChecker(NoopLogger.factory(), keyConstraints = constraints, signatureConstraints = Set())
      for (cert <- trustManager.getAcceptedIssuers) {
        try {
          algorithmChecker.checkKeyAlgorithms(cert)
        } catch {
          case e: CertPathValidatorException =>
            // logger.warn("You are using ws.ssl.default=true and have a weak certificate in your default trust store!  (You can modify ws.ssl.disabledKeyAlgorithms to remove this message.)", e)
        }
      }
    }

  def buildKeyManagerFactory(ssl: SSLConfig): KeyManagerFactoryWrapper =
    new DefaultKeyManagerFactoryWrapper(ssl.keyManagerConfig.algorithm)

  def buildTrustManagerFactory(ssl: SSLConfig): TrustManagerFactoryWrapper =
    new DefaultTrustManagerFactoryWrapper(ssl.trustManagerConfig.algorithm)

  def configureProtocols(existingProtocols: Array[String], sslConfig: SSLConfig): Array[String] =
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

      if (!sslConfig.loose.allowWeakProtocols) {
        val deprecatedProtocols = Protocols.deprecatedProtocols
        for (deprecatedProtocol <- deprecatedProtocols) {
          if (definedProtocols.contains(deprecatedProtocol)) {
            throw new IllegalStateException(s"Weak protocol $deprecatedProtocol found in ws.ssl.protocols!")
          }
        }
      }
      definedProtocols
    }

  def configureCipherSuites(existingCiphers: Array[String], sslConfig: SSLConfig): Array[String] =
    {
      val definedCiphers = sslConfig.enabledCipherSuites match {
        case Some(configuredCiphers) =>
          // If we are given a specific list of ciphers, return it in that order.
          configuredCiphers.filter(existingCiphers.contains(_)).toArray

        case None =>
          Ciphers.recommendedCiphers.filter(existingCiphers.contains(_)).toArray
      }

      if (!sslConfig.loose.allowWeakCiphers) {
        val deprecatedCiphers = Ciphers.deprecatedCiphers
        for (deprecatedCipher <- deprecatedCiphers) {
          if (definedCiphers.contains(deprecatedCipher)) {
            throw new IllegalStateException(s"Weak cipher $deprecatedCipher found in ws.ssl.ciphers!")
          }
        }
      }
      definedCiphers
    }

  def buildHostnameVerifier(sslConfig: SSLConfig): HostnameVerifier =
    {
      import java.lang.reflect.Constructor
      // logger.debug("buildHostnameVerifier: enabling hostname verification using {}", sslConfig.hostnameVerifierClass)
      try {
        val ctors = sslConfig.hostnameVerifierClass.getDeclaredConstructors.toList
        val param0 = (ctors find {_.getParameterTypes.length == 0})
        val param1 = (ctors find {_.getParameterTypes.length == 1})
        (param0, param1) match {
          case (Some(ctor), _) => ctor.newInstance().asInstanceOf[HostnameVerifier]
          case (_, Some(ctor)) => ctor.newInstance(NoopLogger.factory()).asInstanceOf[HostnameVerifier]
          case _               => throw new IllegalStateException(s"Cannot configure hostname verifier: sslConfig.hostnameVerifierClass")
        }
      } catch {
        case e: Exception =>
          throw new IllegalStateException("Cannot configure hostname verifier", e)
      }
    }
}
