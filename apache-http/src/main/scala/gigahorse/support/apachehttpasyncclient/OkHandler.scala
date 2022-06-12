/*
 * Copyright 2022 by Eugene Yokota
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
package support.apachehttpasyncclient

import java.io.File
import shaded.apache.org.apache.http.entity.ContentType

trait OkHandler extends ApacheHandler {
  abstract override def onStatusReceived(code: Int): Unit =
    {
      if (code / 100 == 2) super.onStatusReceived(code)
      else throw StatusError(code)
    }
}

object OkHandler {
  abstract class FullOkHandler[A](f: FullResponse => A) extends FunctionHandler[A](f) with OkHandler {}
  abstract class ZeroCopyOkHandler(f: (File, ContentType) => Unit) extends ApacheZeroCopyHandler with OkHandler {
    override def onFileReceived(file: File, contentType: ContentType): Unit = f(file, contentType)
  }
  def apply[A](f: FullResponse => A): FullOkHandler[A] = new FullOkHandler[A](f) {}
  def zeroCopy(f: (File, ContentType) => Unit): ZeroCopyOkHandler = new ZeroCopyOkHandler(f) {}
}
