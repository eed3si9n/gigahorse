/**
 * This code is generated using [[https://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package gigahorse
/**
 * @param url The base URL for this request
 * @param method The method for this request.
 * @param body The body of this request
 * @param headers The headers for this request
 * @param queryString The query string for this request
 * @param followRedirectsOpt Whether this request should follow redirects. (Default: `None`)
 * @param requestTimeoutOpt The timeout for the request. (Default: `None`)
 * @param virtualHostOpt The virtual host this request will use. (Default: `None`)
 * @param proxyServerOpt The proxy server this request will use. (Default: `None`)
 */
final class Request private (
  val url: String,
  val method: String,
  val body: Body,
  val headers: Map[String, List[String]],
  val queryString: Map[String, List[String]],
  val signatureOpt: Option[SignatureCalculator],
  val authOpt: Option[Realm],
  val followRedirectsOpt: Option[Boolean],
  val requestTimeoutOpt: Option[scala.concurrent.duration.Duration],
  val virtualHostOpt: Option[String],
  val proxyServerOpt: Option[ProxyServer]) extends Serializable {
  import java.io.File
  import java.nio.charset.Charset
  /** Uses GET method. */
  def get: Request                                   = this.withMethod(HttpVerbs.GET)
  /** Uses PATCH method with the given body. */
  def patch[A: HttpWrite](body: A): Request          = this.withMethod(HttpVerbs.PATCH).withBody(body)
  /** Uses PATCH method with the given body. */
  def patch(body: String, charset: Charset): Request = this.withMethod(HttpVerbs.PATCH).withBody(EncodedString(body, charset))
  /** Uses PATCH method with the given file. */
  def patch(file: File): Request                     = this.withMethod(HttpVerbs.PATCH).withBody(FileBody(file))
  /** Uses POST method with the given body. */
  def post[A: HttpWrite](body: A): Request           = this.withMethod(HttpVerbs.POST).withBody(body)
  /** Uses POST method with the given body. */
  def post(body: String, charset: Charset): Request  = this.withMethod(HttpVerbs.POST).withBody(EncodedString(body, charset))
  /** Uses POST method with the given file. */
  def post(file: File): Request                      = this.withMethod(HttpVerbs.POST).withBody(FileBody(file))
  /** Uses PUT method with the given body. */
  def put[A: HttpWrite](body: A): Request            = this.withMethod(HttpVerbs.PUT).withBody(body)
  /** Uses PUT method with the given body. */
  def put(body: String, charset: Charset): Request   = this.withMethod(HttpVerbs.PUT).withBody(EncodedString(body, charset))
  /** Uses PUT method with the given file. */
  def put(file: File): Request                       = this.withMethod(HttpVerbs.PUT).withBody(FileBody(file))
  /** Uses DELETE method. */
  def delete: Request                                = this.withMethod(HttpVerbs.DELETE)
  /** Uses HEAD method. */
  def head: Request                                  = this.withMethod(HttpVerbs.HEAD)
  /** Uses OPTIONS method. */
  def options: Request                               = this.withMethod(HttpVerbs.OPTIONS)
  def withBody[A: HttpWrite](body: A): Request =
  {
    val w = implicitly[HttpWrite[A]]
    val r = this.withBody(InMemoryBody(w.toByteArray(body)))
    (w.contentType, r.contentType) match {
      case (None, _)    => r
      case (_, Some(_)) => r
      case (Some(x), _) => r.withContentType(x)
    }
  }
  def contentType: Option[String] =
  {
    this.headers.find(p => p._1 == HeaderNames.CONTENT_TYPE) map { case (header, values) =>
    values.head
  }}
  def withContentType(ct: String): Request = this.addHeader(HeaderNames.CONTENT_TYPE -> ct)
  def withContentType(mt: String, charset: Charset): Request = this.withContentType(mt + ";charset=" + charset.toString)
  def withAuth(auth: Realm): Request = copy(authOpt = Some(auth))
  def withAuth(username: String, password: String): Request = copy(authOpt = Some(Realm(username = username, password = password)))
  def withAuth(username: String, password: String, scheme: AuthScheme): Request = copy(authOpt = Some(Realm(username = username, password = password, scheme = scheme)))
  def withHeaders(headers0: (String, String)*): Request = copy(headers = Map(headers0 map { case (k, v) => k -> List(v) }: _*))
  def addHeader(headers0: (String, String)*): Request = this.addHeaders(headers0: _*)
  def addHeaders(headers0: (String, String)*): Request = copy(headers = this.headers ++ Map(headers0 map { case (k, v) => k -> List(v) }: _*))
  def addHeaders(headers0: Map[String, List[String]]): Request = copy(headers = this.headers ++ headers0)
  def withQueryString(parameters: (String, String)*): Request = copy(queryString = Map(parameters map { case (k, v) => k -> List(v) }: _*))
  def addQueryString(parameters: (String, String)*): Request = copy(queryString = this.queryString ++ Map(parameters map { case (k, v) => k -> List(v) }: _*))
  def withFollowRedirects(follow: Boolean): Request = copy(followRedirectsOpt = Some(follow))
  def withRequestTimeout(requestTimeout: scala.concurrent.duration.Duration): Request = copy(requestTimeoutOpt = Some(requestTimeout))
  def withVirtualHost(virtualHost: String): Request = copy(virtualHostOpt = Some(virtualHost))
  def withProxyServer(proxyServer: ProxyServer): Request = copy(proxyServerOpt = Some(proxyServer))
  private def this(url: String) = this(url, HttpVerbs.GET, EmptyBody(), Map(), Map(), None, None, None, None, None, None)
  
  override def equals(o: Any): Boolean = o match {
    case x: Request => (this.url == x.url) && (this.method == x.method) && (this.body == x.body) && (this.headers == x.headers) && (this.queryString == x.queryString) && (this.signatureOpt == x.signatureOpt) && (this.authOpt == x.authOpt) && (this.followRedirectsOpt == x.followRedirectsOpt) && (this.requestTimeoutOpt == x.requestTimeoutOpt) && (this.virtualHostOpt == x.virtualHostOpt) && (this.proxyServerOpt == x.proxyServerOpt)
    case _ => false
  }
  override def hashCode: Int = {
    37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (37 * (17 + "gigahorse.Request".##) + url.##) + method.##) + body.##) + headers.##) + queryString.##) + signatureOpt.##) + authOpt.##) + followRedirectsOpt.##) + requestTimeoutOpt.##) + virtualHostOpt.##) + proxyServerOpt.##)
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
  def withSignatureOpt(signatureOpt: SignatureCalculator): Request = {
    copy(signatureOpt = Option(signatureOpt))
  }
  def withAuthOpt(authOpt: Option[Realm]): Request = {
    copy(authOpt = authOpt)
  }
  def withAuthOpt(authOpt: Realm): Request = {
    copy(authOpt = Option(authOpt))
  }
  def withFollowRedirectsOpt(followRedirectsOpt: Option[Boolean]): Request = {
    copy(followRedirectsOpt = followRedirectsOpt)
  }
  def withFollowRedirectsOpt(followRedirectsOpt: Boolean): Request = {
    copy(followRedirectsOpt = Option(followRedirectsOpt))
  }
  def withRequestTimeoutOpt(requestTimeoutOpt: Option[scala.concurrent.duration.Duration]): Request = {
    copy(requestTimeoutOpt = requestTimeoutOpt)
  }
  def withRequestTimeoutOpt(requestTimeoutOpt: scala.concurrent.duration.Duration): Request = {
    copy(requestTimeoutOpt = Option(requestTimeoutOpt))
  }
  def withVirtualHostOpt(virtualHostOpt: Option[String]): Request = {
    copy(virtualHostOpt = virtualHostOpt)
  }
  def withVirtualHostOpt(virtualHostOpt: String): Request = {
    copy(virtualHostOpt = Option(virtualHostOpt))
  }
  def withProxyServerOpt(proxyServerOpt: Option[ProxyServer]): Request = {
    copy(proxyServerOpt = proxyServerOpt)
  }
  def withProxyServerOpt(proxyServerOpt: ProxyServer): Request = {
    copy(proxyServerOpt = Option(proxyServerOpt))
  }
}
object Request {
  
  def apply(url: String): Request = new Request(url)
  def apply(url: String, method: String, body: Body, headers: Map[String, List[String]], queryString: Map[String, List[String]], signatureOpt: Option[SignatureCalculator], authOpt: Option[Realm], followRedirectsOpt: Option[Boolean], requestTimeoutOpt: Option[scala.concurrent.duration.Duration], virtualHostOpt: Option[String], proxyServerOpt: Option[ProxyServer]): Request = new Request(url, method, body, headers, queryString, signatureOpt, authOpt, followRedirectsOpt, requestTimeoutOpt, virtualHostOpt, proxyServerOpt)
  def apply(url: String, method: String, body: Body, headers: Map[String, List[String]], queryString: Map[String, List[String]], signatureOpt: SignatureCalculator, authOpt: Realm, followRedirectsOpt: Boolean, requestTimeoutOpt: scala.concurrent.duration.Duration, virtualHostOpt: String, proxyServerOpt: ProxyServer): Request = new Request(url, method, body, headers, queryString, Option(signatureOpt), Option(authOpt), Option(followRedirectsOpt), Option(requestTimeoutOpt), Option(virtualHostOpt), Option(proxyServerOpt))
}
