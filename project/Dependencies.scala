import sbt._

object Dependencies {
  val scala211 = "2.11.12"
  val scala212 = "2.12.8"
  val scala213 = "2.13.0"
  val slf4jApi = "org.slf4j" % "slf4j-api" % "1.7.26"
  val ahc = "org.asynchttpclient" % "async-http-client" % "2.0.39"
  val scalatest = "org.scalatest" %% "scalatest" % "3.0.8"
  val sslConfig = "com.typesafe" %% "ssl-config-core" % "0.4.0"
  val reactiveStreams = "org.reactivestreams" % "reactive-streams" % "1.0.2"
  val akkaHttpVersion = "10.1.9"
  val akkaHttpCore = "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion
  val akkaHttp = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
  val akkaStream = "com.typesafe.akka" %% "akka-stream" % "2.5.23"
  val okHttp = "com.squareup.okhttp3" % "okhttp" % "3.14.2"
  val unfilteredVersion = "0.10.0-M4"
  val ufDirectives = "ws.unfiltered" %% "unfiltered-directives" % unfilteredVersion
  val ufFilter = "ws.unfiltered" %% "unfiltered-filter" % unfilteredVersion
  val ufScalatest = "ws.unfiltered" %% "unfiltered-scalatest" % unfilteredVersion
  val ufWebsockets = "ws.unfiltered" %% "unfiltered-netty-websockets" % unfilteredVersion
}
