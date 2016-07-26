/**
 * This code is generated using sbt-datatype.
 */

// DO NOT EDIT MANUALLY
package gigahorse
final class Request(
  /** The base URL for this request */
  val url: String,
  /** The method for this request. */
  val method: String,
  /** The body of this request */
  val body: Body,
  /** The headers for this request */
  val headers: Map[String, List[String]],
  /** The query string for this request */
  val queryString: Map[String, List[String]],
  val signatureOpt: Option[SignatureCalculator],
  val authOpt: Option[Realm],
  /** Whether this request should follow redirects. (Default: `None`) */
  val followRedirectsOpt: Option[Boolean],
  /** The timeout for the request. (Default: `None`) */
  val requestTimeoutOpt: Option[scala.concurrent.duration.Duration],
  /** The virtual host this request will use. (Default: `None`) */
  val virtualHostOpt: Option[String],
  /** The proxy server this request will use. (Default: `None`) */
  val proxyServerOpt: Option[ProxyServer]) extends Serializable {
  import java.io.File
  import java.nio.charset.Charset
  import AhcHttpClient.{ utf8, setBodyString, setBody }
  /** Uses GET method. */
  def get: Request = this.withMethod("GET")
  /** Uses PATCH method with the given body. */
  def patch[A: HttpWrite](body: A): Request = setBody(this.withMethod("PATCH"), body)
  /** Uses PATCH method with the given body. */
  def patch(body: String, charset: Charset): Request = setBodyString(this.withMethod("PATCH"), body, charset)
  /** Uses PATCH method with the given file. */
  def patch(file: File): Request = this.withMethod("PATCH").withBody(FileBody(file))
  /** Uses POST method with the given body. */
  def post[A: HttpWrite](body: A): Request = setBody(this.withMethod("POST"), body)
  /** Uses POST method with the given body. */
  def post(body: String, charset: Charset): Request = setBodyString(this.withMethod("POST"), body, charset)
  /** Uses POST method with the given file. */
  def post(file: File): Request = this.withMethod("POST").withBody(FileBody(file))
  /** Uses PUT method with the given body. */
  def put[A: HttpWrite](body: A): Request = setBody(this.withMethod("PUT"), body)
  /** Uses PUT method with the given body. */
  def put(body: String, charset: Charset): Request = setBodyString(this.withMethod("PUT"), body, charset)
  /** Uses PUT method with the given file. */
  def put(file: File): Request = this.withMethod("PUT").withBody(FileBody(file))
  /** Uses DELETE method. */
  def delete: Request = this.withMethod("DELETE")
  /** Uses HEAD method. */
  def head: Request = this.withMethod("HEAD")
  /** Uses OPTIONS method. */
  def options: Request = this.withMethod("OPTIONS")
  def this(url: String) = this(url, "GET", EmptyBody(), Map(), Map(), None, None, None, None, None, None)
  
  override def equals(o: Any): Boolean = o match {
    case x: Request => (this.url == x.url) && (this.method == x.method) && (this.body == x.body) && (this.headers == x.headers) && (this.queryString == x.queryString) && (this.signatureOpt == x.signatureOpt) && (this.authOpt == x.authOpt) && (this.followRedirectsOpt == x.followRedirectsOpt) && (this.requestTimeoutOpt == x.requestTimeoutOpt) && (this.virtualHostOpt == x.virtualHostOpt) && (this.proxyServerOpt == x.proxyServerOpt)
    case _ => false
  }
  override def hashCode: Int = {
    37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (17 + url.##) + method.##) + body.##) + headers.##) + queryString.##) + signatureOpt.##) + authOpt.##) + followRedirectsOpt.##) + requestTimeoutOpt.##) + virtualHostOpt.##) + proxyServerOpt.##)
  }
  override def toString: String = {
    "Request(" + url + ", " + method + ", " + body + ", " + headers + ", " + queryString + ", " + signatureOpt + ", " + authOpt + ", " + followRedirectsOpt + ", " + requestTimeoutOpt + ", " + virtualHostOpt + ", " + proxyServerOpt + ")"
  }
  private[this] def copy(url: String = url, method: String = method, body: Body = body, headers: Map[String, List[String]] = headers, queryString: Map[String, List[String]] = queryString, signatureOpt: Option[SignatureCalculator] = signatureOpt, authOpt: Option[Realm] = authOpt, followRedirectsOpt: Option[Boolean] = followRedirectsOpt, requestTimeoutOpt: Option[scala.concurrent.duration.Duration] = requestTimeoutOpt, virtualHostOpt: Option[String] = virtualHostOpt, proxyServerOpt: Option[ProxyServer] = proxyServerOpt): Request = {
    new Request(url, method, body, headers, queryString, signatureOpt, authOpt, followRedirectsOpt, requestTimeoutOpt, virtualHostOpt, proxyServerOpt)
  }
  def withUrl(url: String): Request = {
    copy(url = url)
  }
  def withMethod(method: String): Request = {
    copy(method = method)
  }
  def withBody(body: Body): Request = {
    copy(body = body)
  }
  def withHeaders(headers: Map[String, List[String]]): Request = {
    copy(headers = headers)
  }
  def withQueryString(queryString: Map[String, List[String]]): Request = {
    copy(queryString = queryString)
  }
  def withSignatureOpt(signatureOpt: Option[SignatureCalculator]): Request = {
    copy(signatureOpt = signatureOpt)
  }
  def withAuthOpt(authOpt: Option[Realm]): Request = {
    copy(authOpt = authOpt)
  }
  def withFollowRedirectsOpt(followRedirectsOpt: Option[Boolean]): Request = {
    copy(followRedirectsOpt = followRedirectsOpt)
  }
  def withRequestTimeoutOpt(requestTimeoutOpt: Option[scala.concurrent.duration.Duration]): Request = {
    copy(requestTimeoutOpt = requestTimeoutOpt)
  }
  def withVirtualHostOpt(virtualHostOpt: Option[String]): Request = {
    copy(virtualHostOpt = virtualHostOpt)
  }
  def withProxyServerOpt(proxyServerOpt: Option[ProxyServer]): Request = {
    copy(proxyServerOpt = proxyServerOpt)
  }
}
object Request {
  def apply(url: String): Request = new Request(url, "GET", EmptyBody(), Map(), Map(), None, None, None, None, None, None)
  def apply(url: String, method: String, body: Body, headers: Map[String, List[String]], queryString: Map[String, List[String]], signatureOpt: Option[SignatureCalculator], authOpt: Option[Realm], followRedirectsOpt: Option[Boolean], requestTimeoutOpt: Option[scala.concurrent.duration.Duration], virtualHostOpt: Option[String], proxyServerOpt: Option[ProxyServer]): Request = new Request(url, method, body, headers, queryString, signatureOpt, authOpt, followRedirectsOpt, requestTimeoutOpt, virtualHostOpt, proxyServerOpt)
}
