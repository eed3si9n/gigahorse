package gigahorse

import scala.concurrent.Future

abstract class HttpClient extends AutoCloseable {
  def underlying[A]: A

  /** Closes this client, and releases underlying resources. */
  def close(): Unit

  /** Executes the request. */
  def execute(request: Request): Future[Response]
}
