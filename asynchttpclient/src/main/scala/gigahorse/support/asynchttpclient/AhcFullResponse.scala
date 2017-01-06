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
package support.asynchttpclient

import scala.collection.JavaConverters._
import org.asynchttpclient.{ Response => XResponse, _ }
import org.asynchttpclient.util.HttpUtils
import java.nio.charset.Charset
import java.nio.ByteBuffer
import scala.collection.immutable.TreeMap
import scala.concurrent.{ Future, Promise }

class AhcFullResponse(ahcResponse: XResponse) extends FullResponse {
  /**
   * @return The underlying response object.
   */
  def underlying[A] = ahcResponse.asInstanceOf[A]

  /**
   * The response body as a `ByteBuffer`.
   */
  override def bodyAsByteBuffer: ByteBuffer =
    ahcResponse.getResponseBodyAsByteBuffer

  /**
   * The response body as String.
   */
  override lazy val bodyAsString: String = {
    // RFC-2616#3.7.1 states that any text/* mime type should default to ISO-8859-1 charset if not
    // explicitly set, while Plays default encoding is UTF-8.  So, use UTF-8 if charset is not explicitly
    // set and content type is not text/*, otherwise default to ISO-8859-1
    val contentType = Option(ahcResponse.getContentType).getOrElse("application/octet-stream")
    val charset: Charset = Option(HttpUtils.parseCharset(contentType)).getOrElse {
      if (contentType.startsWith("text/")) HttpUtils.DEFAULT_CHARSET
      else Charset.forName("utf-8")
    }
    ahcResponse.getResponseBody(charset)
  }

  /**
   * Return the headers of the response as a case-insensitive map
   */
  override lazy val allHeaders: Map[String, List[String]] =
    TreeMap[String, List[String]]() ++
      ahcResponse.getHeaders.asScala.toList.groupBy(_.getKey).mapValues(_.map(_.getValue))

  /**
   * The response status code.
   */
  override def status: Int = ahcResponse.getStatusCode

  /**
   * The response status message.
   */
  override def statusText: String = ahcResponse.getStatusText

  /**
   * Get a response header.
   */
  override def header(key: String): Option[String] = Option(ahcResponse.getHeader(key))
}
