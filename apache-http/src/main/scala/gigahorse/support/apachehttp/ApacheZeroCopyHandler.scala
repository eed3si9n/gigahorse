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
package support.apachehttp

import java.io.File
import shaded.apache.org.apache.http.Header
import shaded.apache.org.apache.http.entity.ContentType

/**
 * Wrapper around org.apache.http.nio.client.methods.ZeroCopyConsumer
 * so we can perform status code check using OkHandler.
 * See OkHanlder.zeroCopy.
 */
abstract class ApacheZeroCopyHandler extends ApacheHandler {
  def onStatusReceived(status: Int): Unit = ()
  def onHeadersReceived(headers: List[Header]): Unit = ()
  def onFileReceived(file: File, contentType: ContentType): Unit
}