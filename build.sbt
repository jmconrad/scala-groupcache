scalaVersion := "2.10.2"

organization := "org.groupcache"

name := "scala-groupcache"

version := "0.5.0"

resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.0.1" % "test",
  "com.twitter" %% "finagle-core" % "6.3.0",
  "com.twitter" %% "finagle-http" % "6.3.0"
)

