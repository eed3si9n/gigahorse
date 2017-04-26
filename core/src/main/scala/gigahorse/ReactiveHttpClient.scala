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

import scala.concurrent.Future

abstract class ReactiveHttpClient extends HttpClient {
  /** Runs the request and return a Future of FullResponse. */
  def runStream(request: Request): Future[StreamResponse]

  /** Runs the request and return a Future of A. */
  def runStream[A](request: Request, f: StreamResponse => Future[A]): Future[A]

  /** Executes the request and return a Future of StreamResponse. Does not error on non-OK response. */
  def processStream(request: Request): Future[StreamResponse]

  /** Executes the request and return a Future of A. Does not error on non-OK response. */
  def processStream[A](request: Request, f: StreamResponse => Future[A]): Future[A]
}
