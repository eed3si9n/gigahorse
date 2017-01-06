/*
 * Copyright 2016 by Alex Dupre
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

import org.asynchttpclient.ws.{ WebSocketByteListener, WebSocketPingListener, WebSocketPongListener, WebSocketTextListener, WebSocket => XWebSocket, WebSocketListener => XWebSocketListener }

import scala.concurrent.Promise
import scala.util.{ Failure, Success }

class WebSocketListener(handler: PartialFunction[WebSocketEvent, Unit], result: Promise[WebSocket]) extends XWebSocketListener with WebSocketByteListener with WebSocketTextListener with WebSocketPingListener with WebSocketPongListener {
  import WebSocketEvent._

  protected var ws: WebSocket = null

  private def broadcast(event: WebSocketEvent): Unit =
    if (handler.isDefinedAt(event)) handler(event)

  override def onMessage(message: Array[Byte]): Unit =
    broadcast(BinaryMessage(ws, message))

  override def onPing(message: Array[Byte]): Unit =
    broadcast(Ping(ws, message))

  override def onPong(message: Array[Byte]): Unit =
    broadcast(Pong(ws, message))

  override def onMessage(message: String): Unit =
    broadcast(TextMessage(ws, message))

  override def onOpen(websocket: XWebSocket): Unit = {
    ws = new WebSocket {
      private val ahcWebSocket = websocket
      def underlying[A]: A = ahcWebSocket.asInstanceOf[A]

      override def sendPing(payload: Array[Byte]): WebSocket = {
        websocket.sendPing(payload)
        this
      }

      override def sendPong(payload: Array[Byte]): WebSocket = {
        websocket.sendPong(payload)
        this
      }

      override def isOpen: Boolean = websocket.isOpen

      override def sendMessage(message: Array[Byte]): WebSocket = {
        websocket.sendMessage(message)
        this
      }

      override def sendMessage(message: String): WebSocket = {
        websocket.sendMessage(message)
        this
      }

      override def sendFragment(fragment: Array[Byte], last: Boolean): WebSocket = {
        websocket.stream(fragment, last)
        this
      }

      override def sendFragment(fragment: String, last: Boolean): WebSocket = {
        websocket.stream(fragment, last)
        this
      }

      override def close(): Unit = websocket.close()
    }
    broadcast(Open(ws))
    result.tryComplete(Success(ws))
  }

  override def onClose(websocket: XWebSocket): Unit = {
    broadcast(Close(ws))
    ws = null
  }

  override def onError(t: Throwable): Unit = {
    broadcast(Error(Option(ws), t))
    result.tryComplete(Failure(t))
  }
}
