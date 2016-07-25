package gigahorse

import com.ning.http.client.{ Response => XResponse, Request => XRequest, _ }
import com.ning.http.client.AsyncHandler.STATE

abstract class CompletionHandler[A] {
  val builder = new XResponse.ResponseBuilder

  def onBodyPartReceived(content: HttpResponseBodyPart): State = {
    builder.accumulate(content)
    State.Continue
  }

  def onStatusReceived(status: HttpResponseStatus): State = {
    builder.reset()
    builder.accumulate(status)
    State.Continue
  }

  def onHeadersReceived(headers: HttpResponseHeaders): State = {
    builder.accumulate(headers)
    State.Continue
  }

  def onCompleted(response: Response): A
}
