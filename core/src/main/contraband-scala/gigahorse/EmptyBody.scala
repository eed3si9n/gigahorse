/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package gigahorse
final class EmptyBody private () extends gigahorse.Body() with Serializable {



override def equals(o: Any): Boolean = o match {
  case x: EmptyBody => true
  case _ => false
}
override def hashCode: Int = {
  17
}
override def toString: String = {
  "EmptyBody()"
}
protected[this] def copy(): EmptyBody = {
  new EmptyBody()
}

}
object EmptyBody {
  
  def apply(): EmptyBody = new EmptyBody()
}
