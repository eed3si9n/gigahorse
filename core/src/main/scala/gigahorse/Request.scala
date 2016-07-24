package gigahorse

import scala.concurrent.Future

abstract class Request {
  def url: String

  def execute: Future[Response]
}
