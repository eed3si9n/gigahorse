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

import gigahorse.FileUtil
import java.io.ByteArrayOutputStream
import unfiltered.netty.cycle
import unfiltered.request.*
import unfiltered.response.*
import unfiltered.netty.request.*

object TestPlan {
  val Fail = Unauthorized ~> WWWAuthenticate("""Basic realm="/"""")

  def testPlan = cycle.Planify {
    case GET(Path(Seg("500" :: Nil))) =>
      InternalServerError ~> ResponseString("500 HTTP Status Code")
    case GET(Path(Seg("404" :: Nil))) =>
      NotFound ~> ResponseString("404 HTTP Status Code")
    // test basic auth
    case r @ GET(Path(Seg("auth" :: Nil))) =>
      r match {
        case BasicAuth(u, p) if (verify(u, p)) =>
          Ok ~> ResponseString("auth ok")
        case _ => Fail
      }
    // form
    case POST(Path("/form")) & Params(params) =>
      params.get("arg1") match {
        case Some(Seq(x)) => Ok ~> ResponseString(x)
        case _            => BadRequest ~> ResponseString("args1 is not found!")
      }
    case r @ POST(Path("/bearer")) =>
      val authz = r.headers("Authorization")
      r match {
        case r if authz.nonEmpty && authz.next == "Bearer token123" =>
          Ok ~> ResponseString(r.headers("Authorization").next())
        case _ => Fail
      }
    case r @ POST(Path("/charset")) =>
      val h = r.headers("Content-Type").filter(_.contains("text/plain"))
      if (h.hasNext)
        Ok ~> ResponseString(h.next().replaceAll("\\s", ""))
      else
        BadRequest ~> ResponseString("Content-Type header 'text/plain' not found")
    // sign
    case r @ GET(Path("/sign")) =>
      val h = r.headers("X-Signature")
      if (h.hasNext)
        Ok ~> ResponseString(s"${h.next()}:${r.parameterValues("query").mkString}")
      else
        BadRequest ~> ResponseString("X-Signature header is not found!")
    case r @ POST(Path("/sign")) =>
      val h = r.headers("X-Signature")
      if (h.hasNext)
        Ok ~> ResponseString(
          s"${h.next()}:${r.parameterValues("query").mkString}:${r.parameterValues("content").mkString}"
        )
      else
        BadRequest ~> ResponseString("X-Signature header is not found!")
    // download
    case GET(Path("/download")) =>
      val r = this.getClass().getClassLoader().getResourceAsStream("a.json")
      val baos = new ByteArrayOutputStream()
      FileUtil.transfer(r, baos)
      Ok ~> ResponseBytes(baos.toByteArray())
    // upload
    case r @ POST(Path("/upload")) =>
      val body = FileUtil.read(r.inputStream)
      Ok ~> ResponseString(body)
    case POST(Path("/upload-as-multipart") & MultiPart(r)) =>
      val mem = MultiPartParams.Memory(r)
      val f = mem.files("a.json").head
      val body = f.stream(FileUtil.read)
      Ok ~> ResponseString(body)
    // multipart form
    case POST(Path("/multipart") & MultiPart(r)) =>
      val mem = MultiPartParams.Memory(r)
      val f = mem.files("a.json").head
      val baos = new ByteArrayOutputStream()
      f.stream(FileUtil.transfer(_, baos))
      Ok ~> ResponseBytes(baos.toByteArray())
    case GET(Path(p)) =>
      println(p)
      Ok ~> ResponseString("fallthrough")
  }

  def verify(login: String, password: String): Boolean = login == "admin"
}
