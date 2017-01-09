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

import akka.http.scaladsl.model.StatusCode
import scala.concurrent.Future

trait OkHandler[A] extends AkkaHttpCompletionHandler[A] {
  abstract override def onStatusReceived(status: StatusCode): State =
    {
      if (status.isFailure) State.Abort
      else super.onStatusReceived(status)
    }
}

object OkHandler {
  def apply[A](f: FullResponse => A): FunctionHandler[A] =
    new FunctionHandler[A](f) with OkHandler[A] {}
  def stream[A](f: StreamResponse => Future[A]): StreamFunctionHandler[A] =
    new StreamFunctionHandler[A](f) with OkHandler[A] {}
}
