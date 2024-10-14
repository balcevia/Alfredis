import scala.collection.Seq

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.4.2"

name := "Alfredis"

lazy val commonSettings = Seq(
  scalacOptions := Seq(
    "-unchecked",
    "-deprecation",
    "-encoding",
    "utf8",
    "-feature",
    "literal-types",
    "-source:3.5",
    "-Wunused:all",
    "-Wvalue-discard",
    "-Xfatal-warnings",
    "-Yretain-trees",
  ),
)

lazy val domain = project
  .settings(commonSettings *)
  .settings(
    libraryDependencies ++= Dependencies.domain,
  )

lazy val zookeeper = project
  .settings(commonSettings *)
  .dependsOn(domain)
  .settings(
    libraryDependencies ++= Dependencies.zookeeper
  )

lazy val `app` = (project in file("."))
  .aggregate(
    domain,
  )
