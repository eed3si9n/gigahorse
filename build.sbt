import Dependencies._
import com.typesafe.sbt.pgp.PgpKeys.publishSigned
import Shade._

lazy val root = (project in file(".")).
  aggregate(core, akkaHttp, asynchttpclient).
  dependsOn(core).
  settings(inThisBuild(List(
      organization := "com.eed3si9n",
      scalaVersion := "2.12.1",
      crossScalaVersions := scalaBoth,
      organizationName := "eed3si9n",
      organizationHomepage := Some(url("http://eed3si9n.com/")),
      homepage := Some(url("https://github.com/eed3si9n/gigahorse")),
      scmInfo := Some(ScmInfo(url("https://github.com/eed3si9n/gigahorse"), "git@github.com:eed3si9n/gigahorse.git")),
      developers := List(
        Developer("eed3si9n", "Eugene Yokota", "@eed3si9n", url("https://github.com/eed3si9n"))
      ),
      version := "0.2.0",
      description := "An HTTP client for Scala with Async Http Client underneath.",
      licenses := Seq("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")),
      scalacOptions ++= Seq(
        "-deprecation", "-Ywarn-unused", "-Ywarn-unused-import"
      ),
      scalacOptions := {
        val old = scalacOptions.value
        scalaBinaryVersion.value match {
          case "2.12" => old
          case _      => old filterNot Set("-Xfatal-warnings", "-deprecation", "-Ywarn-unused", "-Ywarn-unused-import")
        }
      }
    )),
    name := "gigahorse",
    publish := (),
    publishLocal := (),
    publishSigned := ()
  )

lazy val commonSettings = List(
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
    else Some("releases" at nexus + "service/local/staging/deploy/maven2")
  }
)

lazy val core = (project in file("core")).
  enablePlugins(DatatypePlugin).
  settings(
    commonSettings,
    name := "gigahorse-core",
    libraryDependencies ++= Seq(sslConfig, reactiveStreams, slf4jApi, scalatest % Test),
    sourceManaged in (Compile, generateDatatypes) := (sourceDirectory in Compile).value / "scala",
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

lazy val asynchttpclient = (project in file("asynchttpclient")).
  dependsOn(core, shadedAsyncHttpClient, commonTest % Test).
  settings(
    commonSettings,
    name := "gigahorse-asynchttpclient"
  )

lazy val akkaHttp = (project in file("akka-http")).
  dependsOn(core, commonTest % Test).
  settings(
    commonSettings,
    name := "gigahorse-akka-http",
    libraryDependencies ++= Seq(akkaHttpCore, Dependencies.akkaHttp),
    dependencyOverrides += sslConfig
  )

lazy val shadedAsyncHttpClient = (project in file("shaded/asynchttpclient"))
  .configs(ShadeSandbox)
  .settings(commonSettings)
  .settings(ahcShadeSettings)
  .settings(
    libraryDependencies ++= Seq(ahc % ShadeSandbox),
    name := "shaded-asynchttpclient",
    autoScalaLibrary := false,
    crossPaths := false
  )
