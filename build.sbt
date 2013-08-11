name := "scala-groupcache"

version := "0.1"

scalaVersion := "2.10.0"

resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test",
  "net.sandrogrzicic" %% "scalabuff-runtime" % "1.3.5"
)


