package gigahorse

import scala.concurrent.Future
import java.io.File

abstract class HttpClient extends AutoCloseable {
  def underlying[A]: A

  /** Closes this client, and releases underlying resources. */
  def close(): Unit

  /** Executes the request. */
  def run(request: Request): Future[Response]

  /** Downloads the request to the file. */
  def download(request: Request, file: File): Future[File]

  def process[A](request: Request, handler: CompletionHandler[A]): Future[A]
}
