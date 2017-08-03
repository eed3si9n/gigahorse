/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package gigahorse
final class DefaultProxyServer private (
  host: String,
  port: Int,
  securedPort: Option[Int],
  authOpt: Option[Realm],
  nonProxyHosts: List[String]) extends gigahorse.ProxyServer(host, port, securedPort, authOpt, nonProxyHosts) with Serializable {
  def withAuth(auth: Realm): DefaultProxyServer = copy(authOpt = Some(auth))
  def withAuth(username: String, password: String): DefaultProxyServer = copy(authOpt = Some(Realm(username = username, password = password)))
  def withAuth(username: String, password: String, scheme: AuthScheme): DefaultProxyServer = copy(authOpt = Some(Realm(username = username, password = password, scheme = scheme)))
  private def this(host: String, port: Int, securedPort: Option[Int]) = this(host, port, securedPort, None, List())
  
  override def equals(o: Any): Boolean = o match {
    case x: DefaultProxyServer => (this.host == x.host) && (this.port == x.port) && (this.securedPort == x.securedPort) && (this.authOpt == x.authOpt) && (this.nonProxyHosts == x.nonProxyHosts)
    case _ => false
  }
  override def hashCode: Int = {
    37 * (37 * (37 * (37 * (37 * (37 * (17 + "gigahorse.DefaultProxyServer".##) + host.##) + port.##) + securedPort.##) + authOpt.##) + nonProxyHosts.##)
  }
  override def toString: String = {
    "DefaultProxyServer(" + host + ", " + port + ", " + securedPort + ", " + authOpt + ", " + nonProxyHosts + ")"
  }
  protected[this] def copy(host: String = host, port: Int = port, securedPort: Option[Int] = securedPort, authOpt: Option[Realm] = authOpt, nonProxyHosts: List[String] = nonProxyHosts): DefaultProxyServer = {
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
  def withSecuredPort(securedPort: Int): DefaultProxyServer = {
    copy(securedPort = Option(securedPort))
  }
  def withAuthOpt(authOpt: Option[Realm]): DefaultProxyServer = {
    copy(authOpt = authOpt)
  }
  def withAuthOpt(authOpt: Realm): DefaultProxyServer = {
    copy(authOpt = Option(authOpt))
  }
  def withNonProxyHosts(nonProxyHosts: List[String]): DefaultProxyServer = {
    copy(nonProxyHosts = nonProxyHosts)
  }
}
object DefaultProxyServer {
  
  def apply(host: String, port: Int, securedPort: Option[Int]): DefaultProxyServer = new DefaultProxyServer(host, port, securedPort, None, List())
  def apply(host: String, port: Int, securedPort: Int): DefaultProxyServer = new DefaultProxyServer(host, port, Option(securedPort), None, List())
  def apply(host: String, port: Int, securedPort: Option[Int], authOpt: Option[Realm], nonProxyHosts: List[String]): DefaultProxyServer = new DefaultProxyServer(host, port, securedPort, authOpt, nonProxyHosts)
  def apply(host: String, port: Int, securedPort: Int, authOpt: Realm, nonProxyHosts: List[String]): DefaultProxyServer = new DefaultProxyServer(host, port, Option(securedPort), Option(authOpt), nonProxyHosts)
}
