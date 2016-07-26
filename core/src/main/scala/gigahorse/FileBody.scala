/**
 * This code is generated using sbt-datatype.
 */

// DO NOT EDIT MANUALLY
package gigahorse
final class FileBody(
  val file: java.io.File) extends gigahorse.Body() {
  
  
  
  override def equals(o: Any): Boolean = o match {
    case x: FileBody => (this.file == x.file)
    case _ => false
  }
  override def hashCode: Int = {
    37 * (17 + file.##)
  }
  override def toString: String = {
    "FileBody(" + file + ")"
  }
  private[this] def copy(file: java.io.File = file): FileBody = {
    new FileBody(file)
  }
  def withFile(file: java.io.File): FileBody = {
    copy(file = file)
  }
}
object FileBody {
  def apply(file: java.io.File): FileBody = new FileBody(file)
}
