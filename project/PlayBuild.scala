import com.mle.sbtplay.PlayProjects
import sbt.Keys._
import sbt._

object PlayBuild extends Build {

  lazy val p = PlayProjects.plainPlayProject("certmaker").settings(commonSettings: _*)
  val mleGroup = "com.github.malliina"

  val commonSettings = Seq(
    scalaVersion := "2.11.4",
    retrieveManaged := false,
    fork in Test := true,
    resolvers ++= Seq(
      "Typesafe releases" at "http://repo.typesafe.com/typesafe/releases/",
      "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/"),
    libraryDependencies ++= Seq(
      mleGroup %% "play-base" % "0.2.1"
    )
  )
}