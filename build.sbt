import Dependencies._
import com.typesafe.sbt.pgp.PgpKeys.publishSigned
import Shade._

lazy val root = (project in file(".")).
  aggregate(core, asynchttpclient, shadedAsyncHttpClient, okhttp, akkaHttp).
  dependsOn(core).
  settings(inThisBuild(List(
      organization := "com.eed3si9n",
      scalaVersion := scala212,
      crossScalaVersions := Vector(scala212, scala211, scala210),
      organizationName := "eed3si9n",
      organizationHomepage := Some(url("http://eed3si9n.com/")),
      homepage := Some(url("https://github.com/eed3si9n/gigahorse")),
      scmInfo := Some(ScmInfo(url("https://github.com/eed3si9n/gigahorse"), "git@github.com:eed3si9n/gigahorse.git")),
      developers := List(
        Developer("eed3si9n", "Eugene Yokota", "@eed3si9n", url("https://github.com/eed3si9n"))
      ),
      version := "0.3.0",
      description := "An HTTP client for Scala with Async Http Client underneath.",
      licenses := Seq("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")),
      scalacOptions in Compile ++= Seq(
        "-deprecation", "-Ywarn-unused", "-Ywarn-unused-import"
      ),
      scalacOptions in Compile := {
        val old = (scalacOptions in Compile).value
        scalaBinaryVersion.value match {
          case "2.12" => old
          case _      => old filterNot Set("-Xfatal-warnings", "-deprecation", "-Ywarn-unused", "-Ywarn-unused-import")
        }
      }
    )),
    name := "gigahorse",
    publish := (),
    publishLocal := (),
    publishSigned := (),
    commands += Command.command("release-jdk7") { state =>
      "clean" ::
        "++ 2.10.6" ::
        "core/publishSigned" ::
        "okhttp/publishSigned" ::
        state
    },
    commands += Command.command("release-jdk8") { state =>
      "clean" ::
        "++ 2.12.2" ::
        "core/publishSigned" ::
        "okhttp/publishSigned" ::
        "asynchttpclient/publishSigned" ::
        "shadedAsyncHttpClient/publishSigned" ::
        "akkaHttp/publishSigned" ::
        "++ 2.11.11" ::
        "core/publishSigned" ::
        "okhttp/publishSigned" ::
        "asynchttpclient/publishSigned" ::
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
  scalacOptions in (Compile, console) ~= (_ filterNot Set("-deprecation", "-Ywarn-unused", "-Ywarn-unused-import"))
)

lazy val core = (project in file("core")).
  enablePlugins(ContrabandPlugin).
  settings(
    commonSettings,
    name := "gigahorse-core",
    libraryDependencies ++= Seq(sslConfig, reactiveStreams, slf4jApi, scalatest % Test),
    managedSourceDirectories in Compile += (sourceDirectory in Compile).value / "contraband-scala",
    sourceManaged in (Compile, generateContrabands) := (sourceDirectory in Compile).value / "contraband-scala",
    // You need this otherwise you get X is already defined as class.
    sources in Compile := (sources in Compile).value.toList.distinct
  )

lazy val commonTest = (project in file("common-test")).
  dependsOn(core).
  settings(
    libraryDependencies ++= Seq(scalatest),
    publish := (),
    publishLocal := (),
    publishSigned := ()
  )

// lazy val packageSite = taskKey[Unit]("package site")
// lazy val doPackageSite = taskKey[File]("package site")
// lazy val packageSitePath = settingKey[File]("path for the package")
// lazy val docsProject = (project in file("docs-project")).
//   dependsOn(asynchttpclient, akkaHttp).
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

lazy val okhttp = (project in file("okhttp")).
  dependsOn(core, commonTest % Test).
  settings(
    commonSettings,
    name := "gigahorse-okhttp",
    crossScalaVersions := Vector(scala212, scala211, scala210),
    libraryDependencies ++= Seq(Dependencies.okHttp)
  )

lazy val asynchttpclient = (project in file("asynchttpclient")).
  dependsOn(core, shadedAsyncHttpClient, commonTest % Test).
  settings(
    commonSettings,
    crossScalaVersions := Vector(scala212, scala211),
    name := "gigahorse-asynchttpclient"
  )

lazy val akkaHttp = (project in file("akka-http")).
  dependsOn(core, commonTest % Test).
  settings(
    commonSettings,
    crossScalaVersions := Vector(scala212, scala211),
    name := "gigahorse-akka-http",
    libraryDependencies ++= Seq(akkaHttpCore, Dependencies.akkaHttp),
    dependencyOverrides += sslConfig
  )

lazy val shadedAsyncHttpClient = (project in file("shaded/asynchttpclient"))
  .configs(ShadeSandbox)
  .settings(commonSettings)
  .settings(ahcShadeSettings)
  .settings(
    crossScalaVersions := Vector(scala212),
    libraryDependencies ++= Seq(ahc % ShadeSandbox),
    name := "shaded-asynchttpclient",
    autoScalaLibrary := false,
    crossPaths := false
  )
