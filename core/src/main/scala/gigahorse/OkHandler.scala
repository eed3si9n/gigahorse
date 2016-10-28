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

import org.asynchttpclient._

abstract class OkHandler[A](f: Response => A) extends FunctionHandler[A](f) {
  override def onStatusReceived(status: HttpResponseStatus): State = {
    val code = status.getStatusCode
    if (code / 100 == 2) super.onStatusReceived(status)
    else throw StatusError(code)
  }
}

object OkHandler {
  def apply[A](f: Response => A): OkHandler[A] = new OkHandler[A](f) {}
}

final class StatusError(val status: Int) extends RuntimeException("Unexpected status: " + status.toString) {
  override def equals(o: Any): Boolean = o match {
    case x: StatusError => (this.status == x.status)
    case _ => false
  }
  override def hashCode: Int = {
    (17 + status.##)
  }
}
object StatusError {
  def apply(status: Int): StatusError = new StatusError(status)
}
