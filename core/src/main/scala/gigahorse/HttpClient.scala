package gigahorse

abstract class HttpClient {
  def underlying[A]: A

  /**
   * Generates a request holder which can be used to build requests.
   *
   * @param url The base URL to make HTTP requests to.
   * @return Request
   */
  def url(url: String): Request

  /** Closes this client, and releases underlying resources. */
  def close(): Unit
}
