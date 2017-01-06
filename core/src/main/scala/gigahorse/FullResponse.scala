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

import scala.concurrent._
import java.nio.ByteBuffer

/** Represents a completed response.
 */
abstract class FullResponse {
  /**
   * Return the current headers of the request being constructed
   */
  def allHeaders: Map[String, List[String]]

  /**
   * The response body as String.
   */
  def bodyAsString: String

  /**
   * The response body as a `ByteBuffer`.
   */
  def bodyAsByteBuffer: ByteBuffer

  /**
   * The response status code.
   */
  def status: Int

  /**
   * The response status message.
   */
  def statusText: String

  /**
   * Get a response header.
   */
  def header(key: String): Option[String]
}
