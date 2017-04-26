/*
 * Copyright 2016 by Eugene Yokota
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
package support.akkahttp

import java.nio.ByteBuffer
import scala.collection.immutable.TreeMap
import akka.http.scaladsl.model._
import akka.stream.Materializer

class AkkaHttpFullResponse(akkaHttpResponse: HttpResponse, entity: HttpEntity.Strict)(implicit val fm: Materializer) extends FullResponse {
  /**
   * @return The underlying entity object.
   */
  def underlying[A] = entity.asInstanceOf[A]

  def close(): Unit =
    {
      akkaHttpResponse.discardEntityBytes(fm)
    }

  /**
   * @return The underlying response object.
   */
  def underlyingResponse[A] = akkaHttpResponse.asInstanceOf[A]

  /**
   * The response body as a `ByteBuffer`.
   */
  def bodyAsByteBuffer: ByteBuffer = entity.data.asByteBuffer

  /**
   * The response body as String.
   */
  lazy val bodyAsString: String = {
    // RFC-2616#3.7.1 states that any text/* mime type should default to ISO-8859-1 charset if not
    // explicitly set, while Plays default encoding is UTF-8.  So, use UTF-8 if charset is not explicitly
    // set and content type is not text/*, otherwise default to ISO-8859-1
    val contentType = entity.contentType
    val charset = contentType.charsetOption getOrElse HttpCharsets.`UTF-8`
    entity.data.decodeString(charset.value)
  }

  /**
   * Return the headers of the response as a case-insensitive map
   */
  lazy val allHeaders: Map[String, List[String]] =
    TreeMap[String, List[String]]() ++
    akkaHttpResponse.headers.groupBy(_.name).mapValues(vs => vs.toList map { _.value })

  /**
   * The response status code.
   */
  def status: Int = akkaHttpResponse.status.intValue

  /**
   * The response status message.
   */
  def statusText: String = akkaHttpResponse.status.reason

  /**
   * Get a response header.
   */
  def header(key: String): Option[String] =
    akkaHttpResponse.headers.find(_.name == key) map { _.value }
}
