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
package support.asynchttpclient

import org.reactivestreams.{ Publisher, Subscriber, Subscription }
import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.{ Future, Promise }

class AhcStream[A](publisher: Publisher[A]) extends Stream[A] {
  /**
   * @return The underlying Stream object.
   */
  def underlying[A] = publisher.asInstanceOf[A]

  /** Runs f on each element received to the stream. */
  def foreach(f: A => Unit): Future[Unit] =
    {
      val subscriber = new ForEachSubscriber(f)
      publisher.subscribe(subscriber)
      subscriber.value
    }

  /** Runs f on each element received to the stream with its previous output. */
  def fold[B](zero: B)(f: (B, A) => B): Future[B] =
    foldResource(zero)(f, () => ())

  /** Runs f on each element received to the stream with its previous output and does closing operation . */
  def foldResource[B](zero: B)(f: (B, A) => B, close: () => Unit): Future[B] =
    {
      val subscriber = new FoldSubscriber[A, B](zero, f, close)
      publisher.subscribe(subscriber)
      subscriber.value
    }

  /** Runs f on each element received to the stream with its previous output. */
  def reduce(f: (A, A) => A): Future[A] =
    {
      val subscriber = new ReduceSubscriber[A](f)
      publisher.subscribe(subscriber)
      subscriber.value
    }

  def toPublisher: Publisher[A] = publisher
}

class ForEachSubscriber[A](f: A => Unit) extends Subscriber[A] {
  val subscription = new AtomicReference[Subscription]
  val result = Promise[Unit]()

  def onComplete(): Unit =
    {
      result.success(())
    }
  def onError(e: Throwable): Unit =
    {
      result.failure(e)
    }
  def onNext(a: A): Unit =
    {
      f(a)
      subscription.get.request(1)
    }
  def onSubscribe(x: Subscription): Unit =
    {
      subscription.set(x)
      x.request(1)
    }
  def value: Future[Unit] = result.future
}

class FoldSubscriber[A, B](zero: B, f: (B, A) => B, close: () => Unit) extends Subscriber[A] {
  val subscription = new AtomicReference[Subscription]
  val result = Promise[B]()
  private var holder: B = zero

  def onComplete(): Unit =
    {
      close()
      result.success(holder)
    }
  def onError(e: Throwable): Unit =
    {
      close()
      result.failure(e)
    }
  def onNext(a: A): Unit =
    {
      holder = f(holder, a)
      subscription.get.request(1)
    }
  def onSubscribe(x: Subscription): Unit =
    {
      subscription.set(x)
      x.request(1)
    }
  def value: Future[B] = result.future
}

class ReduceSubscriber[A](f: (A, A) => A) extends Subscriber[A] {
  val subscription = new AtomicReference[Subscription]
  val result = Promise[A]()
  private var holder: Option[A] = None

  def onComplete(): Unit =
    holder match {
      case None    => result.failure(new IllegalStateException("Stream completed without an element"))
      case Some(x) => result.success(x)
    }

  def onError(e: Throwable): Unit =
    {
      result.failure(e)
    }
  def onNext(a: A): Unit =
    holder match {
      case None    =>
        holder = Some(a)
        subscription.get.request(1)
      case Some(x) =>
        holder = Some(f(x, a) )
        subscription.get.request(1)
    }
  def onSubscribe(x: Subscription): Unit =
    {
      subscription.set(x)
      x.request(2)
    }
  def value: Future[A] = result.future
}
