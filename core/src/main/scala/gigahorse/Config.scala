package gigahorse

import scala.concurrent.duration._

case class Config(connectionTimeout: Duration = 2.minutes,
  idleTimeout: Duration = 2.minutes,
  requestTimeout: Duration = 2.minutes,
  followRedirects: Boolean = true,
  useProxyProperties: Boolean = true,
  userAgent: Option[String] = None,
  compressionEnabled: Boolean = false) {
}

