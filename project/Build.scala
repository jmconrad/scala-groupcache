import sbt._
import Keys._
import scalabuff.ScalaBuffPlugin._

object build extends Build {
  val dependencies = Seq(
    "net.sandrogrzicic" %% "scalabuff-runtime" % "1.3.5"
  )

  val buildSettings = Defaults.defaultSettings ++ Seq (
    name := "scala-groupcache",
    version := "0.1",
    scalaVersion := "2.10.2",
    crossPaths := false,
    resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
    libraryDependencies ++= dependencies
  )

  lazy val root = Project("main", file("."), settings = Defaults.defaultSettings ++ scalabuffSettings).configs(ScalaBuff)
}

