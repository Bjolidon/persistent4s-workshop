import Dependencies._

ThisBuild / scalaVersion := "3.8.3"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

val CatsEffectV = "3.7.0"

val SkunkV = "1.0.0"

val Http4sV = "0.23.34"

val Smithy4sV = smithy4s.codegen.BuildInfo.version

val PureconfigV = "0.17.8"

lazy val root = (project in file("."))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    name := "test-persitstent4s-import",
    libraryDependencies ++= Seq(
      // Step 0: add the 4 persistent4s dependencies here
      // (persistent4s-core, persistent4s-postgres, persistent4s-circe, persistent4s-monitoring)
      // all at version 0.2.1 under groupId io.github.antoniojimeneznieto
      "org.typelevel" %% "cats-effect" % CatsEffectV,
      "org.tpolecat" %% "skunk-core" % SkunkV,
      "org.tpolecat" %% "skunk-circe" % SkunkV,
      "org.http4s" %% "http4s-ember-server" % Http4sV,
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s-swagger" % Smithy4sV,
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s" % Smithy4sV,
      "org.scalameta" %% "munit" % "1.0.0" % Test
    ),
    scalacOptions += "-deprecation"
  )
