
import sbt._

object Dependencies {

  val resolutionRepos = Seq(
    "spray repo" at "http://repo.spray.io/",
    "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
  )

  def compile   (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "compile")
  def provided  (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "provided")
  def test      (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "test")
  def runtime   (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "runtime")
  def container (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "container")

  val scalaReflect   = "org.scala-lang"                          %   "scala-reflect"               % "2.10.4"
  val akkaActor      = "com.typesafe.akka"                       %%  "akka-actor"                  % "2.2.3"
  val akkaSlf4j      = "com.typesafe.akka"                       %%  "akka-slf4j"                  % "2.2.3"
  val akkaTestKit    = "com.typesafe.akka"                       %%  "akka-testkit"                % "2.2.3"
  val sprayServlet   = "io.spray"                                %   "spray-servlet"               % "1.2.0"
  val sprayRouting   = "io.spray"                                %   "spray-routing"               % "1.2.0"
  val logback        = "ch.qos.logback"                          %   "logback-classic"             % "1.1.1"
}


