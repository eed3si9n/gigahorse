import Dependencies._

lazy val root = (project in file(".")).
  aggregate(core).
  settings(inThisBuild(List(
      organization := "com.eed3si9n",
      scalaVersion := "2.11.8"
    )),
    name := "gigahorse",
    publish := (),
    publishLocal := ()
  )

lazy val core = (project in file("core")).
  settings(
    name := "gigahorse-core",
    libraryDependencies += ahc
  )
