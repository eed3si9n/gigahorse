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
package support.okhttp

import okhttp3.{ Response => XResponse }
import okhttp3.{ WebSocket => XWebSocket , WebSocketListener => XWebSocketListener }
import scala.concurrent.Promise
import okio.ByteString
import scala.util.{ Failure, Success }

class OkhWebSocketListener(handler: PartialFunction[WebSocketEvent, Unit],
                           result: Promise[WebSocket]) extends XWebSocketListener {
  import WebSocketEvent._
  protected var ws: Option[WebSocket] = None

  private def broadcast(event: WebSocketEvent): Unit =
    if (handler.isDefinedAt(event)) {
      handler(event)
    }

  /**
   * Invoked when a web socket has been accepted by the remote peer and may begin transmitting
   * messages.
   */
  override def onOpen(websocket: XWebSocket, resposne: XResponse): Unit = {
    val x = new WebSocket {
      private val okhWebSocket = websocket
      def underlying[A]: A = okhWebSocket.asInstanceOf[A]
      override def isOpen: Boolean = ws.isDefined
      override def close(): Unit = {
        websocket.close(1000, null)
      }

      /**
       * Attempts to enqueue `text` to be UTF-8 encoded and sent as a the data of a text (type
       * `0x1`) message.
       */
      override def sendMessage(message: String): WebSocket = {
        websocket.send(message)
        this
      }

      /**
       * Attempts to enqueue `bytes` to be sent as a the data of a binary (type `0x2`)
       * message.
       */
      override def sendMessage(message: Array[Byte]): WebSocket = {
        websocket.send(ByteString.of(message, 0, message.size))
        this
      }

      override def sendFragment(fragment: Array[Byte], last: Boolean): WebSocket = ???
      override def sendFragment(fragment: String, last: Boolean): WebSocket = ???
      override def sendPing(payload: Array[Byte]): WebSocket = ???
      override def sendPong(payload: Array[Byte]): WebSocket = ???
    }
    ws = Option(x)
    broadcast(Open(ws.get))
    result.tryComplete(Success(ws.get))
  }

  /** Invoked when a text (type {@code 0x1}) message has been received. */
  override def onMessage(websocket: XWebSocket, text: String): Unit =
    broadcast(TextMessage(ws.get, text))

  /** Invoked when a binary (type {@code 0x2}) message has been received. */
  override def onMessage(websocket: XWebSocket, bytes: ByteString): Unit =
    broadcast(BinaryMessage(ws.get, bytes.toByteArray))

  /** Invoked when the peer has indicated that no more incoming messages will be transmitted. */
  override def onClosing(websocket: XWebSocket, code: Int, reason: String): Unit = {
  }

  /**
   * Invoked when both peers have indicated that no more messages will be transmitted and the
   * connection has been successfully released. No further calls to this listener will be made.
   */
  override def onClosed(websocket: XWebSocket, code: Int, reason: String): Unit = {
    broadcast(Close(ws.get))
    ws = None
  }

  /**
   * Invoked when a web socket has been closed due to an error reading from or writing to the
   * network. Both outgoing and incoming messages may have been lost. No further calls to this
   * listener will be made.
   */
  override def onFailure(websocket: XWebSocket, t: Throwable, response: XResponse): Unit = {
    broadcast(Error(ws, t))
    result.tryComplete(Failure(t))
  }
}
