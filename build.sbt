import Dependencies._
import com.typesafe.sbt.pgp.PgpKeys.publishSigned
import Shade._

ThisBuild / organization := "com.eed3si9n"
ThisBuild / scalaVersion := scala212
ThisBuild / crossScalaVersions := Vector(scala212, scala213, scala3)
ThisBuild / organizationName := "eed3si9n"
ThisBuild / organizationHomepage := Some(url("http://eed3si9n.com/"))
ThisBuild / homepage := Some(url("https://github.com/eed3si9n/gigahorse"))
ThisBuild / scmInfo := Some(ScmInfo(url("https://github.com/eed3si9n/gigahorse"), "git@github.com:eed3si9n/gigahorse.git"))
ThisBuild / developers := List(
  Developer("eed3si9n", "Eugene Yokota", "@eed3si9n", url("https://github.com/eed3si9n"))
)
ThisBuild / version := "0.7.1-SNAPSHOT"
ThisBuild / description := "Gigahorse is an HTTP client for Scala with multiple backend support."
ThisBuild / licenses := Seq("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))

lazy val root = (project in file(".")).
  aggregate(core, apacheHttp, asynchttpclient, shadedAsyncHttpClient, okhttp, akkaHttp).
  dependsOn(core).
  settings(
    name := "gigahorse",
    publish / skip := true,
    crossScalaVersions := Nil,
    commands += Command.command("release") { state =>
      "clean" ::
        s"++ ${scala3}!" ::
        "core/publishSigned" ::
        "apacheHttp/publishSigned" ::
        "okhttp/publishSigned" ::
        "asynchttpclient/publishSigned" ::
        s"++ ${scala213}!" ::
        "core/publishSigned" ::
        "apacheHttp/publishSigned" ::
        "okhttp/publishSigned" ::
        "asynchttpclient/publishSigned" ::
        "akkaHttp/publishSigned" ::
        s"++ ${scala212}!" ::
        "core/publishSigned" ::
        "apacheHttp/publishSigned" ::
        "shadedApacheHttpAsyncClient/publishSigned" ::
        "okhttp/publishSigned" ::
        "asynchttpclient/publishSigned" ::
        "shadedAsyncHttpClient/publishSigned" ::
        "akkaHttp/publishSigned" ::
        state
    }
  )

lazy val commonSettings = List(
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
    else Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  scalacOptions ++= Seq(
    "-encoding", "utf8",
    "-deprecation",
    "-unchecked",
    "-Xlint",
    "-feature",
    "-language:existentials",
    "-language:experimental.macros",
    "-language:higherKinds",
    "-language:implicitConversions"
  ),
  Compile / console / scalacOptions --= Seq("-deprecation", "-Xfatal-warnings", "-Xlint"),
  Test / fork := true,
  Compile / javaOptions += "-Xmx2G",
)

lazy val fatalWarnings: Seq[Setting[_]] = List(
  scalacOptions ++= (scalaVersion.value match {
    case VersionNumber(Seq(2, 12, _*), _, _) =>
      List("-Xfatal-warnings")
    case _ => Nil
  }),
)

lazy val core = (project in file("core")).
  enablePlugins(ContrabandPlugin).
  settings(
    commonSettings,
    fatalWarnings,
    name := "gigahorse-core",
    libraryDependencies ++= Seq(sslConfig, reactiveStreams, slf4jApi, scalatest % Test),
    Compile / scalacOptions ++= (scalaVersion.value match {
      case VersionNumber(Seq(2, 12, _*), _, _) =>
        List("-Ywarn-unused:-locals,-explicits,-privates")
      case _ => Nil
    }),
    Compile / managedSourceDirectories += (Compile / sourceDirectory).value / "contraband-scala",
    Compile / unmanagedSourceDirectories += (Compile / sourceDirectory).value / "contraband-scala",
    Compile / generateContrabands / sourceManaged := (Compile / sourceDirectory).value / "contraband-scala",
    // You need this otherwise you get X is already defined as class.
    Compile / sources := (Compile / sources).value.toList.distinct,
  )

lazy val commonTest = (project in file("common-test")).
  dependsOn(core).
  settings(
    libraryDependencies ++= Seq(scalatest,
      ufDirectives, ufFilter, ufWebsockets),
    publish := {},
    publishLocal := {},
    publishSigned := {}
  )

// lazy val packageSite = taskKey[Unit]("package site")
// lazy val doPackageSite = taskKey[File]("package site")
// lazy val packageSitePath = settingKey[File]("path for the package")
// lazy val docsProject = (project in file("docs-project")).
//   dependsOn(asynchttpclient, akkaHttp, okhttp).
//   enablePlugins(PamfletPlugin).
//   settings(
//     sourceDirectory in (Pamflet, pf) := (baseDirectory.value).getParentFile / "docs",
//     packageSitePath := target.value / "gigahorse.tar.gz",
//     doPackageSite := {
//       val out = packageSitePath.value
//       val siteDir = (target in (Pamflet, pfWrite)).value
//       val items = ((siteDir ** "*").get map { _.relativeTo(siteDir) }).flatten
//       Process(s"""tar zcf ${ packageSitePath.value.getAbsolutePath } ${ items.mkString(" ") }""", Some(siteDir)).!
//       out
//     },
//     packageSite := Def.sequential(clean, pfWrite, doPackageSite).value,
//     aggregate in pfWrite := false,
//     aggregate in pf := false,
//     publish := (),
//     publishLocal := (),
//     publishSigned := ()
//   )

lazy val apacheHttp = (project in file("apache-http")).
  dependsOn(core, shadedApacheHttpAsyncClient, commonTest % Test).
  settings(
    commonSettings,
    fatalWarnings,
    name := "gigahorse-apache-http",
    crossScalaVersions := Vector(scala212, scala213, scala3),
  )

lazy val okhttp = (project in file("okhttp")).
  dependsOn(core, commonTest % Test).
  settings(
    commonSettings,
    fatalWarnings,
    name := "gigahorse-okhttp",
    crossScalaVersions := Vector(scala212, scala213, scala3),
    libraryDependencies ++= Seq(Dependencies.okHttp)
  )

lazy val asynchttpclient = (project in file("asynchttpclient")).
  dependsOn(core, shadedAsyncHttpClient, commonTest % Test).
  settings(
    commonSettings,
    fatalWarnings,
    crossScalaVersions := Vector(scala212, scala213, scala3),
    name := "gigahorse-asynchttpclient"
  )

lazy val akkaHttp = (project in file("akka-http")).
  dependsOn(core, commonTest % Test).
  settings(
    commonSettings,
    crossScalaVersions := Vector(scala212, scala213),
    name := "gigahorse-akka-http",
    libraryDependencies ++= Seq(akkaHttpCore, Dependencies.akkaHttp, akkaStream),
    dependencyOverrides += sslConfig
  )

lazy val shadedApacheHttpAsyncClient = (project in file("shaded/apache-httpasyncclient"))
  .configs(ShadeSandbox)
  .settings(commonSettings)
  .settings(apacheShadeSettings)
  .settings(
    name := "shaded-apache-httpasyncclient",
    crossScalaVersions := Vector(scala212, scala213),
    libraryDependencies ++= Seq(
      Dependencies.apacheHttpAsyncClient.exclude("commons-logging", "commons-logging") % ShadeSandbox,
      Dependencies.jclOverSlf4j % ShadeSandbox,
    ),
    autoScalaLibrary := false,
    crossPaths := false
  )

lazy val shadedAsyncHttpClient = (project in file("shaded/asynchttpclient"))
  .configs(ShadeSandbox)
  .settings(commonSettings)
  .settings(ahcShadeSettings)
  .settings(
    crossScalaVersions := Vector(scala212, scala213),
    libraryDependencies ++= Seq(ahc % ShadeSandbox),
    name := "shaded-asynchttpclient",
    autoScalaLibrary := false,
    crossPaths := false
  )
