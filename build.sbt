name := "apibook"

version := "1.0"

scalaVersion := "2.11.7"



lazy val commonSettings = Seq(
  organization := "com.cppdo",
  version := "1.0",
  scalaVersion := "2.11.7"
)

assemblyOption in assembly := (assemblyOption in assembly).value.copy(cacheUnzip = false)

assemblyMergeStrategy in assembly := {
  //case PathList("javax", "servlet", xs @ _*)         => MergeStrategy.first
  //case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
  case ".api_description"                            => MergeStrategy.first
  case "eclipse.inf" => MergeStrategy.first
  case "plugin.properties" => MergeStrategy.first
  case "plugin.xml" => MergeStrategy.first
  case ".options"                                => MergeStrategy.first
  //case ".options"                                => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

lazy val core = (project in file("./apibook-core"))
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= Seq(
    "commons-io" % "commons-io" % "2.4",
    "com.github.scopt" %% "scopt" % "3.3.0",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
    "com.typesafe.akka" %% "akka-actor" % "2.3.9",
    "com.typesafe.akka" %% "akka-testkit" % "2.3.9",
    "com.typesafe.play" %% "play-json" % "2.4.3",
    "org.mongodb" %% "casbah-core" % "3.0.0",
    "com.novus" %% "salat" % "1.9.9",
    "edu.stanford.nlp" % "stanford-corenlp" % "3.5.2",
    "edu.stanford.nlp" % "stanford-corenlp" % "3.5.2" classifier "models",

    "org.apache.httpcomponents" % "httpclient" % "4.5",


    //"org.slf4j" % "slf4j-api" % "1.7.10",
    //"org.slf4j" % "slf4j-nop" % "1.7.10",
    "ch.qos.logback" % "logback-classic" % "1.1.2",
    "com.typesafe.slick" %% "slick" % "3.0.0",
    "com.zaxxer" % "HikariCP" % "2.3.2",
    "com.h2database" % "h2" % "1.4.185",
    "org.xerial" % "sqlite-jdbc" % "3.8.7",
    "org.jsoup" % "jsoup" % "1.8.1",
    "com.github.tototoshi" %% "scala-csv" % "1.2.1",
    "joda-time" % "joda-time" % "2.8.2",
    "org.apache.lucene" % "lucene-core" % "5.3.1",
    "org.apache.lucene" % "lucene-analyzers-common" % "5.3.1",
    "org.apache.lucene" % "lucene-queryparser" % "5.3.1",
    "org.apache.lucene" % "lucene-queries" % "5.3.1",
    "org.eclipse.jdt" % "org.eclipse.jdt.core" % "3.10.0",
    "org.ow2.asm" % "asm-all" % "5.0.4",
    "org.scalatest" %% "scalatest" % "2.2.4" % "test")
  )
  .settings(
    unmanagedJars in Compile += file("java/tools.jar")
  )
  .settings(
    assemblyOption in assembly := (assemblyOption in assembly).value.copy(cacheUnzip = false)
  )
  .settings(
    assemblyMergeStrategy in assembly := {
      //case PathList("javax", "servlet", xs @ _*)         => MergeStrategy.first
      //case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
      case ".api_description"                            => MergeStrategy.first
      case "META-INF/eclipse.inf" => MergeStrategy.first
      case "plugin.properties" => MergeStrategy.first
      case "plugin.xml" => MergeStrategy.first
      case ".options"                                => MergeStrategy.first
      //case ".options"                                => MergeStrategy.discard
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    }
  )

lazy val web = (project in file("./apibook-web")).enablePlugins(PlayScala).dependsOn(core)
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= Seq(
    jdbc,
    cache,
    ws,
    specs2 % Test,
    "org.webjars" %% "webjars-play" % "2.4.0-1",
    "org.webjars" % "angularjs" % "1.4.4",
    "org.webjars" % "angular-ui-bootstrap" % "0.14.3",
    "org.webjars.bower" % "highlightjs" % "8.7.0",
    "org.webjars" % "bootstrap" % "3.3.5",
    "org.webjars" % "underscorejs" % "1.8.3",
    "org.webjars" % "modernizr" % "2.8.3",
    "org.webjars" % "momentjs" % "2.10.2")
  )
  .settings(resolvers += "Scalaz Bintray Repo" at "https://dl.bintray.com/scalaz/releases")

lazy val root = (project in file(".")).aggregate(core, web)
