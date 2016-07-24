package gigahorse

abstract class Gigahorse {
  /**
   * Generates a request.
   *
   * @param url The base URL to make HTTP requests to.
   * @return Request
   */
  def url(url: String): Request =
    Request(url)
      /*, Map(), Map(), None, None, None, None, None, None, None */

  def withHttp[A](config: Config)(f: HttpClient => A): A =
    {
      val client: HttpClient = http(config)
      try {
        f(client)
      }
      finally {
        client.close()
      }
    }
  def withHttp[A](f: HttpClient => A): A =
    withHttp(config)(f)

  def config: Config = Config()

  /** Returns HttpClient. You must call `close` when you're done. */
  def http(config: Config): HttpClient = new AhcHttpClient(config)
}

object Gigahorse extends Gigahorse
