import sbt._
import sbt.Defaults.defaultSettings
import Keys._
import sbtdocker.DockerPlugin.autoImport._
import sbtdocker.DockerKeys.{docker, dockerfile}
import sbtdocker.DockerPlugin
import sbtassembly.AssemblyKeys._
import sbtassembly.AssemblyPlugin._
import sbtassembly.{MergeStrategy, PathList}

object StreetviewLoaderBuild extends Build {

  // Scala version used
  val buildScalaVersion = "2.11.5"

  // Compiler flag
  final val StandardSettings = defaultSettings ++ Seq(
    scalacOptions ++= Seq("-unchecked", "-deprecation"),
    scalaVersion := buildScalaVersion,
    retrieveManaged in ThisBuild := true
  )

  // The default project
  lazy val streetviewloader = Project(
    id = "streetview-loader",
    base = file("."),
    settings = StandardSettings ++ tsAssemblySettings ++ Seq(
      description := "Streetview Loader",
      libraryDependencies ++= additionalComponents, // See below
      resolvers ++= ExtraResolvers,
      mainClass in(Compile, run) := Some("io.torchbearer.streetviewloader.StreetviewLoader"),
      dockerSettings
    )
  ).dependsOn(core).enablePlugins(DockerPlugin)


  // ------------------------------------------
  // Docker Builder
  // ------------------------------------------
  val dockerSettings = dockerfile in docker := {
    // The assembly task generates a fat JAR file
    val artifact: File = assembly.value
    val artifactTargetPath = s"/target/${artifact.name}"

    new Dockerfile {
      from("java")
      add("build.jar", artifactTargetPath)
      entryPoint("java", "-jar", artifactTargetPath)
    }
  }

  // ------------------------------------------
  // Assembly Plugin
  // ------------------------------------------
  val tsAssemblySettings = assemblySettings ++ Seq(
    assemblyOutputPath in assembly := file("target/build.jar"),
    assemblyJarName in assembly := "build.jar",
    mainClass in assembly := Some("io.torchbearer.streetviewloader.StreetviewLoader")
  )

  // ------------------------------------------
  // Torchbearer Core Module
  // ------------------------------------------

  lazy val core = ProjectRef(file("../service-core"), "service-core")

  // ------------------------------------------
  // Additional components
  // ------------------------------------------

  // To enable a component remove the //
  val additionalComponents =
  Seq(akka, simplelatlng)


  // ------------------------------------------
  // Component versions
  // ------------------------------------------

  // Other components
  lazy val akka = "com.typesafe.akka" %% "akka-actor" % "2.5.0"
  lazy val simplelatlng = "com.javadocmd" % "simplelatlng" % "1.3.1"

  // Additional repos, required by some components
  final val ExtraResolvers = Seq(
    // Similar to Scala-tools.org
    "SonaScalaTools" at "http://oss.sonatype.org/content/groups/scala-tools/"

    // Typesafe repo - needed for Akka, Scalatra
    , "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

    // Snapshots: the bleeding edge
    , "snapshots-repo" at "https://oss.sonatype.org/content/repositories/snapshots/"

    // For geocoder jar at http://jgeocoder.sourceforge.net/
    , "Drexel" at "https://www.cs.drexel.edu/~zl25/maven2/repo"
  )

}
