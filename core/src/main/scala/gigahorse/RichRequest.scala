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

import java.io.File
import java.nio.charset.Charset

abstract class BaseRichRequest {
  import AhcHttpClient.{ utf8, setBodyString, setBody }
  def request: Request

  /** Uses GET method.
   */
  def get: Request = request.withMethod("GET")
  /**
   * Uses PATCH method with the given body.
   */
  def patch[A: HttpWrite](body: A): Request = setBody(request.withMethod("PATCH"), body)
  /**
   * Uses PATCH method with the given body.
   */
  def patch(body: String, charset: Charset): Request = setBodyString(request.withMethod("PATCH"), body, utf8)
  /**
   * Uses PATCH method with the given file.
   */
  def patch(file: File): Request = request.withMethod("PATCH").withBody(FileBody(file))
  /**
   * Uses POST method with the given body.
   */
  def post[A: HttpWrite](body: A): Request = setBody(request.withMethod("POST"), body)
  /**
   * Uses POST method with the given body.
   */
  def post(body: String, charset: Charset): Request = setBodyString(request.withMethod("POST"), body, utf8)
  /**
   * Uses POST method with the given file.
   */
  def post(file: File): Request = request.withMethod("POST").withBody(FileBody(file))
  /**
   * Uses PUT method with the given body.
   */
  def put[A: HttpWrite](body: A): Request = setBody(request.withMethod("PUT"), body)
  /**
   * Uses PUT method with the given body.
   */
  def put(body: String, charset: Charset): Request = setBodyString(request.withMethod("PUT"), body, utf8)
  /**
   * Uses PUT method with the given file.
   */
  def put(file: File): Request = request.withMethod("PUT").withBody(FileBody(file))
  /** Uses DELETE method.
   */
  def delete: Request = request.withMethod("DELETE")
  /** Uses HEAD method.
   */
  def head: Request = request.withMethod("HEAD")
  /** Uses OPTIONS method.
   */
  def options: Request = request.withMethod("OPTIONS")
}

final class RichRequest(val request: Request) extends BaseRichRequest
