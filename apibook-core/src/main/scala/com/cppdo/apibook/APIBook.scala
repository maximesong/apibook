package com.cppdo.apibook

import java.io.{FileNotFoundException, File}
import java.net.URL
import java.nio.file.{Path, Paths, Files}
import java.util.Properties

import akka.actor.{PoisonPill, Props, ActorSystem}
import com.cppdo.apibook.ast.{AstTreeManager, ClassVisitor, JarManager}
import com.cppdo.apibook.db._
import com.cppdo.apibook.forum.{ExperimentQuestion, StackOverflowMongoDb, StackOverflowCrawler}
import com.cppdo.apibook.index.{MethodNameIndexManager, MethodIndexManager, MethodTypesIndexManager, IndexManager}
import com.cppdo.apibook.index.IndexManager.FieldName
import com.cppdo.apibook.repository.{GitHubRepositoryManager, ArtifactsManager, MavenRepository}
import com.cppdo.apibook.repository.ArtifactsManager.RichArtifact
import com.cppdo.apibook.repository.MavenRepository.{MavenArtifact, MavenArtifactSeq, MavenProject}
import com.cppdo.apibook.search.{MethodDetailScore, MethodScore, SearchManager}
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
import org.apache.commons.io.filefilter.{DirectoryFileFilter, TrueFileFilter, FalseFileFilter}
import org.apache.lucene.document.Document
import org.apache.lucene.search.SearcherManager
import org.joda.time.{Period, Duration, Instant}
import org.objectweb.asm.Type

import scala.io.Source

import scala.collection.JavaConverters._

import sys.process._
//import slick.driver.H2Driver.api._
import com.mongodb.casbah.Imports._


/**
 * Created by song on 1/17/15.
 */
object APIBook extends LazyLogging {

  case class Config(mode: String = "", n: Option[Int] = None, outputPath: Option[String] = None, begin: Int = 0,
                   overwrite: Option[Boolean] = None, rebuild: Boolean=false,
                    directory: Option[String] = None, file: Option[String] = None, repository: Option[String] = None,
                    explain: Boolean = false, append: Boolean = false, evaluateTypes: Boolean = false, verbose: Boolean = false,
                     args: Seq[String] = Seq(), dbHost: String = "localhost", dbName: String = "apibook")
  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[Config]("apibook") {
      head("APIBook", "1.0")

      opt[Int]('n', "number") action {
        (n, c) => c.copy(n=Some(n))
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
      opt[String]('o', "output") action {
        (output, c) => c.copy(outputPath=Some(output))
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
      opt[Boolean]("overwrite") action {
        (overwrite, c) => c.copy(overwrite=Some(overwrite))
      }
      opt[Unit]("rebuild") action {
        (_, c) => c.copy(rebuild=true)
      }
      opt[Unit]("explain") action {
        (_, c) => c.copy(explain=true)
      }
      opt[Unit]('v', "verbose") action {
        (_, c) => c.copy(verbose=true)
      }
      opt[String]("dbName") action {
        (dbName, c) => c.copy(dbName=dbName)
      }
      opt[String]("dbHost") action {
        (dbHost, c) => c.copy(dbHost=dbHost)
      }
      opt[Unit]('a', "append") action {
        (_, c) => c.copy(append=true)
      }
      opt[Unit]("types") action {
        (_, c) => c.copy(evaluateTypes=true)
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
      cmd("viewUsage") action {
        (_, c) => c.copy(mode="viewUsage")
      }
      cmd("const") action {
        (_, c) => c.copy(mode="const")
      }
      cmd("info") action {
        (_, c) => c.copy(mode="info")
      }
      cmd("methodNameIndex") action {
        (_, c) => c.copy(mode="methodNameIndex")
      }
      cmd("methodIndex") action {
        (_, c) => c.copy(mode="methodIndex")
      }
      cmd("methodTypesIndex") action {
        (_, c) => c.copy(mode="methodTypesIndex")
      }
      cmd("review") action {
        (_, c) => c.copy(mode="review")
      }
      cmd("search") action {
        (_, c) => c.copy(mode="search")
      }
      cmd("searchV2") action {
        (_, c) => c.copy(mode="searchV2")
      }
      cmd("searchMethod") action {
        (_, c) => c.copy(mode="searchMethod")
      }
      cmd("searchMethodTypes") action {
        (_, c) => c.copy(mode="searchMethodTypes")
      }
      cmd("download") action {
        (_, c) => c.copy(mode="download")
      }
      cmd("extract") action {
        (_, c) => c.copy(mode="extract")
      }
      cmd("location") action {
        (_, c) => c.copy(mode="location")
      }
      cmd("snippet") action {
        (_, c) => c.copy(mode="snippet")
      }
      cmd("countJar") action {
        (_, c) => c.copy(mode="countJar")
      }
      cmd("evaluate") action {
        (_, c) => c.copy(mode="evaluate")
      }
      cmd("evaluateTypes") action {
        (_, c) => c.copy(mode="evaluateTypes")
      }
      cmd("questionList") action {
        (_, c) => c.copy(mode="questionList")
      }
      arg[String]("<arg>...") optional() unbounded() action {
        (arg, c) => c.copy(args=c.args :+ arg)
      }
    }
    parser.parse(args, Config()) match {
      case Some(config) => {
        logger.info("Hi")
        config.mode match {
          case "test" => test()
          case "stackoverflow" => stackoverflow(config)
          case "class" => buildClasses(config)
          case "usage" => updateUsage(config)
          case "viewUsage" => viewUsage(config)
          case "find" => find(config)
          case "const" => buildConstant(config)
          case "info" => buildFromDoc(config)
          //case "index" => buildIndex(config)
          case "methodNameIndex" => buildMethodNameIndex(config)
          case "methodTypesIndex" => buildMethodTypesIndex(config)
          case "methodIndex" => buildMethodIndex(config)
          case "search" => search(config)
          case "searchV2" => searchV2(config)
          case "searchMethodTypes" => searchMethodTypes(config)
          case "searchMethod" => searchMethod(config)
          case "review" => review(config)
          case "artifact" => buildArtifacts(config)
          case "download" => download(config)
          case "extract" => extract(config)
          case "location" => buildLocation(config)
          case "snippet" => calculateSnippets(config)
          case "evaluate" => evaluate(config)
          case "countJar" => countJar(config)
          case "questionList" => questionList(config)
          case "evaluateTypes" => evaluateTypes(config)
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

  def evaluateTypes(config:Config) = {
    val experimentDb = new StackOverflowMongoDb(config.dbHost, config.dbName)
    val nonsenseIds = Seq(
      216894 // Get an OutputStream into a String
    )
    val questions = experimentDb.getExperimentQuestions().filter(q => !nonsenseIds.contains(q.stackOverflowQuestionId))
    val implicitTypeNum = questions.map(_.implicitTypes.size).sum
    val shortNameTypeNum = questions.map(_.shortNameTypes.size).sum
    val longNameTypeNum = questions.map(_.longNameTypes.size).sum
    val primitiveTypeNum = questions.map(_.primitiveTypes.size).sum
    val arrayTypeNum = questions.map(_.arrayTypes.size).sum
    val totalTypeNum = implicitTypeNum + shortNameTypeNum + longNameTypeNum + primitiveTypeNum + arrayTypeNum

    val typeNumQuestions = questions.groupBy(_.typeNum)



    val manager = new SearchManager(config.dbHost, config.dbName)
    typeNumQuestions.foreach(group => {
      println(s"${group._1} types: ${group._2.size}")
    })
    println("Zero Types:")
    questions.filter(_.typeNum == 0).foreach(x => println(x.question))
    println("More than 2 Types:")
    questions.filter(_.typeNum > 2).foreach(x => println(x.question))

    val statistics = questions.map(question => {
      val typesFound = manager.findTypes(question.question)
      val shortNameQuestionTypes = question.types.map(_.split("[.]").last)
      println(shortNameQuestionTypes)
      val shortNameTypesFound = typesFound.map(_.split("[.]").last)
      val matchedQuestionTypeCount = question.types.count(typesFound.contains)
      val recall = if (question.types.size != 0) {
        matchedQuestionTypeCount.toDouble / question.types.size
      } else {
        Double.NaN
      }
      val correctTypeMatching = shortNameTypesFound.count(shortNameQuestionTypes.contains)
      val precision = if (typesFound.size != 0) {
        correctTypeMatching.toDouble / typesFound.size
      } else {
        Double.NaN
      }
      if (question.primitiveTypes.exists(t => !typesFound.contains(t))) {
        println(s"!!!!!!!!----- ${question.question}")
      }
      if (question.shortNameTypes.exists(t => !typesFound.contains(t))) {
        println(s"!!!!!!!!----- ${question.question}")
      }
      println(question.question)
      println(s"Question Types: ${question.types}")
      println(s"Found Types: ${typesFound}")
      println(s"precision: ${precision}, recall: ${recall}")
      println(s"Array Types: ${question.arrayTypes.count(typesFound.contains)}")

      (matchedQuestionTypeCount, question.types.size, correctTypeMatching, typesFound.size,
        question.shortNameTypes.count(typesFound.contains),
        question.longNameTypes.count(typesFound.contains),
        question.primitiveTypes.count(typesFound.contains),
        question.arrayTypes.count(typesFound.contains),
        question.implicitTypes.count(typesFound.contains))
    })

    val total = statistics.reduce((x, y) => {
      (x._1 + y._1, x._2 + y._2, x._3 + y._3, x._4 + y._4,
        x._5 + y._5, x._6 + y._6, x._7 + y._7, x._8 + y._8, x._9 + y._9)
    })
    val shortNameRecall = total._5 / questions.map(_.shortNameTypes.size).sum.toDouble
    val longNameRecall = total._6 / questions.map(_.longNameTypes.size).sum.toDouble
    val primitiveRecall = total._7 / questions.map(_.primitiveTypes.size).sum.toDouble
    val arrayRecall = total._8 / questions.map(_.arrayTypes.size).sum.toDouble
    val implicitRecall = total._9 / questions.map(_.implicitTypes.size).sum.toDouble

    println(total)
    val recall = total._1.toDouble / total._2
    val precision = total._3.toDouble / total._4
    println(s"implicit: ${implicitTypeNum}")
    println(s"short name: ${shortNameTypeNum}")
    println(s"long name: ${longNameTypeNum}")
    println(s"primitives: ${primitiveTypeNum}")
    println(s"array: ${arrayTypeNum}")
    println(s"total precision:, ${precision}, total recal:${recall}")
    println(s"short recall:, ${shortNameRecall}, long recal:${longNameRecall}, primitiveRecall: ${primitiveRecall}, arrayRecall: ${arrayRecall}, implicitRecall: ${implicitRecall}")

  }

  def questionList(config: Config): Unit = {
    val nonsenseIds = Seq(
      216894 // Get an OutputStream into a String
    )
    val experimentDb = new StackOverflowMongoDb(config.dbHost, config.dbName)
    val questions = experimentDb.getExperimentQuestions().filter(q => !nonsenseIds.contains(q.stackOverflowQuestionId))

    if (config.args.nonEmpty) {
      val outputPath = config.args(0)
      val writer = CSVWriter.open(outputPath)
      writer.writeRow(List("Question ID", "Title"))
      questions.foreach(question => {
        writer.writeRow(List(question.stackOverflowQuestionId, question.question))
        logger.info(question.question)
      })
      writer.close()
    }

  }

  def evaluate(config: Config): Unit = {
    case class Evaluation(question: ExperimentQuestion, strongRank: Option[Int] = None, weakRank: Option[Int] = None)
    val searchManager = new SearchManager(config.dbHost, config.dbName)
    val experimentDb = new StackOverflowMongoDb(config.dbHost, config.dbName)
    val shouldQuestionIds = Seq(
      4716503, //Best way to read a text file in Java?

      617414, //Create a temporary directory in Java
      1102891, //How to check if a String is a numeric type
      869033 //How do I copy an object in Java?
    )
    val hardQuestionIds = Seq(
      18571223, //How to convert Java String into byte[]?
      80476, //How can I concatenate two arrays in Java?
      1128723, //What's the simplest way to print a Java array?
      3802893, //Number of days between two dates in Joda-Time
      51438, // Getting A File's Mime Type In Java
      921262, //How to download and save a file from Internet using Java?
      189559, //How do I join two lists in Java?

      858980, //File to byte[] in Java
      3758606, //How to convert byte size into human readable format in java?
      1625234, // How to append text to an existing file in Java//
      363681, // Generating random integers in a range with Java
      2298208, // How do I discover memory usage of my application in Android?
      2885173, // How to create a file and write to a file in Java
      5374311, // Convert ArrayList<String> to String[]
      835889 // java.util.Date to XMLGregorianCalendar
    )
    val nonsenseIds = Seq(
      216894 // Get an OutputStream into a String
    )
    val questions = experimentDb.getExperimentQuestions().filter(q => !nonsenseIds.contains(q.stackOverflowQuestionId))
    val searchCount = config.n.getOrElse(100)
    val searchEngineWithEvaluations = config.args.map(searchEngine => {
      var start = Instant.now()
      val evaluations = questions.map(question => {
        logger.info(s"Evaluating question '${question.question}'....")
        val strongCanonicalNames = question.reviews.filter(_.relevance == "strong").map(_.canonicalName)
        val weakCanonicalNames = question.reviews.filter(_.relevance == "weak").map(_.canonicalName)
        val methods = if (searchEngine == "V2") {
          searchManager.searchV2(question.question, searchCount)
        } else if (searchEngine == "V0") {
          searchManager.searchV0(question.question, searchCount)
        } else if (searchEngine == "V1") {
          searchManager.searchV1(question.question, searchCount)
        } else if (searchEngine == "GodMode") {
          searchManager.searchGodMode(question.question, question.types, searchCount)
        } else {
          logger.warn(s"Search Engine: ${searchEngine} not found.")
          return
        }
        var rank = 1
        var evaluation = Evaluation(question)
        methods.foreach(method => {
          if (strongCanonicalNames.contains(method.codeMethod.canonicalName) && evaluation.strongRank.isEmpty) {
            evaluation = evaluation.copy(strongRank =  Some(rank))
          }
          else if (weakCanonicalNames.contains(method.codeMethod.canonicalName) && evaluation.weakRank.isEmpty) {
            evaluation = evaluation.copy(weakRank =  Some(rank))
          }
          rank += 1
        })
        evaluation
      }).toList
      val millis = Instant.now().getMillis - start.getMillis
      (searchEngine, evaluations, millis)
    }).toList

    searchEngineWithEvaluations.foreach { case (searchEngine, evaluations, millis) => {
      println(s"\n########## ${searchEngine} ----------------")
      val noStrongRelevanceQuestions = evaluations.filter(_.strongRank.isEmpty).map(_.question)
      val noRelevanceQuestions = evaluations.filter(e => e.strongRank.isEmpty && e.weakRank.isEmpty).map(_.question)
      val notInTop20Evaluations = evaluations.filter(e => e.strongRank.exists(_ <= 20))

      val topNs = Set(3, 5, 10, 20, 50, searchCount).toSeq.sorted
      topNs.foreach(topN => {
        val matchedEvaluations = evaluations.filter(_.strongRank.exists(_ <= topN))
        val averageRank = if (matchedEvaluations.nonEmpty) {
          matchedEvaluations.map(_.strongRank.get).sum.toDouble / matchedEvaluations.size
        }
        else {
          Double.NaN
        }
        println(s"Top ${topN} matches: ${matchedEvaluations.size.toDouble/evaluations.size}(${matchedEvaluations.size}/${evaluations.size}), average rank: ${averageRank}")
      })
      val mrr = evaluations.filter(_.strongRank.nonEmpty).map(e => {
        1.0 / e.strongRank.get
      }).sum / evaluations.size
      println(s"------ MRR: ${mrr} -----")
      println("------FOR GNUPLOT-----")
      Range(10, 110, 10).foreach(topN => {
        val matchedEvaluations = evaluations.filter(_.strongRank.exists(_ <= topN))
        println(f"${topN} ${matchedEvaluations.size.toDouble/evaluations.size*100}%.1f")
      })
      println("----------------------")
      println(s"Time Millis: ${millis}, Average: ${millis / evaluations.size}")
      println("-------------------------------------------\n")
      if (config.evaluateTypes) {
        // Evaluation by type Num
        evaluations.groupBy(e => {
          if (e.question.typeNum > 2) {
            2
          } else {
            e.question.typeNum
          }
        }).foreach(group => {
          println("----------------------")
          println(s"Type Num: ${group._1}")
          println("-------------------------------------------\n")
          topNs.foreach(topN => {
            val matchedEvaluations = group._2.filter(_.strongRank.exists(_ <= topN))
            val averageRank = if (matchedEvaluations.nonEmpty) {
              matchedEvaluations.map(_.strongRank.get).sum.toDouble / matchedEvaluations.size
            }
            else {
              Double.NaN
            }
            println(s"Top ${topN} matches: ${matchedEvaluations.size.toDouble / group._2.size}(${matchedEvaluations.size}/${group._2.size}), average rank: ${averageRank}")
          })
          val mrr = group._2.filter(_.strongRank.nonEmpty).map(e => {
            1.0 / e.strongRank.get
          }).sum / group._2.size
          println(s"------ MRR: ${mrr} -----")
          println("------FOR GNUPLOT-----")
          Range(10, 110, 10).foreach(topN => {
            val matchedEvaluations = group._2.filter(_.strongRank.exists(_ <= topN))
            println(f"${topN} ${matchedEvaluations.size.toDouble/group._2.size*100}%.1f")
          })
        })
      }
      if (config.verbose) {
        println(s"No Strong Questions: ${noStrongRelevanceQuestions.size}")
        Range(10, 110, 10).foreach(topN => {
          val matchedEvaluations = evaluations.filter(_.strongRank.exists(x => x > topN - 10 && x <= topN))
          println(s"--- ${topN} ---")
          matchedEvaluations.foreach(e => println(e.question.question))
        })
        noStrongRelevanceQuestions.foreach(q => {
          //println(q.question)
        })
      }
    }}
  }

  def viewUsage(config: Config) = {
    val db = new CodeMongoDb(config.dbHost, config.dbName)
    val methods = config.args.flatMap(methodFullName => {
      db.findMethodsWithFullName(methodFullName)
    })
    val usageCounts = db.getUsageCounts(methods.map(_.canonicalName))
    usageCounts.foreach{ case (canonicalName, count) => {
      println(s"${canonicalName}: ${count}")
    }}
  }

  def calculateSnippets(config: Config) = {
    val manager = new SearchManager(config.dbHost, config.dbName)
    config.args.foreach(methodFullName => {
      val snippets = manager.findUsageSnippets(methodFullName)
    })
  }

  def buildSourceLocation(db: CodeMongoDb, sourcePath: String) = {
    val cu = AstTreeManager.getCompilationUnit(sourcePath)
    val packageDeclaration = AstTreeManager.packageDeclarationOf(cu)
    val typeDeclarations = AstTreeManager.typeDeclarationsOf(cu)
    val packageName = packageDeclaration.getName.toString
    typeDeclarations.foreach(typeDeclaration => {
      val typeFullName = s"${packageName}.${typeDeclaration.getName}"
      println(typeFullName)
      db.upsertClassArtifact(typeFullName, ClassArtifacts.sourceCodeFilePath, sourcePath)
    })
  }

  def buildLocation(config: Config) = {
    val db = new CodeMongoDb(config.dbHost, config.dbName)
    config.args.foreach(path => {
      recursiveActOn(path, "java", buildSourceLocation(db, _))
      recursiveActOn(path, "jar", buildJarLocation(db, _))
    })
  }

  def buildJarLocation(db: CodeMongoDb, jarPath: String) = {
    val classNodes = JarManager.getClassNodes(jarPath)
    classNodes.foreach(classNode => {
      val codeClass = AstTreeManager.buildCodeClass(classNode)
      println(codeClass.fullName)
      db.upsertClassArtifact(codeClass.fullName, ClassArtifacts.byteCodeJarPath, jarPath)
    })
  }

  def extract(config: Config) = {
    val outputPath = config.outputPath.getOrElse("repository-sources")
    config.args.foreach(path => {
      logger.info(path)
      recursiveActOn(path, "jar", jarPath => {
        logger.info(s"Extracting ${jarPath}...")
        JarManager.extractJar(jarPath, outputPath, ".java")
        JarManager.extractJar(jarPath, outputPath, ".class")
      })
    })
  }

  def download(config: Config) = {
    val outputPath = config.outputPath.getOrElse("thirdparty")
    val projectNum = config.n.getOrElse(100)
    val overwrite = config.overwrite.getOrElse(false)
    val projects = MavenRepository.getTopProjects(projectNum)
    var count = 1
    var missingArtifacts = Seq[String]()
    projects.foreach(project => {
      val artifact = MavenRepository.collectArtifactsOf(project).takeLatestVersion
      artifact.foreach(artifact => {
        val sourceDestination = new File(s"${outputPath}/src/${artifact.simpleSourcePackageName}")
        val libraryDestination = new File(s"${outputPath}/lib/${artifact.simpleLibraryPackageName}")
        val docDestination = new File(s"${outputPath}/doc/${artifact.simpleDocPackageName}")

        Seq(
          (artifact.sourcePackageUrl, sourceDestination),
          (artifact.libraryPackageUrl, libraryDestination),
          (artifact.docPackageUrl, docDestination)
        ).foreach {case (url, destination) => {
          if (overwrite || !destination.exists()) {
            println(s"Downloading[${count}/${projectNum}] ${destination}...")
            try {
              FileUtils.copyURLToFile(new URL(url), destination)
            } catch {
              case _: FileNotFoundException => {
                logger.warn(s"${url} Not Found")
                missingArtifacts :+= url
              }
            }
          } else {
            println(s"${destination.getAbsolutePath} exists.")
          }
        }}
        count += 1
      })
    })
    if (!missingArtifacts.isEmpty) {
      println("## Missing Artifacts:")
      missingArtifacts.foreach(url => {
        println(url)
      })
    }
  }

  def recursiveActOn(path: String, extension: String, fn: String => Unit) = {
    val f = new File(path)
    if (f.isDirectory) {
      logger.info("is directory")
      val files = FileUtils.listFiles(f, Array(extension), true)
      files.asScala.foreach(file => {
        fn(file.getAbsolutePath)
      })
    } else {
      logger.info("is file")
      fn(f.getAbsolutePath)
    }
  }

  def buildArtifacts(config: Config) = {
    val db = new CodeMongoDb(config.dbHost, config.dbName)
    config.args.foreach(path => {
      recursiveActOn(path, "jar", jarPath => {
        if (jarPath.contains("javadoc")) {

        } else if (jarPath.contains("source")) {

        }
      })
    })
  }
  def review(config: Config) = {
    val stackoverflowDb = new StackOverflowMongoDb("localhost","apibook")
    val questionReviews = stackoverflowDb.getQuestionReviews()
    val questionCount = stackoverflowDb.getQuestions().size
    var programTaskCounts = 0
    var questionReviewed = 0
    var answersUsingAPI = 0
    var singleAPICount = 0
    questionReviews.foreach(review => {
      questionReviewed += 1
      if (review.isProgramTask.contains(true)) {
        programTaskCounts += 1
      }
      if (review.singleKeyApi.contains(true)) {
        singleAPICount += 1
      }
      if (review.answerIdUsingApi.exists(_ > 0)) {
        answersUsingAPI += 1
      }
    })
    println(s"Reviewed: ${questionReviewed}/${questionCount}")
    println(s"Program Task: ${programTaskCounts}/${questionReviewed}")
    println(s"Has answer using APIs: ${answersUsingAPI}/${programTaskCounts}")
    println(s"Has answer using single API: ${singleAPICount}/${answersUsingAPI}")

    val methodReviews = stackoverflowDb.getQuestionMethodReviews()
    methodReviews.foreach(review => {
      println(s"${review.questionId}: ${review.relevance.get}")
    })
  }


  def buildMethodNameIndex(config: Config) = {
    val indexDirectory = config.outputPath.getOrElse("methodNameIndex")
    if (!config.append) {
      logger.info("removing old index...")
      FileUtils.deleteDirectory(new File(indexDirectory))
    }
    val indexManager = new MethodNameIndexManager(indexDirectory)
    val db = new CodeMongoDb(config.dbHost, config.dbName)
    val cachedSize = 10000
    val codeClasses = db.getCodeClasses()
    var documentsToAdd = Seq[Document]()
    codeClasses.foreach(codeClass => {
      println(codeClass.fullName)
      val documents = codeClass.methods.map(method => {
        val methodInfo = db.getMethodInfo(method.canonicalName)
        indexManager.buildDocument(codeClass, method, methodInfo)
      })
      documentsToAdd ++= documents
      if (documentsToAdd.size > cachedSize) {
        logger.info("Add documents...")
        indexManager.addDocuments(documentsToAdd)
        documentsToAdd = Seq[Document]()
      }
    })
    indexManager.addDocuments(documentsToAdd)
    db.close()
  }

  def buildMethodIndex(config: Config) = {
    val indexDirectory = config.outputPath.getOrElse("methodIndex")
    if (!config.append) {
      logger.info("removing old index...")
      FileUtils.deleteDirectory(new File(indexDirectory))
    }
    val indexManager = new MethodIndexManager(indexDirectory)
    val db = new CodeMongoDb(config.dbHost, config.dbName)
    val cachedSize = 10000
    val codeClasses = db.getCodeClasses()
    var documentsToAdd = Seq[Document]()
    codeClasses.foreach(codeClass => {
      println(codeClass.fullName)
      val documents = codeClass.methods.map(method => {
        val methodInfo = db.getMethodInfo(method.canonicalName)
        indexManager.buildDocument(codeClass, method, methodInfo)
      })
      documentsToAdd ++= documents
      if (documentsToAdd.size > cachedSize) {
        logger.info("Add documents...")
        indexManager.addDocuments(documentsToAdd)
        documentsToAdd = Seq[Document]()
      }
    })
    indexManager.addDocuments(documentsToAdd)
    db.close()
  }

  def buildMethodTypesIndex(config: Config) = {
    val indexDirectory = config.outputPath.getOrElse("methodTypeIndex")
    if (!config.append) {
      logger.info("removing old index...")
      FileUtils.deleteDirectory(new File(indexDirectory))
    }
    val indexManager = new MethodTypesIndexManager(indexDirectory)
    val db = new CodeMongoDb(config.dbHost, config.dbName)
    val cachedSize = 10000
    val codeClasses = db.getCodeClasses()
    var documentsToAdd = Seq[Document]()
    codeClasses.foreach(codeClass => {
      println(codeClass.fullName)
      val documents = codeClass.methods.map(method => {
        val methodInfo = db.getMethodInfo(method.canonicalName)
        indexManager.buildDocument(codeClass, method, methodInfo)
      })
      documentsToAdd ++= documents
      if (documentsToAdd.size > cachedSize) {
        logger.info("Add documents...")
        indexManager.addDocuments(documentsToAdd)
        documentsToAdd = Seq[Document]()
      }
    })
    indexManager.addDocuments(documentsToAdd)
    db.close()
  }

  def buildFromDoc(config: Config) = {
    val db = new CodeMongoDb(config.dbHost, config.dbName)
    if (config.rebuild) {
      logger.info("Removing info...")
      db.removeAllMethodInfo()
    }
    config.args.foreach(path => {
      val root = new File(path)
      val directories = root.list(DirectoryFileFilter.DIRECTORY).map(subDirectory => s"${root.getAbsolutePath}/${subDirectory}")
      directories.foreach(directory => {
        println(directory)
        val files = FileUtils.listFiles(new File(directory), Array("java"), true)
        val fileNames = files.asScala.map(_.getAbsolutePath)
        val args = Seq("-doclet", "com.cppdo.apibook.doc.StoreDoc") ++ fileNames
        JavaDocMain.execute(args: _*)
      })
    })
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
    val db = new CodeMongoDb(config.dbHost, config.dbName)
    if (config.rebuild) {
      logger.info("Removing classes and methods...")
      db.removeAllClassesAndMethods()
    }
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

  def countJar(config: Config): Unit = {
    config.args.foreach(path => {
      val classNodes = JarManager.getClassNodes(path)
      var classCount = 0
      var methodCount = 0
      classNodes.foreach(classNode => {
        classCount += 1
        val codeClass = AstTreeManager.buildCodeClass(classNode)
        codeClass.methods.foreach(method => {
          methodCount += 1
        })
      })
      logger.info(s"${path}\tclass: ${classCount} method: ${methodCount}")
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
      val invocations = AstTreeManager.calculateUsage(classNode)
      invocations.foreach(invocation => {
        db.upsertMethodInvocation(invocation)
      })
    })
  }

  def updateUsage(config: Config): Unit = {
    //val classNodes = JarManager.getClassNodes("/Users/song/Projects/apibook/repository/junit/junit/4.12/junit-4.12.jar")
    //val runtimeJarPath = "/Users/song/Projects/apibook/java/rt.jar"
    //val classNodes = JarManager.getClassNodes(runtimeJarPath)
    val db = new CodeMongoDb(config.dbHost, config.dbName)

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
    println("ArrayList<String>".matches(".*[<].*[>].*"))
  }

  def stackoverflow(config: Config): Unit = {
    val overviews = StackOverflowCrawler.fetchQuestionOverviews(config.n.getOrElse(20))
    val mongoClient = new StackOverflowMongoDb("localhost", "apibook")
    val waitTime = 1 * 100
    if (config.args.length < 1) {
      logger.error("ERROR: should specify output path.")
      return
    }
    val outputPath = config.args(0)
    val writer = CSVWriter.open(outputPath)
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
    logger.info(s"Write to ${outputPath}")
    writer.close()
  }

  def saveQuestionSummariesToMongo(count: Int) = {
    val mongoClient = MongoClient("localhost", 27017)
    val db = mongoClient("apibook")
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
    val manager = new SearchManager(config.dbHost, config.dbName)
    val methodNames = manager.searchMethod(searchText)
    //println(methodNames.mkString("\n"))
  }

  def searchV2(config: Config) = {
    val maxCount = config.n.getOrElse(50)
    val searchText = config.args.mkString(" ")
    val manager = new SearchManager(config.dbHost, config.dbName)
    val methodDetailScores = manager.searchV2(searchText, maxCount, explain = config.explain)
    var rank = 1
    methodDetailScores.foreach(detailScore => {
      println(s"${rank}. ${detailScore.codeMethod.canonicalName}: ${detailScore.score.score}")
      println(s"\t${detailScore.score.explain}")
      rank += 1
    })
  }

  def searchMethod(config: Config) = {
    val maxCount = config.n.getOrElse(50)
    val manager = new SearchManager(config.dbHost, config.dbName)
    val methodScores = manager.search(config.args.mkString(" "), maxCount)
    methodScores.foreach(methodScore => {
      println(s"${methodScore.codeMethod.canonicalName}: ${methodScore.value}")
    })
  }

  def searchMethodTypes(config: Config) = {
    val maxCount = config.n.getOrElse(50)
    val manager = new SearchManager(config.dbHost, config.dbName)
    val methodScores = manager.searchMethodTypes(config.args, maxCount, config.explain)
    methodScores.foreach(methodScore => {
      println(s"${methodScore.codeMethod.canonicalName}: ${methodScore.value}")
    })
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
