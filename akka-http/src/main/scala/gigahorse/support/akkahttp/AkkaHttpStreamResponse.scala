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
package support.akkahttp

import java.nio.ByteBuffer
import scala.concurrent.ExecutionContext
import scala.collection.immutable.TreeMap
import akka.util.ByteString
import akka.http.scaladsl.model._
import akka.stream.Materializer
import akka.stream.scaladsl.{ Source, Framing }

/** Represents a stream response.
 */
class AkkaHttpStreamResponse(akkaHttpResponse: HttpResponse, config: Config)(implicit fm: Materializer, ec: ExecutionContext) extends StreamResponse {
  /**
   * @return The underlying entity object.
   */
  def underlying[A] = akkaHttpResponse.asInstanceOf[A]

  def asSource: Source[ByteString, Any] = akkaHttpResponse.entity.dataBytes

  /**
   * The response body as Reactive Stream.
   */
  override def byteBuffers: Stream[ByteBuffer] = new AkkaHttpStream(byteBufferSource)

  def byteBufferSource: Source[ByteBuffer, Any] =
    asSource
      .groupedWithin(64 * 1024, config.frameTimeout)
      .mapConcat(xs => xs map { _.toByteBuffer })

  /**
   * The response body as Reactive Stream of Newline delimited strings.
   */
  override def newLineDelimited: Stream[String] = new AkkaHttpStream(newLineDelimitedSource)

  def newLineDelimitedSource: Source[String, Any] =
    asSource
      .via(Framing.delimiter(ByteString("\n"),
        maximumFrameLength = config.maxFrameSize.bytes.toInt, allowTruncation = false))
      .map(_.utf8String)

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
