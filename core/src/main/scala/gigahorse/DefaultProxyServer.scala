/**
 * This code is generated using sbt-datatype.
 */

// DO NOT EDIT MANUALLY
package gigahorse
final class DefaultProxyServer(
  host: String,
  port: Int,
  securedPort: Option[Int],
  authOpt: Option[Realm],
  nonProxyHosts: List[String]) extends gigahorse.ProxyServer(host, port, securedPort, authOpt, nonProxyHosts) {
  
  def this(host: String, port: Int, securedPort: Option[Int]) = this(host, port, securedPort, None, List())
  
  override def equals(o: Any): Boolean = o match {
    case x: DefaultProxyServer => (this.host == x.host) && (this.port == x.port) && (this.securedPort == x.securedPort) && (this.authOpt == x.authOpt) && (this.nonProxyHosts == x.nonProxyHosts)
    case _ => false
  }
  override def hashCode: Int = {
    37 * (37 * (37 * (37 * (37 * (17 + host.##) + port.##) + securedPort.##) + authOpt.##) + nonProxyHosts.##)
  }
  override def toString: String = {
    "DefaultProxyServer(" + host + ", " + port + ", " + securedPort + ", " + authOpt + ", " + nonProxyHosts + ")"
  }
  private[this] def copy(host: String = host, port: Int = port, securedPort: Option[Int] = securedPort, authOpt: Option[Realm] = authOpt, nonProxyHosts: List[String] = nonProxyHosts): DefaultProxyServer = {
    new DefaultProxyServer(host, port, securedPort, authOpt, nonProxyHosts)
  }
  def withHost(host: String): DefaultProxyServer = {
    copy(host = host)
  }
  def withPort(port: Int): DefaultProxyServer = {
    copy(port = port)
  }
  def withSecuredPort(securedPort: Option[Int]): DefaultProxyServer = {
    copy(securedPort = securedPort)
  }
  def withAuthOpt(authOpt: Option[Realm]): DefaultProxyServer = {
    copy(authOpt = authOpt)
  }
  def withNonProxyHosts(nonProxyHosts: List[String]): DefaultProxyServer = {
    copy(nonProxyHosts = nonProxyHosts)
  }
}
object DefaultProxyServer {
  def apply(host: String, port: Int, securedPort: Option[Int]): DefaultProxyServer = new DefaultProxyServer(host, port, securedPort, None, List())
  def apply(host: String, port: Int, securedPort: Option[Int], authOpt: Option[Realm], nonProxyHosts: List[String]): DefaultProxyServer = new DefaultProxyServer(host, port, securedPort, authOpt, nonProxyHosts)
}
