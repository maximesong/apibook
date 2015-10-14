package com.cppdo.apibook

import java.io.{FileNotFoundException, File}
import java.net.URL
import java.nio.file.{Path, Paths, Files}
import java.util.Properties

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
import com.cppdo.apibook.index.IndexManager.FieldName
import com.cppdo.apibook.repository.{GitHubRepositoryManager, ArtifactsManager, MavenRepository}
import com.cppdo.apibook.repository.ArtifactsManager.RichArtifact
import com.cppdo.apibook.repository.MavenRepository.{MavenArtifact, MavenArtifactSeq, MavenProject}
import com.cppdo.apibook.search.SearchManager
import com.github.tototoshi.csv.CSVWriter
import com.mongodb.casbah.MongoClient
import com.sun.tools.javadoc.{Main=>JavaDocMain}
import com.typesafe.scalalogging.LazyLogging
import edu.mit.jwi.Dictionary
import edu.mit.jwi.item.POS
import edu.stanford.nlp.dcoref.CoNLL2011DocumentReader.NamedEntityAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.{PartOfSpeechAnnotation, TextAnnotation, TokensAnnotation, SentencesAnnotation}
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation
import edu.stanford.nlp.util.CoreMap
import org.apache.commons.io.FileUtils
import org.apache.lucene.document.Document
import org.objectweb.asm.Type
import com.novus.salat._
import com.novus.salat.global._
import org.jsoup.Jsoup
import org.objectweb.asm.tree.ClassNode
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
                    directory: Option[String] = None, file: Option[String] = None, repository: Option[String] = None,
                     args: Seq[String] = Seq())
  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[Config]("apibook") {
      head("APIBook", "1.0")

      opt[Int]('n', "number") action {
        (n, c) => c.copy(n=n)
      }
      opt[Unit]("rt") action {
        (_, c) => c.copy(file=Some("java/rt.jar"))
      }
      opt[Unit]("std") action {
        (_, c) => c.copy(directory=Some("java/classses"))
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
      opt[String]('r', "repository") action {
        (repository, c) => c.copy(repository=Some(repository))
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
      cmd("class") action {
        (_, c) => c.copy(mode="class")
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
      cmd("info") action {
        (_, c) => c.copy(mode="info")
      }
      cmd("index") action {
        (_, c) => c.copy(mode="index")
      }
      cmd("search") action {
        (_, c) => c.copy(mode="search")
      }
      arg[String]("<arg>...") optional() unbounded() action {
        (arg, c) => c.copy(args=c.args :+ arg)
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
          case "class" => buildClasses(config)
          case "usage" => updateUsage(config)
          case "find" => find(config)
          case "const" => buildConstant(config)
          case "info" => buildFromDoc(config)
          case "index" => buildMethodIndex(config)
          case "search" => search(config)
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

  def buildMethodIndex(config: Config) = {
    val db = new CodeMongoDb("localhost","apibook")
    val cachedSize = 10000
    val codeClasses = db.getCodeClasses()
    var documentsToAdd = Seq[Document]()
    codeClasses.foreach(codeClass => {
      println(codeClass.fullName)
      val documents = codeClass.methods.map(method => {
        val methodInfo = db.getMethodInfo(method.canonicalName)
        IndexManager.buildDocument(codeClass, method, methodInfo)
      })
      documentsToAdd ++= documents
      if (documentsToAdd.size > cachedSize) {
        logger.info("Add documents...")
        IndexManager.addDocuments(documentsToAdd)
        documentsToAdd = Seq[Document]()
      }
    })
    IndexManager.addDocuments(documentsToAdd)
    db.close()
  }

  def buildFromDoc(config: Config) = {
    config.directory.foreach(directory => {
      val files = FileUtils.listFiles(new File(directory), Array("java"), true)
      val fileNames = files.asScala.map(_.getAbsolutePath)
      //val args = Array("-doclet", "com.cppdo.apibook.doc.StoreDoc", "*.java")
      val args = Seq("-doclet", "com.cppdo.apibook.doc.StoreDoc") ++ fileNames
      JavaDocMain.execute(args: _*)
    })
    config.file.foreach(file => {
      val args = Seq("-doclet", "com.cppdo.apibook.doc.StoreDoc", file)
      JavaDocMain.execute(args: _*)
    })
    config.repository.foreach(repository => {
      val jarSourceFiles = FileUtils.listFiles(new File(repository), Array("jar"), true)
        .asScala.filter(file => file.getName.contains("-sources.jar"))
      jarSourceFiles.foreach(file => {
        val optionalDirectory = JarManager.extractSource(file.getAbsolutePath)
        optionalDirectory.foreach(directory => {
          val files = FileUtils.listFiles(new File(directory), Array("java"), true)
          val fileNames = files.asScala.map(_.getAbsolutePath)
          //val args = Array("-doclet", "com.cppdo.apibook.doc.StoreDoc", "*.java")
          val args = Seq("-doclet", "com.cppdo.apibook.doc.StoreDoc") ++ fileNames
          JavaDocMain.execute(args: _*)
        })
      })
    })
    //val args = Array("-doclet", "com.cppdo.apibook.doc.StoreDoc", "/Users/song/Projects/apibook/repository/junit/junit/4.12/junit-4.12-sources/org/junit/runners/JUnit4.java")
  }

  def buildClasses(db: CodeMongoDb, jarPath: String): Unit = {
    logger.info(jarPath)
    val classNodes = JarManager.getClassNodes(jarPath)
    classNodes.foreach(classNode => {
      val codeClass = AstTreeManager.buildCodeClass(classNode)
      db.upsertClass(codeClass)
      codeClass.methods.foreach(method => {
        db.upsertMethod(method)
      })
    })
  }
  def buildClasses(config: Config): Unit = {
    val db = new CodeMongoDb("localhost","apibook")
    config.args.foreach(path => {
      val file = new File(path)
      if (file.isDirectory) {
        val files = FileUtils.listFiles(file, Array("jar"), true)
        files.asScala.foreach(jarFile => {
          buildClasses(db, jarFile.getAbsolutePath)

        })
      } else if (file.isFile) {
        buildClasses(db, file.getAbsolutePath)
      }
    })
  }

  def buildConstant(config: Config) = {
    val classNodes = JarManager.getClassNodes("/Users/song/Projects/apibook/repository/junit/junit/4.12/junit-4.12.jar")
    classNodes.foreach(classNode => {
      AstTreeManager.calculateConstantParameter(classNode)
    })

  }

  def updateUsage(db: CodeMongoDb, jarPath: String): Unit = {
    val classNodes = JarManager.getClassNodes(jarPath)
    classNodes.foreach(classNode => {
      val classType = Type.getObjectType(classNode.name)
      logger.info(s"${classType.getClassName}...")
      val (typeSet, methodSet) = AstTreeManager.calculateUsage(classNode)
      methodSet.foreach(method => {
        db.upsertMethodUsage(method, classType.getClassName)
      })
      typeSet.foreach(t => {
        db.upsertClassUsage(t, classType.getClassName)
      })
    })
  }

  def updateUsage(config: Config): Unit = {
    //val classNodes = JarManager.getClassNodes("/Users/song/Projects/apibook/repository/junit/junit/4.12/junit-4.12.jar")
    //val runtimeJarPath = "/Users/song/Projects/apibook/java/rt.jar"
    //val classNodes = JarManager.getClassNodes(runtimeJarPath)
    val db = new CodeMongoDb("localhost","apibook")

    config.args.foreach(path => {
      val file = new File(path)
      if (file.isDirectory) {
        val files = FileUtils.listFiles(file, Array("jar"), true)
        files.asScala.foreach(file => {
          updateUsage(db, file.getAbsolutePath)
        })
      } else if (file.isFile) {
        updateUsage(db, file.getAbsolutePath)
      }
    })
    db.close()
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
          println(method.fullName)
        }
      })
    })
    println(classNodes.size)
    val targetClass = db.findClassReturn(to)

  }

  def test() = {
    val docs = IndexManager.searchMethod("iterate HashMap")
    println(docs.size)
    docs.foreach(scoredDoc => {
        println(scoredDoc.document.get(FieldName.CanonicalName.toString), scoredDoc.score)
    })
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
  def search(config: Config) = {
    val searchText = config.args.mkString(" ")
    val manager = new SearchManager("localhost", "apibook")
    val methodNames = manager.searchMethod(searchText)
    //println(methodNames.mkString("\n"))
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
