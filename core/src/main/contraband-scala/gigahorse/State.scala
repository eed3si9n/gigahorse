/**
 * This code is generated using [[https://www.scala-sbt.org/contraband]].
 */

// DO NOT EDIT MANUALLY
package gigahorse
sealed abstract class State extends Serializable
object State {
  
  
  case object Continue extends State
  case object Abort extends State
  case object Upgrade extends State
}
