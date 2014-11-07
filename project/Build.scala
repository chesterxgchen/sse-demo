import java.net.URL

import sbt.Keys._
import sbt._
import com.earldouglas.xsbtwebplugin.WebappPlugin._
import com.earldouglas.xsbtwebplugin.PluginKeys._

object Build extends Build {

  // configure prompt to show current project
  override lazy val settings = super.settings :+ {
    shellPrompt := { s => Project.extract(s).currentProject.id + " > " }
  }


  lazy val basicSettings = seq(
    licenses              := Seq("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    scalaVersion          := "2.10.4",
    resolvers             ++= Dependencies.resolutionRepos,
    scalacOptions         := Seq(
      "-encoding", "utf8",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-target:jvm-1.6",
      "-language:_",
      "-Xlog-reflective-calls"
    )
  )

  import Dependencies._

  lazy val exampleSettings = basicSettings ++ webappSettings

  lazy val root : sbt.Project = Project("sse-demo",file("."))
    .settings(basicSettings: _*)
    .settings(exampleSettings: _*)
    .settings(libraryDependencies ++=
      compile(akkaActor,sprayServlet, sprayRouting) ++
      runtime(akkaSlf4j, logback)
    )

}
