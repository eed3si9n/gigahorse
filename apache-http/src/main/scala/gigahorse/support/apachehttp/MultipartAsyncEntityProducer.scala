/*
 * Copyright 2025 by Eugene Yokota
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

import java.io.{ ByteArrayInputStream, FileInputStream, InputStream }
import java.util as ju
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import shaded.apache.org.apache.hc.core5
import core5.http.ContentType
import core5.http.nio.{ AsyncEntityProducer, DataStreamChannel }
import scala.collection.mutable

class MultipartAsyncEntityProducer(body: MultipartFormBody) extends AsyncEntityProducer {
  private val boundary = ju.UUID.randomUUID().toString
  private val parts = body.parts
  private val partIterator = parts.iterator
  private val pending = new mutable.Queue[ByteBuffer]()
  private var finished = false
  // save the inputstream across produce runs
  private var currentStream: Option[InputStream] = None

  def getBoundary: String = boundary

  override def getContentType: String =
    s"multipart/form-data; boundary=$boundary"
  override def getContentLength: Long = -1 // unknown length
  override def getContentEncoding: String = null
  override def isChunked: Boolean = true
  override def getTrailerNames: ju.Set[String] = ju.Collections.emptySet()
  override def available: Int =
    if (pending.nonEmpty) pending.front.remaining()
    else 0

  override def isRepeatable(): Boolean = false

  /**
   * Called when the underlying data channel is ready to accept more data.
   * When channel.write(...) writes less bytes than the byteBuffer,
   * get out, since the data channel is likely no longer able to accept
   * the bytes.
   */
  override def produce(channel: DataStreamChannel): Unit = {
    produce0(channel)
  }
  private def produce0(channel: DataStreamChannel): Unit = {
    var incompleteWrite = false
    while (pending.nonEmpty && !incompleteWrite) {
      val buf: ByteBuffer = pending.front
      channel.write(buf)
      if (buf.hasRemaining) incompleteWrite = true
      else pending.dequeue()
    }
    // exit early on incomplete write
    if (incompleteWrite) ()
    else
      currentStream match {
        case Some(stream) =>
          val buf = new Array[Byte](1024 * 1024)
          val read = stream.read(buf)
          if (read == -1) {
            stream.close()
            pending.enqueue(encode("\r\n"))
            currentStream = None
            produce0(channel)
          } else {
            pending.enqueue(ByteBuffer.wrap(buf, 0, read))
            produce0(channel)
          }
        case None =>
          if (partIterator.hasNext) {
            val part = partIterator.next()
            writePartHeaders(part)
            val stream = partToStream(part)
            currentStream = Some(stream)
            produce0(channel)
          } else if (!finished) {
            pending.enqueue(encode(s"--$boundary--\r\n"))
            finished = true
            produce0(channel)
          } else
            channel.endStream()
      }
  }
  private def partToStream(part: FormPart): InputStream =
    part.body match {
      case b: InMemoryBody =>
        new ByteArrayInputStream(b.bytes)
      case b: FileBody =>
        new FileInputStream(b.file)
      case b: EmptyBody =>
        new ByteArrayInputStream(Array())
      case b: MultipartFormBody =>
        sys.error(s"multipart body cannot be nested")
    }

  private def writePartHeaders(part: FormPart): Unit = {
    val sb = new StringBuilder
    sb.append(s"--$boundary\r\n")
    sb.append(s"""Content-Disposition: form-data; name="${part.name}"""")
    part.body match {
      case b: FileBody =>
        sb.append(s"""; filename="${b.file.getName}"""")
      case _ => ()
    }
    sb.append("\r\n")
    val ct = part.body match {
      case b: FileBody =>
        part.contentType.getOrElse(ContentType.DEFAULT_BINARY.toString())
      case _ =>
        part.contentType.getOrElse(ContentType.TEXT_PLAIN.withCharset("UTF-8"))
    }
    sb.append(s"Content-Type: $ct\r\n")
    sb.append("\r\n")
    pending.enqueue(encode(sb.toString()))
  }
  private def encode(str: String): ByteBuffer =
    ByteBuffer.wrap(str.getBytes(StandardCharsets.UTF_8))
  override def releaseResources(): Unit = {
    // parts.foreach(p => try p.body.close() catch { case _: Throwable => () })
    pending.clear()
  }
  override def failed(cause: Exception): Unit = {
    releaseResources()
  }
}

object MultipartAsyncEntityProducer {
  def apply(body: MultipartFormBody): MultipartAsyncEntityProducer =
    new MultipartAsyncEntityProducer(body)
}
