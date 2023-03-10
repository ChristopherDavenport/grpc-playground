import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

val Scala213 = "2.13.7"

ThisBuild / crossScalaVersions := Seq(Scala213)
ThisBuild / scalaVersion := Scala213

ThisBuild / testFrameworks += new TestFramework("munit.Framework")

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
  .aggregate(core, fs2GRPC)

lazy val core = project
  // .crossType(CrossType.Pure)
  .in(file("core"))
  .enablePlugins(Http4sGrpcPlugin)
  .settings(
    name := "grpc-testing",
    Compile / PB.targets ++= Seq(
      // set grpc = false because http4s-grpc generates its own code
      scalapb.gen(grpc = false) -> (Compile / sourceManaged).value / "scalapb"
    )
  )

lazy val fs2GRPC = project.in(file("fs2-grpc"))
  .enablePlugins(Fs2Grpc)
  .settings(
    libraryDependencies += "io.grpc" % "grpc-netty-shaded" % scalapb.compiler.Version.grpcJavaVersion,
    libraryDependencies +=       "ch.qos.logback" % "logback-classic" % "1.2.3",
  )
