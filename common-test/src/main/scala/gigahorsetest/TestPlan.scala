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

package gigahorsetest

import unfiltered.request._
import unfiltered.response._
import unfiltered.filter.Plan

class TestPlan extends Plan {
  val Fail = Unauthorized ~> WWWAuthenticate("""Basic realm="/"""")

  def intent = {
    case GET(Path(Seg("500" :: Nil))) =>
      InternalServerError ~> ResponseString("500 HTTP Status Code")
    case GET(Path(Seg("404" :: Nil))) =>
      NotFound ~> ResponseString("404 HTTP Status Code")
    // test basic auth
    case r @ GET(Path(Seg("auth" :: Nil))) =>
      r match {
        case BasicAuth(u, p) if(verify(u, p)) =>
          Ok ~> ResponseString("auth ok")
        case _ => Fail
      }
    // form
    case POST(Path("/form")) & Params(params) =>
      params.get("arg1") match {
        case Some(Seq(x)) => Ok ~> ResponseString(x)
        case _            => BadRequest ~> ResponseString("args1 is not found!")
      }
    case GET(Path(p)) =>
      println(p)
      Ok ~> ResponseString("foo")
  }

  def verify(login: String, password: String): Boolean = login == "admin"
}

