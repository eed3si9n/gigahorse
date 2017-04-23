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
package support.asynchttpclient

import shaded.ahc.org.asynchttpclient
import asynchttpclient.{ Response => XResponse, _ }

abstract class AhcCompletionHandler[A] extends CompletionHandler[A] with AhcHandler {
  val builder = new XResponse.ResponseBuilder

  def onBodyPartReceived(content: HttpResponseBodyPart): State = {
    builder.accumulate(content)
    State.Continue
  }

  def onStatusReceived(status: HttpResponseStatus): State = {
    builder.reset()
    builder.accumulate(status)
    State.Continue
  }

  def onHeadersReceived(headers: HttpResponseHeaders): State = {
    builder.accumulate(headers)
    State.Continue
  }

  def onCompleted(response: FullResponse): A
}
