/**
 * This code is generated using [[https://www.scala-sbt.org/contraband]].
 */

// DO NOT EDIT MANUALLY
package gigahorse
final class MultipartFormBody private (
  val parts: Vector[FormPart]) extends gigahorse.Body() with Serializable {
  
  private def this() = this(Vector())
  
  override def equals(o: Any): Boolean = this.eq(o.asInstanceOf[AnyRef]) || (o match {
    case x: MultipartFormBody => (this.parts == x.parts)
    case _ => false
  })
  override def hashCode: Int = {
    37 * (37 * (17 + "gigahorse.MultipartFormBody".##) + parts.##)
  }
  override def toString: String = {
    "MultipartFormBody(" + parts + ")"
  }
  private def copy(parts: Vector[FormPart]): MultipartFormBody = {
    new MultipartFormBody(parts)
  }
  def withParts(parts: Vector[FormPart]): MultipartFormBody = {
    copy(parts = parts)
  }
}
object MultipartFormBody {
  def apply(parts: FormPart*): MultipartFormBody = new MultipartFormBody(parts.toVector)
  def apply(): MultipartFormBody = new MultipartFormBody()
  def apply(parts: Vector[FormPart]): MultipartFormBody = new MultipartFormBody(parts)
}
