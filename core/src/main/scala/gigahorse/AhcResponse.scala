/*
 * Original implementation (C) 2009-2016 Lightbend Inc. (https://www.lightbend.com).
 * Adapted and extended in 2016 by Eugene Yokota
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gigahorse

import scala.collection.JavaConverters._
import com.ning.http.client.{ Response => XResponse, _ }
import com.ning.http.util.AsyncHttpProviderUtils
import java.nio.charset.Charset
import scala.collection.immutable.TreeMap

class AhcResponse(ahcResponse: XResponse) extends Response {
  /**
   * @return The underlying response object.
   */
  def underlying[A] = ahcResponse.asInstanceOf[A]

  /**
   * The response body as a byte array.
   */
  def bodyAsBytes: Array[Byte] = ahcResponse.getResponseBodyAsBytes

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

  /**
   * Return the headers of the response as a case-insensitive map
   */
  lazy val allHeaders: Map[String, List[String]] =
    TreeMap[String, List[String]]() ++
      mapAsScalaMapConverter(ahcResponse.getHeaders).asScala.mapValues(_.asScala.toList)

  /**
   * The response status code.
   */
  def status: Int = ahcResponse.getStatusCode

  /**
   * The response status message.
   */
  def statusText: String = ahcResponse.getStatusText

  /**
   * Get a response header.
   */
  def header(key: String): Option[String] = Option(ahcResponse.getHeader(key))
}
