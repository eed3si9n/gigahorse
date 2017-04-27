import sbt._

object Dependencies {
  val scala210 = "2.10.6"
  val scala211 = "2.11.11"
  val scala212 = "2.12.2"
  val slf4jApi = "org.slf4j" % "slf4j-api" % "1.7.25"
  val scalaBoth = Seq(scala211, scala212)
  val ahc = "org.asynchttpclient" % "async-http-client" % "2.0.31"
  val scalatest = "org.scalatest" %% "scalatest" % "3.0.0"
  val sslConfig = "com.typesafe" %% "ssl-config-core" % "0.2.2"
  val reactiveStreams = "org.reactivestreams" % "reactive-streams" % "1.0.0"
  val akkaHttpVersion = "10.0.5"
  val akkaHttpCore = "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion
  val akkaHttp = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
  val sbtIo = "org.scala-sbt" %% "io" % "1.0.0-M7"
  val okHttp = "com.squareup.okhttp3" % "okhttp" % "3.7.0"
  val unfilteredVersion = "0.9.1"
  val ufDirectives = "ws.unfiltered" %% "unfiltered-directives" % unfilteredVersion
  val ufFilter = "ws.unfiltered" %% "unfiltered-filter" % unfilteredVersion
  val ufJetty = "ws.unfiltered" %% "unfiltered-jetty" % unfilteredVersion
  val ufScalatest = "ws.unfiltered" %% "unfiltered-scalatest" % unfilteredVersion
}
