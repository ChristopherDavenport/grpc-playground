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
  .aggregate(google)


// lazy val scalapbProj = project
//   // .crossType(CrossType.Pure)
//   .in(file("scalapb"))
//   .enablePlugins(Http4sGrpcPlugin)
//   .settings(
//     name := "http4s-grpc-google-apis-scalapb",
//     Compile / PB.targets ++= Seq(
//       // set grpc = false because http4s-grpc generates its own code
//       scalapb.gen(grpc = false) -> (Compile / sourceManaged).value / "scalapb"
//     )
//   )

// lazy val http4sGrpc = project
//   // .crossType(CrossType.Pure)
//   .in(file("http4s-grpc"))
//   .dependsOn(scalapbProj)
//   .enablePlugins(Http4sGrpcPlugin)
//   .settings(
//     name := "http4s-grpc-google-apis",
//     Compile / PB.targets ++= Seq(
//       // set grpc = false because http4s-grpc generates its own code
//       scalapb.gen(grpc = false) -> (Compile / sourceManaged).value / "scalapb"
//     )
//   )

lazy val google = project
  // .crossType(CrossType.Pure)
  .in(file("google"))
  .enablePlugins(Http4sGrpcPlugin)
  .settings(
    name := "google",
    Compile / PB.targets ++= Seq(
      // set grpc = false because http4s-grpc generates its own code
      scalapb.gen(grpc = false) -> (Compile / sourceManaged).value / "scalapb"
    )
  )
