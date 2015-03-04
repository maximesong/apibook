name := "apibook"

version := "1.0"

scalaVersion := "2.11.5"

libraryDependencies ++= Seq(
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
  "org.ow2.asm" % "asm-all" % "5.0.3"
)