package com.cppdo.apibook

import java.io.{FileNotFoundException, File}
import java.net.URL
import java.nio.file.{Path, Paths, Files}

import akka.actor.{PoisonPill, Props, ActorSystem}
import akka.routing.RoundRobinPool
import com.cppdo.apibook.actor.ActorProtocols._
import com.cppdo.apibook.actor._
import com.cppdo.apibook.ast.{AstTreeManager, ClassVisitor, JarManager}
import com.cppdo.apibook.db._
import com.cppdo.apibook.index.IndexManager
import com.cppdo.apibook.repository.{GitHubRepositoryManager, ArtifactsManager, MavenRepository}
import com.cppdo.apibook.repository.ArtifactsManager.RichArtifact
import com.cppdo.apibook.repository.MavenRepository.{MavenArtifact, MavenArtifactSeq, MavenProject}
import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.io.FileUtils
import slick.driver.JdbcDriver
import slick.jdbc.meta.MTable
import slick.model.ForeignKeyAction.NoAction

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.Source

import sys.process._
//import slick.driver.H2Driver.api._

/**
 * Created by song on 1/17/15.
 */
object APIBook extends LazyLogging {
  case class Config(mode: String = "")
  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[Config]("apibook") {
      head("APIBook", "1.0")
      cmd("fetch")  action {
        (_, c) => c.copy(mode="fetch")
      }
      cmd("build") action {
        (_, c) => c.copy(mode="build")
      }
      cmd("test") action {
        (_, c) => c.copy(mode="test")
      }
    }
    parser.parse(args, Config()) match {
      case Some(config) => {
        logger.info("Hi")
        config.mode match {
          case "fetch" => fetch()
          case "build" => buildIndex()
          case "test" => test()
          case _ => parser.reportError("No command") // do nothing
        }
        logger.info("Bye")
      }


      case None =>
      // arguments are bad, error message will have been displayed
    }

    //fetchProjects()
    //fetchAll()
    //buildIndex
    //search("test")
    //testVersions()
    //testJar()
    //testSource()

    //testActor()
    //downloadPackages()
    //analyze()
    //tryAnalyze()
    //buildIndexActor()
    //testGithub()
    //test()

  }

  def test() = {
    //JarManager.getDocEntries("/Users/song/Projects/apibook/repository/commons-io/commons-io/2.4/commons-io-2.4-javadoc.jar", Klass())
  }

  def fetch() = {
    val projects = ActorMaster.collectProjects(10)
    projects.foreach(project => {
      ActorMaster.analyzeProject(project)
    })
    logger.info(projects.toString())
    ActorMaster.shutdown()
  }

  def buildIndex() = {
    val indexWriter = IndexManager.createIndexWriter()
    DatabaseManager.getClasses().foreach(klass => {
      val document = IndexManager.buildDocument(klass)
      indexWriter.addDocument(document)
    })

    DatabaseManager.getMethods().foreach(method => {
      val document = IndexManager.buildDocument(method)
      indexWriter.addDocument(document)
    })
    indexWriter.close()
  }

  def testGithub() = {
    val system = ActorSystem()
    val github = system.actorOf(Props(new GitHubRepositoryActor()))
    github ! CollectProjects(10)
  }

  def buildIndexActor() = {
    val system = ActorSystem()
    val indexer = system.actorOf(Props(new BuildIndexActor()))
    DatabaseManager.getClasses().foreach(klass => {
      indexer ! BuildIndexForClass(klass)
    })
    DatabaseManager.getMethods().foreach(method => {
      indexer ! BuildIndexForMethod(method)
    })
    indexer ! PoisonPill
  }

  def downloadPackages() = {
    val system = ActorSystem()
    val storageActor = system.actorOf(Props(new DbWriteActor()), "db")
    val fetchActor = system.actorOf(Props(new PackageFetchActor(storageActor)))
    fetchActor ! FetchLatestPackages()
  }

  def tryAnalyze() = {
    val system = ActorSystem()
    val storageActor = system.actorOf(Props(new DbWriteActor()), "db")
    val analyzer = system.actorOf(Props(new ArtifactAnalyzer(storageActor)))
    val project = DatabaseManager.getProjects().head
    DatabaseManager.getArtifacts(project).takeLatestVersion.foreach(artifact => {
      analyzer ! artifact
    })
  }

  def analyze() = {
    val system = ActorSystem()
    val storageActor = system.actorOf(Props(new DbWriteActor()), "db")
    val analyzer = system.actorOf(Props(new ArtifactAnalyzer(storageActor)))
    analyzer ! AnalyzeAndSave()
  }

  def testActor() = {
    val system = ActorSystem()
    val mavenFetchActor = system.actorOf(
      RoundRobinPool(3).props(Props(new MavenFetchActor())), "maven")

    val storageActor = system.actorOf(Props(new DbWriteActor()), "db")

    val artifactsCollector = system.actorOf(Props(new ArtifactsCollectActor(mavenFetchActor, storageActor)), "artifact")

    mavenFetchActor ! FetchProjects(5, Some(artifactsCollector))

  }

  def testSource() = {
    val jarFile = "/home/song/Projects/apibook/repository/junit/junit/4.12/junit-4.12-sources.jar"
    val compilationUnits = JarManager.getCompilationUnits(jarFile)
    logger.info("size:" + compilationUnits.size)
    compilationUnits.foreach(cu => {
      val classVisitor = new ClassVisitor
      cu.accept(classVisitor)
      classVisitor.types.foreach(t => {
        val klass = AstTreeManager.buildFrom(t, Artifact("", "", "", Some(1)))
        t.getMethods.foreach(methodDeclaration => {
          val method = AstTreeManager.buildFrom(methodDeclaration, klass.copy(id=Some(1)))
          logger.info(method.toString)
        })
        //logger.info(klass.toString)
      })

    })
  }

  def search(query: String) = {
    val results = IndexManager.trivial_search(query)
    logger.info("HERE?" + results.size)
    results.foreach(document => println(document.get("Name")))
  }


  def testVersions(): Unit = {
    val v = MavenRepository.Version(0)
    //println(v.matchQualifierToVersion("M-1"))
    //println(v.matchQualifierToVersion("abc"))
    /*
    val t1 = "1.0.6-M1"
    val t2 = "1.0.6"
    val v1 = MavenRepository.Version.parse(t1)
    val v2 = MavenRepository.Version.parse(t2)
    //println(v1 compare v2)
    */
    val projects = MavenRepository.getTopProjects(500)
    projects.flatMap(project => {
      val artifacts = project.fetchArtifacts
      artifacts.flatMap(artifact => {
        MavenRepository.Version.tryParse(artifact.version).flatMap(_.qualifier)
      })
    }).toSet.toList.sortBy(v.matchQualifierToVersion).foreach(println)

  }

  def testJar() = {
    val nodes = JarManager.getClassNodes("/home/song/Downloads/asm-5.0.3.jar")
    nodes.foreach(node => println(node.name))
    IndexManager.buildIndex(nodes)
  }

  def fetchAll() = {
    ArtifactsManager.fetchTopProjectsAndArtifactsToDb(1000)
    ArtifactsManager.downloadLatestPackages
  }

  def fetchProjects() = {
    val baseDirectory = "repository"
    val projects = MavenRepository.getTopProjects(10)
    val latestArtifacts = projects.flatMap(_.fetchArtifacts.takeLatestVersion)
    latestArtifacts.foreach(artifact => {
      println(s"Downloading artifacts of ${artifact.name}...")
      FileUtils.copyURLToFile(new URL(artifact.libraryPackageUrl), new File(artifact.fullLibraryPackagePath))
      FileUtils.copyURLToFile(new URL(artifact.sourcePackageUrl), new File(artifact.fullSourcePackagePath))
      //new URL(artifact.libraryPackageUrl) #> new File(artifact.libraryPackagePath) !!
    })
      //val insertActions = projects.map(project => projectsTable.insertOrUpdate(project))
      //Await.result(db.run(setup), Duration.Inf)
  }


  /*
  import slick.driver.SQLiteDriver.api._
  val db = Database.forConfig("sqliteDb")
  val artifactsTable = TableQuery[Artifacts]
  val setup = DBIO.seq(
    artifactsTable.schema.create
  )
  Await.result(db.run(setup), Duration.Inf)
  //val db = Database.forConfig("h2mem1")
  */
}
