/**
 * This code is generated using [[https://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package gigahorse
final class MultipartFormBody private (
  val parts: Vector[FormPart]) extends gigahorse.Body() with Serializable {
  
  private def this() = this(Vector())
  
  override def equals(o: Any): Boolean = o match {
    case x: MultipartFormBody => (this.parts == x.parts)
    case _ => false
  }
  override def hashCode: Int = {
    37 * (37 * (17 + "gigahorse.MultipartFormBody".##) + parts.##)
  }
  override def toString: String = {
    "MultipartFormBody(" + parts + ")"
  }
  private[this] def copy(parts: Vector[FormPart] = parts): MultipartFormBody = {
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
