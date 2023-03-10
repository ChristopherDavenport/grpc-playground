import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

val Scala213 = "2.13.7"

ThisBuild / crossScalaVersions := Seq(Scala213)
ThisBuild / scalaVersion := Scala213

ThisBuild / testFrameworks += new TestFramework("munit.Framework")
ThisBuild / resolvers ++= Resolver.sonatypeOssRepos("snapshots")

val catsV = "2.7.0"
val catsEffectV = "3.2.9"
val fs2V = "3.2.3"
val http4sV = "0.23.18"
val circeV = "0.14.1"
val doobieV = "1.0.0-RC1"
val munitCatsEffectV = "1.0.7"


// Projects
lazy val `grpc-testing` = project.in(file("."))
  .disablePlugins(MimaPlugin)
  .enablePlugins(NoPublishPlugin)
  .aggregate(otel)


lazy val otel = project
  // .crossType(CrossType.Pure)
  .in(file("otel"))
  .enablePlugins(Http4sGrpcPlugin)
  .settings(
    name := "otel",
    Compile / PB.targets ++= Seq(
      // set grpc = false because http4s-grpc generates its own code
      scalapb.gen(grpc = false) -> (Compile / sourceManaged).value / "scalapb"
    )
  )
