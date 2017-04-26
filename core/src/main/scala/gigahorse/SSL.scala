/*
 * Original implementation (C) 2009-2016 Lightbend Inc. (https://www.lightbend.com).
 * Adapted and extended in 2017 by Eugene Yokota
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

import com.typesafe.sslconfig.ssl._
import com.typesafe.sslconfig.util.NoopLogger
import javax.net.ssl._
import java.security.KeyStore
import java.security.cert.{ CertPathValidatorException, X509Certificate }

private[gigahorse] object SSL {
  def buildContext(sslConfig: SSLConfigSettings): (SSLContext, Option[TrustManager]) = {
    if (sslConfig.default) {
      validateDefaultTrustManager(sslConfig)
      (SSLContext.getDefault, None)
    } else {
      // break out the static methods as much as we can...
      val keyManagerFactory = buildKeyManagerFactory(sslConfig)
      val trustManagerFactory = buildTrustManagerFactory(sslConfig)
      val tmf = TrustManagerFactory.getInstance(sslConfig.trustManagerConfig.algorithm)
      tmf.init(null.asInstanceOf[KeyStore])
      val trustManager: X509TrustManager = tmf.getTrustManagers()(0).asInstanceOf[X509TrustManager]
      val context = new ConfigSSLContextBuilder(NoopLogger.factory(), sslConfig, keyManagerFactory, trustManagerFactory).build()
      val trustManagerOpt = Option(trustManager)
      (context, trustManagerOpt)
    }
  }

  def buildKeyManagerFactory(ssl: SSLConfigSettings): KeyManagerFactoryWrapper =
    new DefaultKeyManagerFactoryWrapper(ssl.keyManagerConfig.algorithm)

  def buildTrustManagerFactory(ssl: SSLConfigSettings): TrustManagerFactoryWrapper =
    new DefaultTrustManagerFactoryWrapper(ssl.trustManagerConfig.algorithm)

  def validateDefaultTrustManager(sslConfig: SSLConfigSettings): Unit =
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
          case _: CertPathValidatorException =>
            // logger.warn("You are using ws.ssl.default=true and have a weak certificate in your default trust store!  (You can modify ws.ssl.disabledKeyAlgorithms to remove this message.)", e)
        }
      }
    }

  lazy val insecureTrustManager: X509TrustManager = new X509TrustManager {
    def checkClientTrusted(certs: Array[X509Certificate], authType: String): Unit = ()
    def checkServerTrusted(certs: Array[X509Certificate], authType: String): Unit = ()
    def getAcceptedIssuers(): Array[X509Certificate] = Array()
  }

  lazy val insecureHostnameVerifier: HostnameVerifier = new HostnameVerifier {
    def verify(hostname: String, session: SSLSession): Boolean = true
  }
}
