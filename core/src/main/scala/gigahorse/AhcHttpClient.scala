package gigahorse

import com.ning.http.client.{ Response => XResponse, _ }
import scala.concurrent.{ Future, Promise }

class AhcHttpClient(config: AsyncHttpClientConfig) extends HttpClient {
  private val asyncHttpClient = new AsyncHttpClient(config)
  def underlying[A]: A = asyncHttpClient.asInstanceOf[A]
  def url(url: String): Request =
    new AhcRequest(this, url, "GET" /*, EmptyBody, Map(), Map(), None, None, None, None, None, None, None */)
  def close(): Unit = asyncHttpClient.close()
  override def toString: String =
    s"""AchHttpClient($config)"""

  private[gigahorse] def executeRequest(request: AhcRequest): Future[Response] =
    {
      import com.ning.http.client.AsyncCompletionHandler
      val result = Promise[AhcResponse]()
      val xrequest = request.buildRequest
      asyncHttpClient.executeRequest(xrequest, new AsyncCompletionHandler[XResponse]() {
        override def onCompleted(response: XResponse) = {
          result.success(new AhcResponse(response))
          response
        }
        override def onThrowable(t: Throwable) = {
          result.failure(t)
        }
      })
      result.future
    }
}
