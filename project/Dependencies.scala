import sbt._

object Dependencies {
  val scala211 = "2.11.12"
  val scala212 = "2.12.20"
  val scala213 = "2.13.16"
  val scala3 = "3.3.5"
  val slf4jV = "1.7.28"
  val slf4jApi = "org.slf4j" % "slf4j-api" % slf4jV
  val jclOverSlf4j = "org.slf4j" % "jcl-over-slf4j" % slf4jV
  val ahc = "org.asynchttpclient" % "async-http-client" % "2.0.39"
  val scalatest = "org.scalatest" %% "scalatest" % "3.2.10"
  val sslConfig = "com.typesafe" %% "ssl-config-core" % "0.6.1"
  val reactiveStreams = "org.reactivestreams" % "reactive-streams" % "1.0.3"
  val akkaHttpVersion = "10.2.7"
  val akkaHttpCore = "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion
  val akkaHttp = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
  val akkaStream = "com.typesafe.akka" %% "akka-stream" % "2.5.32"
  val okHttp = "com.squareup.okhttp3" % "okhttp" % "3.14.2"
  val httpClient5 = "org.apache.httpcomponents.client5" % "httpclient5" % "5.4.3"
  val unfilteredVersion = "0.12.1"
  val ufDirectives = "ws.unfiltered" %% "unfiltered-directives" % unfilteredVersion
  val ufFilter = "ws.unfiltered" %% "unfiltered-filter" % unfilteredVersion
  val ufWebsockets = "ws.unfiltered" %% "unfiltered-netty-websockets" % unfilteredVersion
  val ufUploads = "ws.unfiltered" %% "unfiltered-netty-uploads" % unfilteredVersion
}
