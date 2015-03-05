name := """apibook-web"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.5"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "org.webjars" %% "webjars-play" % "2.3.0-2",
  "org.webjars" % "angularjs" % "1.3.14",
  "org.webjars" % "bootstrap" % "3.3.2-1",
  "org.webjars" % "modernizr" % "2.8.3"
)
