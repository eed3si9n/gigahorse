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

import akka.actor._
import akka.stream.actor._
import akka.http.scaladsl.model.ws.Message

object MessageForwarder {
  def props : Props = Props[MessageForwarder]
}

class MessageForwarder extends Actor with ActorPublisher[Message] {
  var items:List[Message] = List.empty
  import ActorPublisherMessage._
  def receive = {
    case m: Message =>
      if (totalDemand == 0) items = items :+ m
      else onNext(m)
    case Request(demand) =>
      if (demand > items.size){
        items foreach (onNext)
        items = List.empty
      }
      else {
        val (send, keep) = items.splitAt(demand.toInt)
        items = keep
        send foreach (onNext)
      }
  }
}
