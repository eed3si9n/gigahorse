/**
 * This code is generated using sbt-datatype.
 */

// DO NOT EDIT MANUALLY
package gigahorse
abstract class Body() extends Serializable {




override def equals(o: Any): Boolean = o match {
  case x: Body => true
  case _ => false
}
override def hashCode: Int = {
  17
}
override def toString: String = {
  "Body()"
}
}
