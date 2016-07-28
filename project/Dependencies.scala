import sbt._

object Dependencies {
  val ahc = "com.ning" % "async-http-client" % "1.9.38"
  val scalatest = "org.scalatest" %% "scalatest" % "2.2.6"
  val sbtIo = "org.scala-sbt" %% "io" % "1.0.0-M6"
  val sslConfig = "com.typesafe" %% "ssl-config-core" % "0.2.1"
  val akkaHttpCore = "com.typesafe.akka" %% "akka-http-core" % "2.4.7"
  val akkaHttpExperimental = "com.typesafe.akka" %% "akka-http-experimental" % "2.4.7"
}
