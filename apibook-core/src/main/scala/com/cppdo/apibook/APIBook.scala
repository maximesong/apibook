package com.cppdo.apibook

import java.io.{FileNotFoundException, File}
import java.net.URL
import java.nio.file.{Path, Paths, Files}

import akka.actor.{PoisonPill, Props, ActorSystem}
import akka.pattern._
import akka.routing.RoundRobinPool
import akka.util.Timeout
import com.cppdo.apibook.actor.ActorProtocols._
import com.cppdo.apibook.actor._
import com.cppdo.apibook.ast.{AstTreeManager, ClassVisitor, JarManager}
import com.cppdo.apibook.db._
import com.cppdo.apibook.forum.{StackOverflowMongoDb, StackOverflowCrawler}
import com.cppdo.apibook.index.IndexManager
import com.cppdo.apibook.repository.{GitHubRepositoryManager, ArtifactsManager, MavenRepository}
import com.cppdo.apibook.repository.ArtifactsManager.RichArtifact
import com.cppdo.apibook.repository.MavenRepository.{MavenArtifact, MavenArtifactSeq, MavenProject}
import com.github.tototoshi.csv.CSVWriter
import com.mongodb.casbah.MongoClient
import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.io.FileUtils
import org.objectweb.asm.Type
import com.novus.salat._
import com.novus.salat.global._
import org.jsoup.Jsoup
import play.libs.Json
import slick.driver.JdbcDriver
import slick.jdbc.meta.MTable
import slick.model.ForeignKeyAction.NoAction

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.io.Source

import scala.collection.JavaConverters._

import sys.process._
//import slick.driver.H2Driver.api._
import com.mongodb.casbah.Imports._


/**
 * Created by song on 1/17/15.
 */
object APIBook extends LazyLogging {

  case class Config(mode: String = "", n: Int = 20, outFile: String = "out.csv", begin: Int = 0,
                    directory: Option[String] = None, file: Option[String] = None)
  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[Config]("apibook") {
      head("APIBook", "1.0")

      opt[Int]('n', "number") action {
        (n, c) => c.copy(n=n)
      }
      opt[Int]('b', "begin") action {
        (begin, c) => c.copy(begin=begin)
      }
      opt[String]('o', "out") action {
        (outFile, c) => c.copy(outFile=outFile)
      }
      opt[String]('d', "directory") action {
        (directory, c) => c.copy(directory=Some(directory))
      }
      opt[String]('f', "file") action {
        (file, c) => c.copy(file=Some(file))
      }
      cmd("fetch")  action {
        (_, c) => c.copy(mode="fetch")
      }
      cmd("build") action {
        (_, c) => c.copy(mode="build")
      }
      cmd("test") action {
        (_, c) => c.copy(mode="test")
      }
      cmd("so") action {
        (_, c) => c.copy(mode="stackoverflow")
      }
      cmd("db") action {
        (_, c) => c.copy(mode="db")
      }
      cmd("find") action {
        (_, c) => c.copy(mode="find")
      }
      cmd("usage") action {
        (_, c) => c.copy(mode="usage")
      }
      cmd("const") action {
        (_, c) => c.copy(mode="const")
      }
    }
    parser.parse(args, Config()) match {
      case Some(config) => {
        logger.info("Hi")
        config.mode match {
          case "fetch" => fetch(config.n)
          case "build" => buildIndex()
          case "test" => test()
          case "stackoverflow" => stackoverflow(config)
          case "db" => db(config)
          case "usage" => usage(config)
          case "find" => find(config)
          case "const" => buildConstant(config)
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

  def db(config: Config) = {
    val db = new CodeMongoDb("localhost","apibook")
    config.directory.foreach(directory => {
      val files = FileUtils.listFiles(new File(directory), Array("jar"), true)
      files.asScala.foreach(file => {
        println(file.getAbsolutePath)
        val classNodes = JarManager.getClassNodes(file.getAbsolutePath)
        classNodes.foreach(classNode => {
          val codeClass = AstTreeManager.buildCodeClass(classNode)
          db.upsertClass(codeClass)
        })
      })
    })
    config.file.foreach(file => {
      val classNodes = JarManager.getClassNodes(file)
      classNodes.foreach(classNode => {
        val codeClass = AstTreeManager.buildCodeClass(classNode)
        db.upsertClass(codeClass)
      })
    })
    val runtimeJarPath = "/Users/song/Projects/apibook/java/rt.jar"


  }

  def buildConstant(config: Config) = {
    val classNodes = JarManager.getClassNodes("/Users/song/Projects/apibook/repository/junit/junit/4.12/junit-4.12.jar")
    classNodes.foreach(classNode => {
      AstTreeManager.calculateConstantParameter(classNode)
    })

  }

  def usage(config: Config) = {
    //val classNodes = JarManager.getClassNodes("/Users/song/Projects/apibook/repository/junit/junit/4.12/junit-4.12.jar")
    //val runtimeJarPath = "/Users/song/Projects/apibook/java/rt.jar"
    //val classNodes = JarManager.getClassNodes(runtimeJarPath)
    val db = new CodeMongoDb("localhost","apibook")

    config.file.foreach(filename => {
      val classNodes = JarManager.getClassNodes(filename)
      classNodes.foreach(classNode => {
        val classType = Type.getObjectType(classNode.name)
        val methodSet = AstTreeManager.calculateMethodUsage(classNode)
        logger.info(s"${classType.getClassName}...")
        methodSet.foreach(method => {
          db.upsertMethodInvocation(method, classType.getClassName)
        })
      })
    })

    config.directory.foreach(directory => {
      val files = FileUtils.listFiles(new File(directory), Array("jar"), true)
      files.asScala.foreach(file => {
        val classNodes = JarManager.getClassNodes(file.getAbsolutePath)
        classNodes.foreach(classNode => {
          val classType = Type.getObjectType(classNode.name)
          val methodSet = AstTreeManager.calculateMethodUsage(classNode)
          logger.info(s"${classType.getClassName}...")
          methodSet.foreach(method => {
            db.upsertMethodInvocation(method, classType.getClassName)
          })
        })
      })
    })
  }

  def find(config: Config) = {
    findMethodConvertTo("java.io.InputStream", "java.lang.String")
  }

  def findMethodConvertTo(from: String, to: String) = {
    val db = new CodeMongoDb("localhost","apibook")
    val classNodes = db.findMethodConvert(from, to)
    classNodes.foreach(classNode => {
      classNode.methods.foreach(method => {
        if (method.parameterTypes.contains(from) && method.returnType == to) {
          println(s"${classNode.fullName}.${method.name}")
        }
      })
    })
    println(classNodes.size)
    val targetClass = db.findClassReturn(to)

  }

  def test() = {
    /*
    val url = "http://stackoverflow.com/questions/215497/in-java-whats-the-difference-between-public-default-protected-and-private"
    val question = StackOverflowCrawler.fetchQuestion(url)
    question.answers.foreach(answer => {
      println(answer.voteNum)
      answer.inlineCodeList.foreach(code => {
        println(code)
      })
    })
    println(Json.prettyPrint(Json.toJson(question)))
    */


    val db = new CodeMongoDb("localhost","apibook")


    //val classNodes = JarManager.getClassNodes("/Users/song/Projects/apibook/java/rt.jar")
    /*
    val classNodes = JarManager.getClassNodes("/Users/song/Projects/apibook/repository/junit/junit/4.12/junit-4.12.jar")
    //println(classNodes.size)
    classNodes.foreach(node => {
      //println(Type.getObjectType(node.name).getClassName)
      AstTreeManager.buildCodeClass(node)
    })
    val codeClass = AstTreeManager.buildCodeClass(classNodes(0))
    */
    //val dbo = grater[CodeClass].asDBObject(codeClass)
    //println(dbo.toString)

  }

  def stackoverflow(config: Config) = {
    val overviews = StackOverflowCrawler.fetchQuestionOverviews(config.n)
    val mongoClient = new StackOverflowMongoDb("localhost", "apibook")
    val waitTime = 1 * 100

    val writer = CSVWriter.open(config.outFile)
    writer.writeRow(List("Question ID", "Votes", "Title", "Ask API", "Answer With One API", "Link"))
    var i = 0
    overviews.foreach(overview => {
      i += 1
      writer.writeRow(List(overview.id, overview.voteNum, overview.title, "", "", overview.questionUrl))
      mongoClient.upsertQuestionOverview(overview)
      logger.info(s"Fetching Question #${overview.id} [${i}/${config.n}]")
      if (i >= config.begin) {
        val question = StackOverflowCrawler.fetchQuestion(overview.questionUrl)
        Thread.sleep(waitTime)
        mongoClient.upsertQuestion(question)
      }
    })
    logger.info(s"Write to ${config.outFile}")
    writer.close()
  }

  def saveQuestionSummariesToMongo(count: Int) = {
    val mongoClient = MongoClient("localhost", 27017)
    val db = mongoClient("apibook")
  }

  def fetch(n: Int) = {
    val projects = ActorMaster.collectProjects(n)
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
