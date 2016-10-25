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

import java.io.Closeable

abstract class WebSocket extends Closeable {

  def underlying[A]: A

  /** Return <code>true</code> if the WebSocket is open/connected. */
  def isOpen: Boolean

  /** Send a byte message. */
  def sendMessage(message: Array[Byte]): WebSocket

  /** Send a binary fragment. */
  def sendFragment(fragment: Array[Byte], last: Boolean): WebSocket

  /** Send a text message. */
  def sendMessage(message: String): WebSocket

  /** Send a message fragment. */
  def sendFragment(fragment: String, last: Boolean): WebSocket

  /** Send a <code>ping</code> with an optional payload (limited to 125 bytes or less). */
  def sendPing(payload: Array[Byte]): WebSocket

  /** Send a <code>ping</code> with an optional payload (limited to 125 bytes or less). */
  def sendPong(payload: Array[Byte]): WebSocket
}
