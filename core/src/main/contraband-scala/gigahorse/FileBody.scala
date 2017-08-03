/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package gigahorse
final class FileBody private (
  val file: java.io.File) extends gigahorse.Body() with Serializable {
  
  
  
  override def equals(o: Any): Boolean = o match {
    case x: FileBody => (this.file == x.file)
    case _ => false
  }
  override def hashCode: Int = {
    37 * (37 * (17 + "gigahorse.FileBody".##) + file.##)
  }
  override def toString: String = {
    "FileBody(" + file + ")"
  }
  protected[this] def copy(file: java.io.File = file): FileBody = {
    new FileBody(file)
  }
  def withFile(file: java.io.File): FileBody = {
    copy(file = file)
  }
}
object FileBody {
  
  def apply(file: java.io.File): FileBody = new FileBody(file)
}
