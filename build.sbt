name := "neo4j-scala-client"
organization := "me.jeffmay"
organizationName := "Jeff May"

version := "0.1.0"
scalaVersion := "2.11.7"

// Publish to https://bintray.com/jeffmay/maven
bintraySettings
bintrayPublishSettings

val playVersion = "2.4.6"
val scalacheckVersion = "1.12.5"
val scalatestVersion = "3.0.0-M10"

resolvers ++= Seq(
  "jeffmay" at "http://dl.bintray.com/jeffmay/maven",
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
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

lazy val cleanup = settingKey[Boolean]("Set to false to disable cleaning up after each test")
cleanup := true
addCommandAlias("cleanupOn", "set cleanup := true")
addCommandAlias("cleanupOff", "set cleanup := false")

// call setup and teardown hooks before and after any tests run
testOptions ++= Seq(
  Tests.Setup {
    classLoader: ClassLoader =>
      val cleanDBAfterTests = cleanup.value
      sys.props += "NEO4J_TEST_CLEANUP_AFTER" -> cleanDBAfterTests.toString
      if (cleanDBAfterTests) {
        println(AnsiColor.GREEN +
          """All tests will cleanup their changes to their namespace in the database after they finish.
            |Note: The tests will cleanup their own namespace from previous test runs before they run.""".stripMargin
          + AnsiColor.RESET)
      }
      else {
        println(AnsiColor.BLUE +
          """Tests will NOT cleanup their changes to their namespace in the database after they finish.
            |Note: The tests will cleanup their own namespace from previous test runs before they run.""".stripMargin
          + AnsiColor.RESET)
      }
      classLoader.loadClass("me.jeffmay.neo4j.client.SetupBeforeTests").newInstance()
  },
  Tests.Cleanup {
    classLoader: ClassLoader =>
      classLoader.loadClass("me.jeffmay.neo4j.client.CleanupAfterTests").newInstance()
  }
)

