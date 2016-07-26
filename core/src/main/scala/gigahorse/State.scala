/**
 * This code is generated using sbt-datatype.
 */

// DO NOT EDIT MANUALLY
package gigahorse
sealed abstract class State extends Serializable
object State {
  
  
  case object Continue extends State
  case object Abort extends State
  case object Upgrade extends State
}
