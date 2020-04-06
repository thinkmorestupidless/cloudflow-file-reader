import sbt._
import sbt.Keys._

lazy val root =
  Project(id = "root", base = file("."))
    .enablePlugins(ScalafmtPlugin)
    .settings(
      name := "root",
      skip in publish := true,
      scalafmtOnCompile := true,
    )
    .withId("root")
    .settings(commonSettings)
    .aggregate(
      pipeline,
      datamodel,
      `akka-streams`,
      flink
    )

lazy val pipeline = appModule("pipeline")
  .enablePlugins(CloudflowApplicationPlugin)
  .settings(commonSettings)
  .settings(
    name := "pdf-ingestion",
    runLocalConfigFile := Some("src/main/resources/local.conf")
  )
  .dependsOn(`akka-streams`, flink)

lazy val datamodel = appModule("datamodel")
  .enablePlugins(CloudflowLibraryPlugin)
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.10"
    ),
    (sourceGenerators in Compile) += (avroScalaGenerateSpecific in Test).taskValue
  )

lazy val `akka-streams` = appModule("akka-streams")
  .enablePlugins(CloudflowAkkaStreamsLibraryPlugin)
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "com.lightbend.akka" %% "akka-stream-alpakka-file" % "1.1.2",
      "com.typesafe.akka"  %% "akka-http-spray-json"     % "10.1.10",
      "ch.qos.logback"     % "logback-classic"           % "1.2.3",
      "org.scalatest"      %% "scalatest"                % "3.0.8" % "test"
    )
  )
  .dependsOn(datamodel)

lazy val flink = appModule("flink")
  .enablePlugins(CloudflowFlinkLibraryPlugin)
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-slf4j" % "2.5.30",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "org.scalatest"  %% "scalatest"      % "3.0.8" % "test"
    )
  )
  .settings(
    parallelExecution in Test := false
  )
  .dependsOn(datamodel)

def appModule(moduleID: String): Project =
  Project(id = moduleID, base = file(moduleID))
    .settings(
      name := moduleID
    )
    .withId(moduleID)
    .settings(commonSettings)

lazy val commonSettings = Seq(
  organization := "com.dataspartan",
  scalaVersion := "2.12.10",
  crossScalaVersions := Vector(scalaVersion.value),
  scalacOptions ++= Seq(
    "-encoding",
    "UTF-8",
    "-target:jvm-1.8",
    "-Xlog-reflective-calls",
    "-Xlint",
    "-Ywarn-unused",
    "-Ywarn-unused-import",
    "-deprecation",
    "-feature",
    "-language:_",
    "-unchecked"
  ),
  scalacOptions in (Compile, console) --= Seq("-Ywarn-unused", "-Ywarn-unused-import"),
  scalacOptions in (Test, console) := (scalacOptions in (Compile, console)).value
)
