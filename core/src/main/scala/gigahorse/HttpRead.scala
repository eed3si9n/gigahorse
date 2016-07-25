package gigahorse

abstract class HttpRead[A] {
  def fromByteArray(bytes: Array[Byte], contentType: Option[String]): A
}
