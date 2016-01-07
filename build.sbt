
name := "neo4j-scala-client"

organization := "me.jeffmay"

scalaVersion := "2.11.7"

val playVersion = "2.4.6"
val scalacheckVersion = "1.12.5"
val scalatestVersion = "3.0.0-M10"

resolvers ++= Seq(
  "Artima Maven Repository" at "http://repo.artima.com/releases",
  "jeffmay" at "http://dl.bintray.com/jeffmay/maven"
)

libraryDependencies ++= Seq(
  compilerPlugin("org.psywerx.hairyfotr" %% "linter" % "0.1.12"),
  "com.typesafe.play" %% "play-json" % playVersion,
  "com.typesafe.play" %% "play-ws" % playVersion,
  "org.scalacheck" %% "scalacheck" % scalacheckVersion % "test",
  "org.scalactic" %% "scalactic" % scalatestVersion,
  "org.scalatest" %% "scalatest" % scalatestVersion % "test"
)

dependencyOverrides := Set(
  "com.typesafe.play" %% "play-json" % playVersion,
  "com.google.guava" % "guava" % "18.0",
  "org.scalactic" %% "scalactic" % scalatestVersion
)

// stricter Scala compiler options
scalacOptions in ThisBuild := Seq(
  "-Xfatal-warnings",
  "-feature",
  "-deprecation:false",
  "-unchecked",
  "-Xfuture"
)

