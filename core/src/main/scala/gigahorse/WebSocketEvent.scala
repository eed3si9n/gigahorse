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

sealed trait WebSocketEvent

object WebSocketEvent {

  case class Open(ws: WebSocket) extends WebSocketEvent

  case class Ping(ws: WebSocket, payload: Array[Byte]) extends WebSocketEvent

  case class Pong(ws: WebSocket, payload: Array[Byte]) extends WebSocketEvent

  case class TextMessage(ws: WebSocket, message: String) extends WebSocketEvent

  case class BinaryMessage(ws: WebSocket, message: Array[Byte]) extends WebSocketEvent

  case class Close(ws: WebSocket) extends WebSocketEvent

  case class Error(ws: Option[WebSocket], throwable: Throwable) extends WebSocketEvent

}