/*
 * Main plugins
 */

resolvers += Resolver.jcenterRepo

// Build, package and load native libraries
addSbtPlugin("ch.jodersky" % "sbt-jni" % "0.4.3")

/*
 * Utility plugins, can be disabled during plain build
 */

// Generate documentation for all sources
addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.3.3")

// Build website
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "0.8.2")

// Integrate website with GitHub pages
addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.5.4")

// Automate release process
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.0")

// Usually a global plugin, made explicit to work with release automation
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")

// Usually a global plugin, made explicit to work with release automation
addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0")
