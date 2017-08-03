/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package gigahorse
abstract class ProxyServer(
  /** The hostname of the proxy server. */
  val host: String,
  /** The port of the proxy server. */
  val port: Int,
  /** The secured port of the proxy server. */
  val securedPort: Option[Int],
  /** The realm of the proxy server. */
  val authOpt: Option[Realm],
  val nonProxyHosts: List[String]) extends Serializable {
  
  def this(host: String, port: Int, securedPort: Option[Int]) = this(host, port, securedPort, None, List())
  
  
  override def equals(o: Any): Boolean = o match {
    case x: ProxyServer => (this.host == x.host) && (this.port == x.port) && (this.securedPort == x.securedPort) && (this.authOpt == x.authOpt) && (this.nonProxyHosts == x.nonProxyHosts)
    case _ => false
  }
  override def hashCode: Int = {
    37 * (37 * (37 * (37 * (37 * (37 * (17 + "gigahorse.ProxyServer".##) + host.##) + port.##) + securedPort.##) + authOpt.##) + nonProxyHosts.##)
  }
  override def toString: String = {
    "ProxyServer(" + host + ", " + port + ", " + securedPort + ", " + authOpt + ", " + nonProxyHosts + ")"
  }
}
object ProxyServer {
  
}
