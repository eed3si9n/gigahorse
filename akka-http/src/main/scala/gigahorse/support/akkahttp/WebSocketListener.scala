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

import akka.{ Done, NotUsed }
import akka.util.ByteString
import akka.stream.scaladsl._
import scala.util.Success
import scala.concurrent.{ Future, Promise, ExecutionContext }
import akka.actor.{ ActorRef, ActorSystem, Props, PoisonPill }
import akka.stream.actor.ActorPublisher
import akka.http.scaladsl.model.ws.{ Message, TextMessage => XTextMessage, BinaryMessage => XBinaryMessage }
import WebSocketEvent._

// http://doc.akka.io/api/akka-http/current/akka/index.html
// http://doc.akka.io/api/akka/2.4.16/

class WebSocketListener(
  handler: PartialFunction[WebSocketEvent, Unit],
  system: ActorSystem) { self =>
  protected var ws: WebSocket = null
  protected var open: Boolean = true
  val result = Promise[WebSocket]()
  val forwarder = system.actorOf(Props[MessageForwarder])
  val publisher = ActorPublisher[Message](forwarder)
  val source: Source[Message, NotUsed] = Source.fromPublisher(publisher)
  val sink: Sink[Message, Future[Done]] =
    Sink.foreach {
      case message: XTextMessage.Strict =>
        broadcast(TextMessage(ws, message.text))
      case message: XBinaryMessage.Strict =>
        broadcast(BinaryMessage(ws, message.data.toArray))
      case _ =>
    }
  ws = new WebSocket {
    def underlying[A]: A = self.asInstanceOf[A]
      override def isOpen: Boolean = open
      override def sendMessage(message: Array[Byte]): WebSocket = {
        forwarder ! XBinaryMessage(ByteString(message))
        this
      }
      override def sendMessage(message: String): WebSocket = {
        forwarder ! XTextMessage(message)
        this
      }
      override def close(): Unit =
        {
          forwarder ! PoisonPill
          open = false
        }
      override def sendPing(payload: Array[Byte]): WebSocket = ???
      override def sendPong(payload: Array[Byte]): WebSocket = ???
      override def sendFragment(fragment: Array[Byte], last: Boolean): WebSocket = ???
      override def sendFragment(fragment: String, last: Boolean): WebSocket = ???
  }
  result.tryComplete(Success(ws))
  private def broadcast(event: WebSocketEvent): Unit =
    if (handler.isDefinedAt(event)) handler(event)
    else ()
}
