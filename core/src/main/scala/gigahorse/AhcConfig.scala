package gigahorse

import com.ning.http.client._
import scala.concurrent.duration._

object AhcConfig {
  /** Build `AsyncHttpClientConfig` */
  def buildConfig(config: Config): AsyncHttpClientConfig =
    {
      val builder = new AsyncHttpClientConfig.Builder()
      builder.setConnectTimeout(toMillis(config.connectTimeout))
      builder.setReadTimeout(toMillis(config.readTimeout))
      builder.setRequestTimeout(toMillis(config.requestTimeout))
      builder.setFollowRedirect(config.followRedirects)
      builder.setUseProxyProperties(config.useProxyProperties)
      builder.setCompressionEnforced(config.compressionEnforced)
      builder.build()
    }

  def toMillis(duration: Duration): Int =
    if (duration.isFinite()) duration.toMillis.toInt
    else -1
}
