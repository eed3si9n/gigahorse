/**
 * This code is generated using [[http://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package gigahorse
final class EncodedString private (
  val string: String,
  val charset: java.nio.charset.Charset) extends Serializable {
  
  
  
  override def equals(o: Any): Boolean = o match {
    case x: EncodedString => (this.string == x.string) && (this.charset == x.charset)
    case _ => false
  }
  override def hashCode: Int = {
    37 * (37 * (37 * (17 + "gigahorse.EncodedString".##) + string.##) + charset.##)
  }
  override def toString: String = {
    "EncodedString(" + string + ", " + charset + ")"
  }
  protected[this] def copy(string: String = string, charset: java.nio.charset.Charset = charset): EncodedString = {
    new EncodedString(string, charset)
  }
  def withString(string: String): EncodedString = {
    copy(string = string)
  }
  def withCharset(charset: java.nio.charset.Charset): EncodedString = {
    copy(charset = charset)
  }
}
object EncodedString {
  
  def apply(string: String, charset: java.nio.charset.Charset): EncodedString = new EncodedString(string, charset)
}
