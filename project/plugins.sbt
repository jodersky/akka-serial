/*
 * Main plugins
 */

// Build, package and load native libraries
addSbtPlugin("ch.jodersky" % "sbt-jni" % "1.3.2")

/*
 * Utility plugins, can be disabled during plain build
 */
// Generate documentation for all sources
addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.4.2")

// Generate website content
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "1.3.2")

// Sign published artifacts
addSbtPlugin("io.crashbox" % "sbt-gpg" % "0.2.0")

// Publish to sonatype and sync with maven central
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.4")
