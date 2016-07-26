/**
 * This code is generated using sbt-datatype.
 */

// DO NOT EDIT MANUALLY
package gigahorse
final class EmptyBody() extends gigahorse.Body() {



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
private[this] def copy(): EmptyBody = {
  new EmptyBody()
}

}
object EmptyBody {
  def apply(): EmptyBody = new EmptyBody()
}
