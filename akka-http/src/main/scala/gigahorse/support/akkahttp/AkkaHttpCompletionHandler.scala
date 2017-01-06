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

import scala.concurrent.{ Future, ExecutionContext }
import akka.http.scaladsl.model.{ HttpResponse, StatusCode, HttpHeader }
import akka.stream.Materializer

abstract class AkkaHttpCompletionHandler[A] extends CompletionHandler[A] {
  def onStatusReceived(status: StatusCode): State = State.Continue
  def onHeadersReceived(headers: Seq[HttpHeader]): State = State.Continue
  def onCompleted(response: FullResponse): A
  def onPartialResponse(httpResponse: HttpResponse, config: Config)(implicit fm: Materializer, ec: ExecutionContext): Future[A] =
    for {
      entity <- httpResponse.entity.toStrict(config.requestTimeout)
    } yield onCompleted(new AkkaHttpFullResponse(httpResponse, entity))
}
