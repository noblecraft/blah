import sbt._
import Keys._

import sbtassembly.Plugin._
import AssemblyKeys._

object BlahBuild  extends Build {

  fork in run := true

  val PROJECT = "blah"

  val UNFILTERED_VERSION = "0.5.3"

  val SPRING_VERSION = "3.1.1.RELEASE"

  val LOGBACK_VERSION = "1.0.6"

  val commonDeps = Seq(
    "cglib" % "cglib" % "2.2",
    "org.slf4j" % "slf4j-api" % "1.6.6",
    "ch.qos.logback" % "logback-classic" % LOGBACK_VERSION,
    "ch.qos.logback" % "logback-core" % LOGBACK_VERSION,
    "joda-time" % "joda-time" % "2.0",
    "org.joda" % "joda-convert" % "1.2",
    "org.codehaus.jackson" % "jackson-core-asl" % "1.9.3",
    "org.codehaus.jackson" % "jackson-mapper-asl" % "1.9.3",
    "org.eclipse.jgit" % "org.eclipse.jgit" % "2.0.0.201206130900-r",
    "org.eclipse.jetty" % "jetty-servlets" % "7.4.5.v20110725",
    "org.scalatest" %% "scalatest" % "1.6.1" % "test",
    "org.scalamock" %% "scalamock-scalatest-support" % "2.2" % "test",
    "org.easymock" % "easymock" % "3.0" % "test",
    "junit" % "junit" % "4.10" % "test",
    "org.hamcrest" % "hamcrest-all" % "1.1" % "test"
  )

  val springDeps = Seq(
    "org.springframework" % "spring-core" % SPRING_VERSION,
    "org.springframework" % "spring-context" % SPRING_VERSION,
    "org.springframework" % "spring-beans" % SPRING_VERSION
  )

  val resourcesSettings = Seq(resourceDirectory in Compile <<= baseDirectory { _ / "src/main/resources" },
    resourceDirectory in Test <<= baseDirectory { _ / "src/test/resources" })

  val assemblyMergeSettings = Seq(mergeStrategy in assembly <<= (mergeStrategy in assembly) {
    (old) => {
      case "META-INF/spring.tooling" => MergeStrategy.first
      case "overview.html" => MergeStrategy.discard
      case "about.html" => MergeStrategy.discard
      case x => old(x)
    }
  }, mainClass in assembly := Some("com.davezhu.blah.web.Main"))

  lazy val buildSettings = Defaults.defaultSettings ++ assemblySettings ++ Seq(
    organization := "davezhu.com",
    scalaVersion := "2.9.1",
    crossScalaVersions := Seq("2.8.0", "2.8.1", "2.8.2", "2.9.0", "2.9.1"),
    scalacOptions += "-unchecked" // show warnings
  ) ++ resourcesSettings ++ assemblyMergeSettings

  // see: http://stackoverflow.com/questions/8588285/how-to-configure-sbt-to-load-resources-when-running-application

  private def srcPathSetting(projectId: String, rootPkg: String) : Setting[Task[Seq[(File, String)]]] = {
    mappings in (LocalProject(projectId), Compile,  packageSrc) ~= {
      defaults: Seq[(File, String)] =>
        defaults.map { case(file, path) =>
          (file, rootPkg + "/" + path)
        }
    }
  }

  private def module(name: String) (
    settings: Seq[Setting[_]],
    projectId: String = PROJECT + "-" + name,
    dirName: String = name,
    srcPath: String = PROJECT + name
    ) = Project(projectId, file(dirName), settings = (buildSettings ++ srcPathSetting(projectId, srcPath)) ++ settings)

  // Master module
  lazy val rootModule = Project(PROJECT, file("."),
    settings = buildSettings ++ Seq(
      name := PROJECT + "-master"
    )) aggregate (webModule, coreModule)

  lazy val webModule = module("web")(
    settings = Seq[Setting[_]](
      libraryDependencies ++= Seq(
        "net.databinder" %% "unfiltered" % UNFILTERED_VERSION,
        "net.databinder" %% "unfiltered-filter" % UNFILTERED_VERSION,
        "net.databinder" %% "unfiltered-jetty" % UNFILTERED_VERSION,
        "net.databinder" %% "unfiltered-netty" % UNFILTERED_VERSION exclude("org.jboss.netty", "netty"),
        "net.databinder" %% "unfiltered-netty-server" % UNFILTERED_VERSION exclude("org.jboss.netty", "netty"),
        "net.databinder.dispatch" %% "core" % "0.9.0"
      ) ++ springDeps ++ commonDeps
    )
  ) dependsOn(uri("git://github.com/unfiltered/unfiltered-scalate#0.5.3"), coreModule)

  lazy val coreModule = module("core")(
    settings = Seq[Setting[_]](
      libraryDependencies ++= Seq(
        "org.scalaz" %% "scalaz-core" % "6.0.3",
        "org.apache.commons" % "commons-math" % "2.0",
        "commons-io" % "commons-io" % "2.0"
      ) ++ springDeps ++ commonDeps
    ) 
  )

}

