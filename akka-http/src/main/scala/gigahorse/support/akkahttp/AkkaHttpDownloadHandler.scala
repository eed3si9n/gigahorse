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

import java.io.File
import scala.concurrent.{ Future, ExecutionContext }
import akka.stream.Materializer
import akka.http.scaladsl.model.HttpResponse
import akka.stream.scaladsl.FileIO

abstract class AkkaHttpDownloadHandler(file: File) extends OkHandler[File](_ => file) {
  // This will not be called
  override def onCompleted(response: FullResponse): File = sys.error("Unexpected call to onCompleted")
  override def onPartialResponse(httpResponse: HttpResponse, config: Config)(implicit fm: Materializer, ec: ExecutionContext): Future[File] =
    {
      val source = httpResponse.entity.dataBytes
      for {
        _ <- source.runWith(FileIO.toPath(file.toPath))
      } yield file
    }
}

object AkkaHttpDownloadHandler {
  def apply(file: File): AkkaHttpDownloadHandler =
    new AkkaHttpDownloadHandler(file) {}
}
