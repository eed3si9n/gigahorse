/**
 * This code is generated using [[https://www.scala-sbt.org/contraband]].
 */

// DO NOT EDIT MANUALLY
package gigahorse
final class ConfigMemorySize private (
  val bytes: Long) extends Serializable {
  
  
  
  override def equals(o: Any): Boolean = this.eq(o.asInstanceOf[AnyRef]) || (o match {
    case x: ConfigMemorySize => (this.bytes == x.bytes)
    case _ => false
  })
  override def hashCode: Int = {
    37 * (37 * (17 + "gigahorse.ConfigMemorySize".##) + bytes.##)
  }
  override def toString: String = {
    "ConfigMemorySize(" + bytes + ")"
  }
  private def copy(bytes: Long): ConfigMemorySize = {
    new ConfigMemorySize(bytes)
  }
  def withBytes(bytes: Long): ConfigMemorySize = {
    copy(bytes = bytes)
  }
}
object ConfigMemorySize {
  
  def apply(bytes: Long): ConfigMemorySize = new ConfigMemorySize(bytes)
}
