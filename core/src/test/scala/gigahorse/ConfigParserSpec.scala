/*
 * Copyright 2026 by Eugene Yokota
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

import com.typesafe.config.ConfigFactory
import org.scalatest.funsuite.AnyFunSuite

class ConfigParserSpec extends AnyFunSuite {
  private def parse(s: String): Config =
    ConfigParser.parse(ConfigFactory.parseString(s))

  test("httpVersionPolicy should default to Negotiate") {
    assert(parse("gigahorse {}").httpVersionPolicy == HttpVersionPolicy.Negotiate)
  }

  test("httpVersionPolicy should accept the HTTP/1.1 spellings") {
    val expected = HttpVersionPolicy.Http1_1
    assert(parse("""gigahorse { httpVersionPolicy = "http1_1" }""").httpVersionPolicy == expected)
    assert(parse("""gigahorse { httpVersionPolicy = "http1.1" }""").httpVersionPolicy == expected)
    assert(parse("""gigahorse { httpVersionPolicy = "HTTP/1.1" }""").httpVersionPolicy == expected)
  }

  test("httpVersionPolicy should accept the HTTP/2 spellings") {
    val expected = HttpVersionPolicy.Http2
    assert(parse("""gigahorse { httpVersionPolicy = "http2" }""").httpVersionPolicy == expected)
    assert(parse("""gigahorse { httpVersionPolicy = "HTTP/2" }""").httpVersionPolicy == expected)
  }

  test("httpVersionPolicy should reject an unknown value") {
    assertThrows[RuntimeException](
      parse("""gigahorse { httpVersionPolicy = "http3" }""")
    )
  }
}
