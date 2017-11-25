/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package gigahorse
final class InMemoryBody private (
  val bytes: Array[Byte]) extends gigahorse.Body() with Serializable {
  
  
  
  override def equals(o: Any): Boolean = o match {
    case x: InMemoryBody => (this.bytes == x.bytes)
    case _ => false
  }
  override def hashCode: Int = {
    37 * (37 * (17 + "gigahorse.InMemoryBody".##) + bytes.##)
  }
  override def toString: String = {
    "InMemoryBody(" + bytes + ")"
  }
  protected[this] def copy(bytes: Array[Byte] = bytes): InMemoryBody = {
    new InMemoryBody(bytes)
  }
  def withBytes(bytes: Array[Byte]): InMemoryBody = {
    copy(bytes = bytes)
  }
}
object InMemoryBody {
  
  def apply(bytes: Array[Byte]): InMemoryBody = new InMemoryBody(bytes)
}
