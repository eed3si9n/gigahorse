package object gigahorse {
  implicit def richRequest(request: Request): RichRequest = new RichRequest(request)
  type AsyncHandler[A] = com.ning.http.client.AsyncHandler[A]
}
