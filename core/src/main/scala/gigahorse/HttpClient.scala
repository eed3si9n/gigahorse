package gigahorse

import scala.concurrent.Future

abstract class HttpClient extends AutoCloseable {
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

  /** Executes the request. */
  def execute(request: Request): Future[Response]
}
