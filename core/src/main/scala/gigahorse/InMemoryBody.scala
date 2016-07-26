/**
 * This code is generated using sbt-datatype.
 */

// DO NOT EDIT MANUALLY
package gigahorse
final class InMemoryBody(
  val bytes: Array[Byte]) extends gigahorse.Body() {
  
  
  
  override def equals(o: Any): Boolean = o match {
    case x: InMemoryBody => (this.bytes == x.bytes)
    case _ => false
  }
  override def hashCode: Int = {
    37 * (17 + bytes.##)
  }
  override def toString: String = {
    "InMemoryBody(" + bytes + ")"
  }
  private[this] def copy(bytes: Array[Byte] = bytes): InMemoryBody = {
    new InMemoryBody(bytes)
  }
  def withBytes(bytes: Array[Byte]): InMemoryBody = {
    copy(bytes = bytes)
  }
}
object InMemoryBody {
  def apply(bytes: Array[Byte]): InMemoryBody = new InMemoryBody(bytes)
}
