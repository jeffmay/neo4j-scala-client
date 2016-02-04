// Add the resolvers
resolvers ++= Seq(
  Classpaths.sbtPluginReleases,
  Resolver.url(
    "bintray-sbt-plugin-releases",
    url("http://dl.bintray.com/content/sbt/sbt-plugin-releases")
  )(Resolver.ivyStylePatterns)
)

// Allows publishing to bintray
addSbtPlugin("me.lessis" % "bintray-sbt" % "0.2.1")

// The current versions of these are failing because of this bug:
// https://github.com/scoverage/sbt-coveralls/issues/73
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.0.0") // 1.0.3
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.0.1") // 1.3.5
