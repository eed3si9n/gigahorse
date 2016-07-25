package object gigahorse {
  implicit def richRequest(request: Request): RichRequest = new RichRequest(request)
}
