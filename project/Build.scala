import sbt._
import Keys._
import scalabuff.ScalaBuffPlugin._

object build extends Build {
  lazy val buildSettings = Defaults.defaultSettings ++ Seq (
    libraryDependencies ++= Seq("net.sandrogrzicic" %% "scalabuff-runtime" % "1.3.5"),
    scalacOptions ++= Seq("-encoding", "utf8", "-unchecked", "-deprecation", "-Xlint", "-Ywarn-all")
  )

  object sonatype extends PublishToSonatype(build) {
    def projectUrl    = "https://github.com/jmconrad/scala-groupcache"
    def developerId   = "jmconrad"
    def developerName = "Josh Conrad"
  }

  lazy val root = Project("main", file("."), settings = Defaults.defaultSettings ++ scalabuffSettings ++ sonatype.settings).configs(ScalaBuff)


  /**
   * Source:  https://github.com/paulp/scala-improving/blob/master/project/Publishing.scala
   * License: https://github.com/paulp/scala-improving/blob/master/LICENSE.txt
   */
  abstract class PublishToSonatype(build: Build) {
    import build._

    val ossSnapshots = "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
    val ossStaging   = "Sonatype OSS Staging" at "https://oss.sonatype.org/service/local/staging/deploy/maven2/"

    def projectUrl: String
    def developerId: String
    def developerName: String

    def licenseName         = "Apache"
    def licenseUrl          = "http://www.apache.org/licenses/LICENSE-2.0"
    def licenseDistribution = "repo"
    def scmUrl              = projectUrl
    def scmConnection       = "scm:git:" + scmUrl

    def generatePomExtra(scalaVersion: String): xml.NodeSeq = {
      <url>{ projectUrl }</url>
        <licenses>
          <license>
            <name>{ licenseName }</name>
            <url>{ licenseUrl }</url>
            <distribution>{ licenseDistribution }</distribution>
          </license>
        </licenses>
        <scm>
          <url>{ scmUrl }</url>
          <connection>{ scmConnection }</connection>
        </scm>
        <developers>
          <developer>
            <id>{ developerId }</id>
            <name>{ developerName }</name>
          </developer>
        </developers>
    }

    def settings: Seq[Setting[_]] = Seq(
      publishMavenStyle := true,
      publishTo <<= version((v: String) => Some( if (v.trim endsWith "SNAPSHOT") ossSnapshots else ossStaging)),
      publishArtifact in Test := false,
      pomIncludeRepository := (_ => false),
      pomExtra <<= (scalaVersion)(generatePomExtra)
    )
  }
}

