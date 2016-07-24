package gigahorse

import scala.concurrent.Future
import com.ning.http.client.{ Request => XRequest, _ }

class AhcRequest(val client: AhcHttpClient,
    val url: String,
    val method: String) extends Request {
  def execute: Future[Response] = client.executeRequest(this)

  /**
   * Creates and returns an AHC request, running all operations on it.
   */
  def buildRequest(): XRequest = {
    val builder = new RequestBuilder(method)
    builder.setUrl(url)
    builder.build()

    /*
    // The builder has a bunch of mutable state and is VERY fiddly, so
    // should not be exposed to the outside world.

    val builder = disableUrlEncoding.map { disableEncodingFlag =>
      new RequestBuilder(method, disableEncodingFlag)
    }.getOrElse {
      new RequestBuilder(method)
    }

    // Set the URL.
    builder.setUrl(url)

    // auth
    auth.foreach { data =>
      val realm = auth(data._1, data._2, authScheme(data._3))
      builder.setRealm(realm)
    }

    // queries
    for {
      (key, values) <- queryString
      value <- values
    } builder.addQueryParam(key, value)

    // Configuration settings on the builder, if applicable
    virtualHost.foreach(builder.setVirtualHost)
    followRedirects.foreach(builder.setFollowRedirects)
    proxyServer.foreach(p => builder.setProxyServer(createProxy(p)))
    requestTimeout.foreach(builder.setRequestTimeout)

    val (builderWithBody, updatedHeaders) = body match {
      case EmptyBody => (builder, this.headers)
      case FileBody(file) =>
        import com.ning.http.client.generators.FileBodyGenerator
        val bodyGenerator = new FileBodyGenerator(file)
        builder.setBody(bodyGenerator)
        (builder, this.headers)
      case InMemoryBody(bytes) =>
        val ct: String = contentType.getOrElse("text/plain")

        val h = try {
          // Only parse out the form body if we are doing the signature calculation.
          if (ct.contains(HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED) && calc.isDefined) {
            // If we are taking responsibility for setting the request body, we should block any
            // externally defined Content-Length field (see #5221 for the details)
            val filteredHeaders = this.headers.filterNot { case (k, v) => k.equalsIgnoreCase(HttpHeaders.Names.CONTENT_LENGTH) }

            // extract the content type and the charset
            val charset = Charset.forName(
              Option(AsyncHttpProviderUtils.parseCharset(ct)).getOrElse {
                // NingWSRequest modifies headers to include the charset, but this fails tests in Scala.
                //val contentTypeList = Seq(ct + "; charset=utf-8")
                //possiblyModifiedHeaders = this.headers.updated(HttpHeaders.Names.CONTENT_TYPE, contentTypeList)
                "utf-8"
              }
            )

            // Get the string body given the given charset...
            val stringBody = new String(bytes, charset)
            // The Ning signature calculator uses request.getFormParams() for calculation,
            // so we have to parse it out and add it rather than using setBody.

            val params = for {
              (key, values) <- FormUrlEncodedParser.parse(stringBody).toSeq
              value <- values
            } yield new Param(key, value)
            builder.setFormParams(params.asJava)
            filteredHeaders
          } else {
            builder.setBody(bytes)
            this.headers
          }
        } catch {
          case e: UnsupportedEncodingException =>
            throw new RuntimeException(e)
        }

        (builder, h)
      case StreamedBody(bytes) =>
        (builder, this.headers)
    }

    // headers
    for {
      header <- updatedHeaders
      value <- header._2
    } builder.addHeader(header._1, value)

    // Set the signature calculator.
    calc.map {
      case signatureCalculator: com.ning.http.client.SignatureCalculator =>
        builderWithBody.setSignatureCalculator(signatureCalculator)
      case _ =>
        throw new IllegalStateException("Unknown signature calculator found: use a class that implements SignatureCalculator")
    }

    builderWithBody.build()
    */
  }
}
