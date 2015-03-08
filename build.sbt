name := "apibook"

version := "1.0"

scalaVersion := "2.11.5"

lazy val commonSettings = Seq(
  organization := "com.cppdo",
  version := "1.0",
  scalaVersion := "2.11.5"
)

lazy val core = (project in file("./apibook-core"))
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
    //"org.slf4j" % "slf4j-api" % "1.7.10",
    //"org.slf4j" % "slf4j-nop" % "1.7.10",
    "ch.qos.logback" % "logback-classic" % "1.1.2",
    "com.typesafe.slick" %% "slick" % "3.0.0-RC1",
    "com.zaxxer" % "HikariCP" % "2.3.2",
    "com.h2database" % "h2" % "1.4.185",
    "org.xerial" % "sqlite-jdbc" % "3.8.7",
    "org.jsoup" % "jsoup" % "1.8.1",
    "joda-time" % "joda-time" % "2.7",
    "org.apache.lucene" % "lucene-core" % "5.0.0",
    "org.apache.lucene" % "lucene-analyzers-common" % "5.0.0",
    "org.ow2.asm" % "asm-all" % "5.0.3")
  )

lazy val web = (project in file("./apibook-web")).enablePlugins(PlayScala).dependsOn(core)
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= Seq(
    jdbc,
    anorm,
    cache,
    ws,
    "org.webjars" %% "webjars-play" % "2.3.0-2",
    "org.webjars" % "angularjs" % "1.3.14",
    "org.webjars" % "bootstrap" % "3.3.2-1",
    "org.webjars" % "underscorejs" % "1.8.2",
    "org.webjars" % "modernizr" % "2.8.3",
    "org.webjars" % "momentjs" % "2.9.0")
  )

lazy val root = (project in file(".")).aggregate(core, web)
