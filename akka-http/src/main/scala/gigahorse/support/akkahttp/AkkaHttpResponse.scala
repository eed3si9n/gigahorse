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

import scala.collection.JavaConverters._
import java.nio.charset.Charset
import scala.collection.immutable.TreeMap
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{ Unmarshal, FromResponseUnmarshaller }
import scala.concurrent._
import scala.concurrent.duration._
import akka.stream.Materializer

class AkkaHttpResponse(akkaHttpResponse: HttpResponse)(implicit val ec: ExecutionContext, implicit val fm: Materializer) extends Response {
  /**
   * @return The underlying response object.
   */
  def underlying[A] = akkaHttpResponse.asInstanceOf[A]

  /**
   * The response body as a byte array.
   */
  def bodyAsBytes: Array[Byte] = {
    // akkaHttpResponse.getResponseBodyAsBytes
    Await.result(Unmarshal(akkaHttpResponse.entity).to[Array[Byte]], Duration.Inf)
  }

  /**
   * The response body as String.
   */
  lazy val body: String = {
    // RFC-2616#3.7.1 states that any text/* mime type should default to ISO-8859-1 charset if not
    // explicitly set, while Plays default encoding is UTF-8.  So, use UTF-8 if charset is not explicitly
    // set and content type is not text/*, otherwise default to ISO-8859-1
    // val contentType = Option(akkaHttpResponse.getContentType).getOrElse("application/octet-stream")
    // val charset: String = Option(AsyncHttpProviderUtils.parseCharset(contentType)).getOrElse {
    //   if (contentType.startsWith("text/")) AsyncHttpProviderUtils.DEFAULT_CHARSET.toString
    //   else "utf-8"
    // }
    // akkaHttpResponse.getResponseBody(charset)
    Await.result(Unmarshal(akkaHttpResponse.entity).to[String], Duration.Inf)
  }

  /**
   * Return the headers of the response as a case-insensitive map
   */
  lazy val allHeaders: Map[String, List[String]] = Map()
    // TreeMap[String, List[String]]() ++
    //   mapAsScalaMapConverter(akkaHttpResponse.getHeaders).asScala.mapValues(_.asScala.toList)

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
  def header(key: String): Option[String] = ??? // Option(akkaHttpResponse.getHeader(key))
}
