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
import org.reactivestreams.{ Publisher, Subscription, Subscriber }
import shaded.ahc.org.asynchttpclient.{ Response => XResponse }
import scala.collection.immutable.TreeMap
import shaded.ahc.org.asynchttpclient.HttpResponseBodyPart
import java.nio.ByteBuffer
import java.nio.charset.Charset

/** Represents a stream response.
 */
class AhcStreamResponse(ahcResponse: XResponse, publisher: Publisher[HttpResponseBodyPart]) extends StreamResponse {
  /**
   * @return The underlying response object.
   */
  def underlying[A] = ahcResponse.asInstanceOf[A]

  /**
   * The response body as Reactive Stream.
   */
  override def byteBuffers: Stream[ByteBuffer] =
    new AhcStream(new ByteBufferPublisher(publisher))

  /**
   * The response body as Reactive Stream of Newline delimited strings.
   */
  override def newLineDelimited: Stream[String] =
    new AhcStream(new DelimitedPublisher(publisher, '\n'.toByte, Charset.forName("UTF-8")))

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

class ByteBufferPublisher(xpublisher: Publisher[HttpResponseBodyPart]) extends Publisher[ByteBuffer] {
  def subscribe(s: Subscriber[_ >: ByteBuffer]): Unit =
    {
      xpublisher.subscribe(new SubscriberAdapter(s))
    }
  class SubscriberAdapter(s: Subscriber[_ >: ByteBuffer]) extends Subscriber[HttpResponseBodyPart] {
    def onComplete(): Unit = s.onComplete()
    def onError(e: Throwable) = s.onError(e)
    def onNext(p: HttpResponseBodyPart): Unit = s.onNext(p.getBodyByteBuffer)
    def onSubscribe(x: Subscription): Unit = s.onSubscribe(x)
  }
}

class DelimitedPublisher(xpublisher: Publisher[HttpResponseBodyPart],
  val delimiter: Byte,
  val charset: Charset) extends Publisher[String] {
  def subscribe(s: Subscriber[_ >: String]): Unit =
    {
      xpublisher.subscribe(new SubscriberAdapter(s))
    }
  class SubscriberAdapter(s: Subscriber[_ >: String]) extends Subscriber[HttpResponseBodyPart] {
    var buffer: Vector[Byte] = Vector()
    def onComplete(): Unit =
      {
        if (buffer.nonEmpty) {
          val chunk = buffer
          s.onNext(new String(chunk.toArray, charset))
        }
        s.onComplete()
      }
    def onError(e: Throwable) = s.onError(e)
    def onNext(p: HttpResponseBodyPart): Unit = {
      buffer = buffer ++ p.getBodyPartBytes.toVector
      var delimPos = buffer.indexOf(delimiter)
      while (delimPos > 0) {
        val chunk = buffer.take(delimPos)
        buffer = buffer.drop(delimPos + 1)
        s.onNext(new String(chunk.toArray, charset))
        delimPos = buffer.indexOf(delimiter)
      }
    }
    def onSubscribe(x: Subscription): Unit = s.onSubscribe(x)
  }
}
