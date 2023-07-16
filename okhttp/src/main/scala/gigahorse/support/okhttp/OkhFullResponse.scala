/*
 * Copyright 2017 by Eugene Yokota
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
package support.okhttp

import okhttp3.{ Response => XResponse }

import java.nio.ByteBuffer
import scala.collection.JavaConverters._

/** The response body must be consumed or closed.
 */
class OkhFullResponse(okhResponse: XResponse) extends FullResponse {
  /**
   * @return The underlying response object.
   */
  override def underlying[A] = okhResponse.asInstanceOf[A]

  override def close(): Unit = okhResponse.close()

  /**
   * The response body as a `ByteBuffer`.
   */
  override def bodyAsByteBuffer: ByteBuffer = {
    ByteBuffer.wrap(okhResponse.body.bytes)
  }

  /**
   * The response body as String.
   */
  override lazy val bodyAsString: String =
    okhResponse.body.string

  /**
   * Return the headers of the response as a case-insensitive map
   */
  override lazy val allHeaders: Map[String, List[String]] =
    (okhResponse.headers.toMultimap.asScala map { case (k, v) =>
      (k, v.asScala.toList)
    }).toMap

  /**
   * The response status code.
   */
  override def status: Int = okhResponse.code

  /**
   * The response status message.
   */
  override def statusText: String =
    Option(okhResponse.message).getOrElse("")

  /**
   * Get a response header.
   */
  override def header(key: String): Option[String] =
    Option(okhResponse.header(key))
}
