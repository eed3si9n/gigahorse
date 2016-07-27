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

/**
 * Lifts Future[A] into Future[Either[Throwable, A]]
 */
final class FutureLifter[A](f: Response => A) {
  def run(value: Future[Response])(implicit ec: ExecutionContext): Future[Either[Throwable, A]] =
    value map { r => Right[Throwable, A](f(r)) } recoverWith { case e =>
      Future.successful(Left[Throwable, A](e)) }

  def map[B](g: A => B): FutureLifter[B] = new FutureLifter[B](x => g(f(x)))
  override def toString: String = "FutureLifter(function)"
}

object FutureLifter {
  def asEither: FutureLifter[Response] = new FutureLifter[Response](identity)
}
