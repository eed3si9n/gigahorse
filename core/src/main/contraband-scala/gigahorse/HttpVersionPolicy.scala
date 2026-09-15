/**
 * This code is generated using [[https://www.scala-sbt.org/contraband]].
 */

// DO NOT EDIT MANUALLY
package gigahorse
sealed abstract class HttpVersionPolicy extends Serializable
object HttpVersionPolicy {
  
  
  case object Http1_1 extends HttpVersionPolicy
  case object Http2 extends HttpVersionPolicy
  case object Negotiate extends HttpVersionPolicy
}
