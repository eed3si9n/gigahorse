package gigahorse

abstract class Response {
  /**
   * Return the current headers of the request being constructed
   */
  def allHeaders: Map[String, List[String]]

  /**
   * The response body as String.
   */
  def body: String

  /**
   * The response body as a byte array.
   */
  def bodyAsBytes: Array[Byte]

  /**
   * The response body as an `A`.
   */
  def as[A: HttpRead]: A

  /**
   * The response status code.
   */
  def status: Int

  /**
   * The response status message.
   */
  def statusText: String

  /**
   * Get a response header.
   */
  def header(key: String): Option[String]
}
