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
      builder.setFollowRedirect(config.followRedirect)
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
      builder.build()
    }

  def toMillis(duration: Duration): Int =
    if (duration.isFinite()) duration.toMillis.toInt
    else -1
}
