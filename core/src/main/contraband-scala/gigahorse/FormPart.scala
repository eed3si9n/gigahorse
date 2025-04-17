/**
 * This code is generated using [[https://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package gigahorse
/**
 * FormPart represents one part of a multipart form.
 * @param name The name this form part
 * @param body The body of this form part
 * @param headers The headers for this form part
 */
final class FormPart private (
  val name: String,
  val body: Body,
  val headers: Map[String, List[String]]) extends Serializable {
  import java.nio.charset.Charset
  def contentType: Option[String] =
  {
    this.headers.find(p => p._1 == HeaderNames.CONTENT_TYPE) map { case (header, values) =>
    values.head
  }}
  def withContentType(ct: String): FormPart = this.addHeader(HeaderNames.CONTENT_TYPE -> ct)
  def withContentType(mt: String, charset: Charset): FormPart = this.withContentType(mt + ";charset=" + charset.toString)
  def addHeader(headers0: (String, String)*): FormPart = this.addHeaders(headers0*)
  def addHeaders(headers0: (String, String)*): FormPart = copy(headers = this.headers ++ Map((headers0 map { case (k, v) => k -> List(v) })*))
  def addHeaders(headers0: Map[String, List[String]]): FormPart = copy(headers = this.headers ++ headers0)
  private def this(name: String) = this(name, EmptyBody(), Map())
  private def this(name: String, body: Body) = this(name, body, Map())
  
  override def equals(o: Any): Boolean = o match {
    case x: FormPart => (this.name == x.name) && (this.body == x.body) && (this.headers == x.headers)
    case _ => false
  }
  override def hashCode: Int = {
    37 * (37 * (37 * (37 * (17 + "gigahorse.FormPart".##) + name.##) + body.##) + headers.##)
  }
  override def toString: String = {
    "FormPart(" + name + ", " + body + ", " + headers + ")"
  }
  private[this] def copy(name: String = name, body: Body = body, headers: Map[String, List[String]] = headers): FormPart = {
    new FormPart(name, body, headers)
  }
  def withName(name: String): FormPart = {
    copy(name = name)
  }
  def withBody(body: Body): FormPart = {
    copy(body = body)
  }
  def withHeaders(headers: Map[String, List[String]]): FormPart = {
    copy(headers = headers)
  }
}
object FormPart {
  import java.io.File
  def apply(name: String, file: File): FormPart = new FormPart(name, FileBody(file))
  def apply(name: String, file: File, contentType: String): FormPart = new FormPart(name, FileBody(file)).addHeader(HeaderNames.CONTENT_TYPE -> contentType)
  def apply(name: String, body: String, contentType: String): FormPart = new FormPart(name, InMemoryBody(body.getBytes("UTF-8"))).addHeader(HeaderNames.CONTENT_TYPE -> contentType)
  def apply(name: String): FormPart = new FormPart(name)
  def apply(name: String, body: Body): FormPart = new FormPart(name, body)
  def apply(name: String, body: Body, headers: Map[String, List[String]]): FormPart = new FormPart(name, body, headers)
}
