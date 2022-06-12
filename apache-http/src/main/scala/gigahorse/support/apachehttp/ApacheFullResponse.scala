/*
 * Copyright 2022 by Eugene Yokota
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
package support.apachehttp

import java.nio.ByteBuffer
import shaded.apache.org.apache.http.{ Header, HttpResponse => XResponse }
import shaded.apache.org.apache.http.util.EntityUtils

/**
 * https://javadoc.io/doc/org.apache.httpcomponents/httpcore/4.4.1/org/apache/http/HttpResponse.html
 */
class ApacheFullResponse(apacheResponse: XResponse) extends FullResponse {
  /**
   * @return The underlying response object.
   */
  override def underlying[A] = apacheResponse.asInstanceOf[A]
  private[this] def entity = apacheResponse.getEntity
  private[this] def statusLine = apacheResponse.getStatusLine

  override def close(): Unit = EntityUtils.consumeQuietly(entity)

  /**
   * The response body as a `ByteBuffer`.
   */
  override def bodyAsByteBuffer: ByteBuffer =
    ByteBuffer.wrap(EntityUtils.toByteArray(entity))

  /**
   * The response body as String.
   */
  override lazy val bodyAsString: String =
    EntityUtils.toString(entity)

  /**
   * Return the headers of the response as a case-insensitive map
   */
  override lazy val allHeaders: Map[String, List[String]] =
    apacheResponse.getAllHeaders.toList
      .groupBy(_.getName)
      .map { case (k, vs) =>
        (k, vs.map(_.getValue))
      }

  /**
   * The response status code.
   */
  override def status: Int = ApacheFullResponse.status(apacheResponse)

  /**
   * The response status message.
   */
  override def statusText: String =
    Option(statusLine.getReasonPhrase).getOrElse("")

  /**
   * Get a response header.
   */
  override def header(key: String): Option[String] =
    Option(apacheResponse.getFirstHeader(key)).map(_.getValue)
}

object ApacheFullResponse {
  def headers(apacheResponse: XResponse): List[Header] =
    apacheResponse.getAllHeaders.toList

  def status(apacheResponse: XResponse): Int =
    apacheResponse.getStatusLine.getStatusCode
}
