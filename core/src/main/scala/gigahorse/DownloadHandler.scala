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

import java.io.{ File, FileOutputStream }
import scala.concurrent.Future

object DownloadHandler {
  /** Function from `StreamResponse` to `Future[File]` */
  def asFile(file: File): StreamResponse => Future[File] = (response: StreamResponse) =>
    {
      val stream = response.byteBuffers
      val out = new FileOutputStream(file).getChannel
      stream.fold(file)((acc, bb) => {
        out.write(bb)
        acc
      })
    }
}
