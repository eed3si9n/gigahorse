package gigahorse

import com.ning.http.client.{ Response => XResponse, _ }
import com.ning.http.util.AsyncHttpProviderUtils
import java.nio.charset.Charset

class AhcResponse(ahcResponse: XResponse) extends Response {
  /**
   * @return The underlying response object.
   */
  def underlying[A] = ahcResponse.asInstanceOf[A]

  /**
   * The response body as String.
   */
  lazy val body: String = {
    // RFC-2616#3.7.1 states that any text/* mime type should default to ISO-8859-1 charset if not
    // explicitly set, while Plays default encoding is UTF-8.  So, use UTF-8 if charset is not explicitly
    // set and content type is not text/*, otherwise default to ISO-8859-1
    val contentType = Option(ahcResponse.getContentType).getOrElse("application/octet-stream")
    val charset: String = Option(AsyncHttpProviderUtils.parseCharset(contentType)).getOrElse {
      if (contentType.startsWith("text/")) AsyncHttpProviderUtils.DEFAULT_CHARSET.toString
      else "utf-8"
    }
    ahcResponse.getResponseBody(charset)
  }
}
