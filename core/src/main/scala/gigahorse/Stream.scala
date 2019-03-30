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

import org.reactivestreams.Publisher
import scala.concurrent.Future

abstract class Stream[A] {
  /**
   * @return The underlying Stream object.
   */
  def underlying[B]

  def toPublisher: Publisher[A]

  /** Runs f on each element received to the stream. */
  def foreach(f: A => Unit): Future[Unit]

  /** Runs f on each element received to the stream with its previous output. */
  def fold[B](zero: B)(f: (B, A) => B): Future[B]

  /** Runs f on each element received to the stream with its previous output and close resource */
  def foldResource[B](zero: B)(f: (B, A) => B, close: () => Unit): Future[B]

  /** Similar to fold but uses first element as zero element. */
  def reduce(f: (A, A) => A): Future[A]
}
