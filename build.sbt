import Dependencies._

lazy val root = (project in file(".")).
  aggregate(core).
  dependsOn(core).
  settings(inThisBuild(List(
      organization := "com.eed3si9n",
      scalaVersion := "2.11.8",
      crossScalaVersions := Seq("2.10.6", "2.11.8"),
      organizationName := "eed3si9n",
      organizationHomepage := Some(url("http://eed3si9n.com/")),
      homepage := Some(url("https://github.com/eed3si9n/gigahorse")),
      scmInfo := Some(ScmInfo(url("https://github.com/eed3si9n/gigahorse"), "git@github.com:eed3si9n/gigahorse.git")),
      developers := List(
        Developer("eed3si9n", "Eugene Yokota", "@eed3si9n", url("https://github.com/eed3si9n"))
      ),
      version := "0.2-SNAPSHOT",
      crossScalaVersions := Seq("2.10.6", "2.11.8"),
      scalaVersion := "2.11.8",
      description := "An HTTP client for Scala with Async Http Client underneath.",
      licenses := Seq("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))
    )),
    name := "gigahorse",
    publish := (),
    publishLocal := ()
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
    libraryDependencies ++= Seq(ahc, sslConfig, scalatest % Test),
    sourceManaged in (Compile, generateDatatypes) := (sourceDirectory in Compile).value / "scala",
    // You need this otherwise you get X is already defined as class.
    sources in Compile := (sources in Compile).value.toList.distinct
  )
