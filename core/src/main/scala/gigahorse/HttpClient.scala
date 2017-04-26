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

import scala.concurrent.{ Future, ExecutionContext }
import java.io.File

abstract class HttpClient extends AutoCloseable {
  def underlying[A]: A

  /** Closes this client, and releases underlying resources. */
  def close(): Unit

  /** Runs the request and return a Future of FullResponse. Errors on non-OK response. */
  def run(request: Request): Future[FullResponse]

  /** Runs the request and return a Future of A. Errors on non-OK response. */
  def run[A](request: Request, f: FullResponse => A): Future[A]

  /** Runs the request and return a Future of Either a FullResponse or a Throwable. Errors on non-OK response. */
  def run[A](request: Request, lifter: FutureLifter[A])(implicit ec: ExecutionContext): Future[Either[Throwable, A]]

  /** Downloads the request to the file. Errors on non-OK response. */
  def download(request: Request, file: File): Future[File]

  /** Executes the request and return a Future of FullResponse. Does not error on non-OK response. */
  def processFull(request: Request): Future[FullResponse]

  /** Executes the request and return a Future of A. Does not error on non-OK response. */
  def processFull[A](request: Request, f: FullResponse => A): Future[A]

  /** Executes the request and return a Future of Either a FullResponse or a Throwable. Does not error on non-OK response. */
  def processFull[A](request: Request, lifter: FutureLifter[A])(implicit ec: ExecutionContext): Future[Either[Throwable, A]]

  /** Open a websocket connection. */
  def websocket(request: Request)(handler: PartialFunction[WebSocketEvent, Unit]): Future[WebSocket]
}
