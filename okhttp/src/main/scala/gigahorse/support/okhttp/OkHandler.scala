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

import scala.concurrent.Future

trait OkHandler extends OkhHandler {
  abstract override def onStatusReceived(code: Int): Unit =
    {
      if (code / 100 == 2) super.onStatusReceived(code)
      else throw StatusError(code)
    }
}

object OkHandler {
  abstract class FullOkHandler[A](f: FullResponse => A) extends FunctionHandler[A](f) with OkHandler {}
  abstract class StreamOkHandler[A](f: FullResponse => Future[A]) extends StreamFunctionHandler[A](f) with OkHandler {}

  def apply[A](f: FullResponse => A): FullOkHandler[A] = new FullOkHandler[A](f) {}
  def stream[A](f: FullResponse => Future[A]): StreamOkHandler[A] = new StreamOkHandler[A](f) {}
}
