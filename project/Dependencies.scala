import sbt._

object Dependencies {
  val scala211 = "2.11.8"
  val scala212 = "2.12.1"
  val slf4jApi = "org.slf4j" % "slf4j-api" % "1.7.25"
  val scalaBoth = Seq(scala211, scala212)
  val ahc = "org.asynchttpclient" % "async-http-client" % "2.0.31"
  val scalatest = "org.scalatest" %% "scalatest" % "3.0.0"
  val sslConfig = "com.typesafe" %% "ssl-config-core" % "0.2.1"
  val reactiveStreams = "org.reactivestreams" % "reactive-streams" % "1.0.0"
  val akkaHttpCore = "com.typesafe.akka" %% "akka-http-core" % "10.0.1"
  val akkaHttp = "com.typesafe.akka" %% "akka-http" % "10.0.1"
  val sbtIo = "org.scala-sbt" %% "io" % "1.0.0-M7"
}
