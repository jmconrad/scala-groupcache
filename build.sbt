scalaVersion := "2.10.2"

resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.0.1" % "test",
  "com.twitter" %% "finagle-core" % "6.3.0"
)

