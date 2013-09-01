import sbt._
import Keys._
import scalabuff.ScalaBuffPlugin._

object build extends Build {
  val dependencies = Seq(
    "net.sandrogrzicic" %% "scalabuff-runtime" % "1.3.5"
  )

  val buildSettings = Defaults.defaultSettings ++ Seq (
    libraryDependencies ++= dependencies
  )

  lazy val root = Project("main", file("."), settings = Defaults.defaultSettings ++ scalabuffSettings).configs(ScalaBuff)
}

