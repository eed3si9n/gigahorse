package gigahorse

import java.nio.charset.Charset

abstract class HttpWrite[A] {
  def toByteArray(a: A): Array[Byte]
  def contentType: Option[String]
}

object HttpWrite {
  val utf8 = Charset.forName("UTF-8")
  implicit val stringHttpWrite: HttpWrite[String] = new StringHttpWrite
  final class StringHttpWrite extends HttpWrite[String] {
    def toByteArray(a: String): Array[Byte] = a.getBytes(utf8)
    def contentType: Option[String] = None
  }
}
