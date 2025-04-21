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

import shaded.ahc.org.asynchttpclient.ws.{
  WebSocket as XWebSocket,
  WebSocketListener as XWebSocketListener
}

import scala.concurrent.Promise
import scala.util.{ Failure, Success }

class WebSocketListener(handler: PartialFunction[WebSocketEvent, Unit], result: Promise[WebSocket])
    extends XWebSocketListener {
  import WebSocketEvent.*

  protected var ws: WebSocket = null

  private def broadcast(event: WebSocketEvent): Unit =
    if (handler.isDefinedAt(event)) handler(event)

  override def onBinaryFrame(message: Array[Byte], finalFragment: Boolean, rsv: Int): Unit =
    broadcast(BinaryMessage(ws, message))

  override def onPingFrame(message: Array[Byte]): Unit =
    broadcast(Ping(ws, message))

  override def onPongFrame(message: Array[Byte]): Unit =
    broadcast(Pong(ws, message))

  override def onTextFrame(message: String, finalFragment: Boolean, rsv: Int): Unit =
    broadcast(TextMessage(ws, message))

  override def onOpen(websocket: XWebSocket): Unit = {
    ws = new WebSocket {
      private val ahcWebSocket = websocket
      def underlying[A]: A = ahcWebSocket.asInstanceOf[A]

      override def sendPing(payload: Array[Byte]): WebSocket = {
        websocket.sendPingFrame(payload)
        this
      }

      override def sendPong(payload: Array[Byte]): WebSocket = {
        websocket.sendPongFrame(payload)
        this
      }

      override def isOpen: Boolean = websocket.isOpen

      override def sendMessage(message: Array[Byte]): WebSocket = {
        websocket.sendBinaryFrame(message)
        this
      }

      override def sendMessage(message: String): WebSocket = {
        websocket.sendTextFrame(message)
        this
      }

      override def sendFragment(fragment: Array[Byte], last: Boolean): WebSocket = {
        websocket.sendBinaryFrame(fragment, last, 0)
        this
      }

      override def sendFragment(fragment: String, last: Boolean): WebSocket = {
        websocket.sendTextFrame(fragment, last, 0)
        this
      }

      override def close(): Unit = websocket match {
        case x: AutoCloseable => x.close()
        case _                => ()
      }
    }
    broadcast(Open(ws))
    result.tryComplete(Success(ws))
  }

  override def onClose(websocket: XWebSocket, code: Int, reason: String): Unit = {
    broadcast(Close(ws))
    ws = null
  }

  override def onError(t: Throwable): Unit = {
    broadcast(Error(Option(ws), t))
    result.tryComplete(Failure(t))
  }
}
