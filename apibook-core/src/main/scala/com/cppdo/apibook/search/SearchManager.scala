package com.cppdo.apibook.search

import com.cppdo.apibook.ast.{AstTreeManager, JarManager}
import com.cppdo.apibook.db.{CodeClass, CodeMethod, CodeMongoDb}
import com.cppdo.apibook.index.{MethodNameIndexManager, MethodIndexManager, MethodTypesIndexManager, IndexManager}
import com.cppdo.apibook.index.IndexManager.FieldName
import com.cppdo.apibook.nlp.CoreNLP
import com.typesafe.scalalogging.LazyLogging
import scala.collection.JavaConverters._
import play.api.libs.json.{JsArray, Json, JsValue}

/**
 * Created by song on 10/13/15.
 */

case class Score(methodScore: Float = 0, methodTypesScore: Float = 0, usageScore: Float = 0) {
  val baseScore = 0.1F
  val baseUsageScore = 1F
  def score = (baseScore + methodScore) * (baseScore + methodTypesScore) *  (baseScore + usageScore)
  def explain = s"score(${score}) = methodScore(${baseScore + methodScore}) * methodTypesScore(${baseScore + methodTypesScore}) *  usageScore(${baseUsageScore + usageScore})"
}

case class MethodDetailScore(codeMethod: CodeMethod, score: Score)

case class MethodScore(codeMethod: CodeMethod, value: Float)

class SearchManager(mongoHost: String, mongoDatabase: String,
                    methodNameIndexDirectory: String = "methodNameIndex",
                    methodIndexDirectory: String = "methodIndex", methodTypesIndexDirectory: String = "methodTypeIndex",
                    classLoader: Option[ClassLoader] = None) extends LazyLogging{

  val db = new CodeMongoDb(mongoHost, mongoDatabase, classLoader = classLoader)
  val indexDirectory = "data"
  val indexManager = new IndexManager(indexDirectory)
  val methodNameIndexManager = new MethodNameIndexManager(methodNameIndexDirectory)
  val methodIndexManager = new MethodIndexManager(methodIndexDirectory)
  val methodTypesIndexManager = new MethodTypesIndexManager(methodTypesIndexDirectory)

  def toJson(methodScores: Seq[MethodScore]): Seq[JsValue] = {
    methodScores.map(methodScore => {
      //Json.parse(grater[MethodScore].toPrettyJSON(methodScore))
      //JsValue()
      ""
    })
    Seq[JsValue]()
  }

  def getCodeOf(codeMethod: CodeMethod): Option[String] = {
    val classArtifacts = db.getClassArtifacts(codeMethod.typeFullName)
    classArtifacts.flatMap(artifacts => {
      logger.info(artifacts.toString)
      artifacts.sourceCodeFilePath.flatMap(sourceCodeFilePath => {
        println(sourceCodeFilePath)
        val cu = AstTreeManager.getCompilationUnit(sourceCodeFilePath)
        val packageDeclaration = AstTreeManager.packageDeclarationOf(cu)
        val packageName = packageDeclaration.getName.toString
        val typeDeclarations = AstTreeManager.typeDeclarationsOf(cu)
        val codeSnippet = typeDeclarations.find(typeDeclaration => {
          s"${packageName}.${typeDeclaration.getName.toString}" == codeMethod.typeFullName
        }).flatMap(typeDeclaration => {
          typeDeclaration.getMethods.find(methodDeclaration => {
            s"${packageName}.${typeDeclaration.getName.toString}.${methodDeclaration.getName.toString}" == codeMethod.fullName
          }).map(methodDeclaration => {
            methodDeclaration.toString
          })
        })
        codeSnippet
      })
    })
  }

  def findUsageSnippets(methodFullName: String) = {
    val codeMethods = db.findMethodsWithFullName(methodFullName)
    codeMethods.foreach(codeMethod => {
      logger.info(s"Usage for: ${codeMethod.canonicalName}")
      val invocations = db.findMethodInvocations(codeMethod.canonicalName)
      val invokedByMethods = db.getCodeMethods(invocations.map(_.invokedByCanonicalName))
      invokedByMethods.foreach(method => {
        logger.info(method.fullName)
        getCodeOf(method)
      })
    })
  }

  def findUsageSnippetsOfCanonicalName(canonicalName: String, n: Int = 10): Seq[String] = {
    logger.info(canonicalName)
    val codeMethod = db.getCodeMethod(canonicalName)
    logger.info(codeMethod.get.fullName)
    logger.info(codeMethod.get.canonicalName)
    codeMethod.map(codeMethod => {
      val invocations = db.findMethodInvocations(codeMethod.canonicalName)
      logger.info(invocations.size.toString)
      // FIXME: getExistingCodeMethods should be replaced with getCodeMethods, just workaround for a bug
      val invokedByMethods = db.getExistingCodeMethods(invocations.map(_.invokedByCanonicalName))
      invokedByMethods.take(n).flatMap(method => {
        getCodeOf(method)
      })
    }).getOrElse(Seq[String]())
  }

  def searchMethodAndReturnJson(searchText: String, n:Int = 1000): Seq[JsValue] = {
    val methodScores = searchMethod(searchText, n)
    methodScores.map(score => {
      Json.parse(db.toJson(score))
    })
  }

  def searchAndReturnJson(queryText: String, n: Int = 1000, searchEngine: String, explain: Boolean = false): Seq[JsValue] = {
    val methodScores = if (searchEngine.equals("V2")) {
      searchV2(queryText, n, explain = explain)
    } else if (searchEngine.equals("V0")) {
      searchV0(queryText, n, explain = explain)
    } else if (searchEngine == "V1") {
      searchV1(queryText, n, explain = explain)
    } else if (searchEngine == "GodMode") {
      searchV2(queryText, n, explain = explain)
    }
    else {
      logger.warn(s"Engine: ${searchEngine} not known. Use default v2.")
      searchV2(queryText, n, explain = explain)
    }
    methodScores.map(score => {
      Json.parse(db.toJson(score))
    })
  }

  def searchGodModeAndReturnJson(queryText: String, types: Seq[String], n: Int = 1000, searchEngine: String, explain: Boolean = false): Seq[JsValue] = {
    val methodScores = searchGodMode(queryText, types, n, explain = explain)
    methodScores.map(score => {
      Json.parse(db.toJson(score))
    })
  }


  def searchMethodTypes(typeFullNames: Seq[String], maxCount: Int = 3000, explain: Boolean = false): Seq[MethodScore] = {
    val scoredDocuments = methodTypesIndexManager.searchMethodTypes(typeFullNames, maxCount, explain)
    val canonicalNames = scoredDocuments.map(_.document.get(FieldName.CanonicalName.toString))
    val codeMethods = db.getCodeMethods(canonicalNames)
    val scores = scoredDocuments.map(_.score)
    codeMethods.zip(scores).map(pair => {
      MethodScore(pair._1, pair._2)
    })
  }

  def searchMethod(searchText: String, n: Int = 1000, explain: Boolean = false): Seq[MethodScore] = {
    val posMap = CoreNLP.getPOSMap(searchText)
    val tokens = searchText.split(" ")
    val groupedTokens = groupTokens(tokens, posMap)
    val filteredTokens = searchText.split(" ").filter(token => {
      groupedTokens.nouns.contains(token) || groupedTokens.verbs.contains(token) || groupedTokens.adjs.contains(token)
    })
    val filteredQueryText = filteredTokens.mkString(" ")
    logger.info(posMap.toString)
    logger.info(s"Filtered QueryText: ${filteredQueryText}")
    val scoredDocuments = methodIndexManager.searchMethod(filteredQueryText, n, explain=explain)
    val canonicalNames = scoredDocuments.map(_.document.get(FieldName.CanonicalName.toString))
    val codeMethods = db.getCodeMethods(canonicalNames)
    val scores = scoredDocuments.map(_.score)
    codeMethods.zip(scores).map(pair => {
      MethodScore(pair._1, pair._2)
    })
  }

  def searchV0(searchText: String, n: Int = 1000, explain: Boolean = false): Seq[MethodDetailScore] = {
    println("############## Search V0 ----------------")
    val cleanedSearchText = searchText.replaceAll("[?]", "")
    val posMap = CoreNLP.getPOSMap(cleanedSearchText)
    val tokens = cleanedSearchText.split(" ")
    val groupedTokens = groupTokens(tokens, posMap)
    val filteredTokens = cleanedSearchText.split(" ").filter(token => {
      groupedTokens.nouns.contains(token) || groupedTokens.verbs.contains(token) || groupedTokens.adjs.contains(token)
    })
    val filteredQueryText = filteredTokens.mkString(" ")
    val methodScores = methodNameIndexManager.searchMethod(filteredQueryText, n, explain=explain)
    var scores = Map[String, Score]()
    methodScores.foreach(scoredMethod => {
      val canonicalName = scoredMethod.document.get(FieldName.CanonicalName.toString)
      if (scores.contains(canonicalName)) {
        scores += canonicalName -> scores(canonicalName).copy(methodScore = scoredMethod.score)
      } else {
        scores += canonicalName -> Score(methodScore = scoredMethod.score)
      }
    })
    val sortedScores = scores.toSeq.sortBy(-_._2.score)
    val codeMethods = db.getCodeMethods(sortedScores.map(_._1))
    codeMethods.zip(sortedScores.map(_._2)).map { case (codeMethod, score) => {
      MethodDetailScore(codeMethod, score)
    }
    }.take(n)
  }

  def searchV1(searchText: String, n: Int = 1000, explain: Boolean = false): Seq[MethodDetailScore] = {
    println("############## Search V1 ----------------")
    val cleanedSearchText = searchText.replaceAll("[?]", "")
    val posMap = CoreNLP.getPOSMap(cleanedSearchText)
    val tokens = cleanedSearchText.split(" ")
    val groupedTokens = groupTokens(tokens, posMap)
    val filteredTokens = cleanedSearchText.split(" ").filter(token => {
      groupedTokens.nouns.contains(token) || groupedTokens.verbs.contains(token) || groupedTokens.adjs.contains(token)
    })
    val filteredQueryText = filteredTokens.mkString(" ")
    val methodScores = methodIndexManager.searchMethod(filteredQueryText, n, explain=explain)
    var scores = Map[String, Score]()
    methodScores.foreach(scoredMethod => {
      val canonicalName = scoredMethod.document.get(FieldName.CanonicalName.toString)
      if (scores.contains(canonicalName)) {
        scores += canonicalName -> scores(canonicalName).copy(methodScore = scoredMethod.score)
      } else {
        scores += canonicalName -> Score(methodScore = scoredMethod.score)
      }
    })
    val sortedScores = scores.toSeq.sortBy(-_._2.score)
    val codeMethods = db.getCodeMethods(sortedScores.map(_._1))
    codeMethods.zip(sortedScores.map(_._2)).map { case (codeMethod, score) => {
      MethodDetailScore(codeMethod, score)
    }
    }.take(n)
  }

  def findTypes(searchText: String): Seq[String] = {
    println("############## Find Types ----------------")
    val cleanedSearchText = searchText.replaceAll("[?]", "")
    val posMap = CoreNLP.getPOSMap(cleanedSearchText)
    val tokens = cleanedSearchText.split(" ")
    val groupedTokens = groupTokens(tokens, posMap)
    val filteredTokens = cleanedSearchText.split(" ").filter(token => {
      groupedTokens.nouns.contains(token) || groupedTokens.verbs.contains(token) || groupedTokens.adjs.contains(token)
    })
    val primitiveTypes = Seq("int", "double", "float", "long", "short", "char", "byte")
    val boxedTypes = Seq("java.lang.Integer", "java.lang.Double", "java.lang.Float", "java.lang.Long", "java.lang.Short", "java.lang.Character", "java.lang.Byte")
    val filteredTypes = Seq(
      "org.apache.tools.ant.taskdefs.Java"
    )
    val nounTypes = groupedTokens.nouns.flatMap(noun => {
      if (primitiveTypes.contains(noun.toLowerCase)) {
        val i = primitiveTypes.indexOf(noun.toLowerCase )
        Seq(noun, boxedTypes(i))
      } else if (noun.equals("string")) { // this may not be reasonable, since query text should write "String"
        Seq("java.lang.String")
      } else if (noun.equals("object")) {
        Seq("java.lang.Object")
      } else if (noun.equals("file")) {
        Seq("java.io.File")
      } else {
        db.findClassesWithName(noun).map(_.fullName)
      }
    })//.filter(!filteredTypes.contains(_))
    nounTypes.toSeq
  }

  def extractPrimitives(types: Seq[String]): Seq[String] = {
    val primitiveTypesMapping = Map(
      "java.lang.Integer" -> "int",
      "java.lang.Double" -> "double",
      "java.lang.Float" -> "float",
      "java.lang.Byte" -> "byte",
      "java.lang.Long" -> "long",
      "java.lang.Character" -> "char"
    )
    val primitives = types.filter(primitiveTypesMapping.contains).map(primitiveTypesMapping)
    primitives
  }
  def searchGodMode(searchText: String, types: Seq[String], n: Int = 1000, fetchFactor: Int = 10, explain: Boolean = false) = {
    println("############## Search GodMode ----------------")
    val cleanedSearchText = searchText.replaceAll("[?]", "")
    val fetchCount = n * fetchFactor
    val posMap = CoreNLP.getPOSMap(cleanedSearchText)
    val tokens = cleanedSearchText.split(" ")
    val groupedTokens = groupTokens(tokens, posMap)
    val filteredTokens = cleanedSearchText.split(" ").filter(token => {
      groupedTokens.nouns.contains(token) || groupedTokens.verbs.contains(token) || groupedTokens.adjs.contains(token)
    })
    val filteredQueryText = filteredTokens.mkString(" ")
    logger.info(posMap.toString)
    logger.info(s"Filtered QueryText: ${filteredQueryText}")

    val nounTypes = types ++ extractPrimitives(types)
    logger.info(nounTypes.toString())
    logger.info("Searching method types....")
    val methodTypesScores = methodTypesIndexManager.searchMethodTypes(nounTypes, fetchCount)
    logger.info("Searching methods....")
    val methodScores = methodIndexManager.searchMethod(filteredQueryText, fetchCount, explain=explain)
    var scores = Map[String, Score]()
    methodTypesScores.foreach(scoredMethod => {
      val canonicalName = scoredMethod.document.get(FieldName.CanonicalName.toString)
      if (scores.contains(canonicalName)) {
        scores += canonicalName -> scores(canonicalName).copy(methodTypesScore = scoredMethod.score)
      } else {
        scores += canonicalName -> Score(methodTypesScore = scoredMethod.score)
      }
    })
    methodScores.foreach(scoredMethod => {
      val canonicalName = scoredMethod.document.get(FieldName.CanonicalName.toString)
      if (scores.contains(canonicalName)) {
        scores += canonicalName -> scores(canonicalName).copy(methodScore = scoredMethod.score)
      } else {
        scores += canonicalName -> Score(methodScore = scoredMethod.score)
      }
    })
    logger.info("Scoring usages....")
    db.getUsageCounts(scores.keys.toSeq).foreach { case (canonicalName, count) =>{
      scores += canonicalName -> scores(canonicalName).copy(usageScore = Math.log10((count + 1).toDouble).toFloat)
    }}
    val sortedScores = scores.toSeq.sortBy(-_._2.score)
    val codeMethods = db.getCodeMethods(sortedScores.map(_._1))
    codeMethods.zip(sortedScores.map(_._2)).map { case (codeMethod, score) => {
      MethodDetailScore(codeMethod, score)
    }
    }.take(n)
  }

  def searchV2(searchText: String, n: Int = 1000, fetchFactor: Int = 10, explain: Boolean = false): Seq[MethodDetailScore] = {
    println("############## Search V2 ----------------")
    val cleanedSearchText = searchText.replaceAll("[?]", "")
    val fetchCount = n * fetchFactor
    val posMap = CoreNLP.getPOSMap(cleanedSearchText)
    val tokens = cleanedSearchText.split(" ")
    val groupedTokens = groupTokens(tokens, posMap)
    val filteredTokens = cleanedSearchText.split(" ").filter(token => {
      groupedTokens.nouns.contains(token) || groupedTokens.verbs.contains(token) || groupedTokens.adjs.contains(token)
    })
    val filteredQueryText = filteredTokens.mkString(" ")
    logger.info(posMap.toString)
    logger.info(s"Filtered QueryText: ${filteredQueryText}")
    val primitiveTypes = Seq("int", "double", "float", "long", "short", "char")
    val boxedTypes = Seq("java.lang.Integer", "java.lang.Double", "java.lang.Float", "java.lang.Long", "java.lang.Short", "java.lang.Character")
    val filteredTypes = Seq(
      "org.apache.tools.ant.taskdefs.Java"
    )
    val nounTypes = groupedTokens.nouns.flatMap(noun => {
      if (primitiveTypes.contains(noun.toLowerCase)) {
        val i = primitiveTypes.indexOf(noun.toLowerCase )
        Seq(noun, boxedTypes(i))
      } else if (noun.equals("string")) { // this may not be reasonable, since query text should write "String"
        Seq("java.lang.String")
      } else if (noun.equals("object")) {
        Seq("java.lang.Object")
      } else if (noun.equals("file1")) {
        Seq("java.io.File")
      } else {
        db.findClassesWithName(noun).map(_.fullName)
      }
    })//.filter(!filteredTypes.contains(_))
    logger.info(nounTypes.toString())
    logger.info("Searching method types....")
    val methodTypesScores = methodTypesIndexManager.searchMethodTypes(nounTypes, fetchCount)
    logger.info("Searching methods....")
    val methodScores = methodIndexManager.searchMethod(filteredQueryText, fetchCount, explain=explain)
    var scores = Map[String, Score]()
    methodTypesScores.foreach(scoredMethod => {
      val canonicalName = scoredMethod.document.get(FieldName.CanonicalName.toString)
      if (scores.contains(canonicalName)) {
        scores += canonicalName -> scores(canonicalName).copy(methodTypesScore = scoredMethod.score)
      } else {
        scores += canonicalName -> Score(methodTypesScore = scoredMethod.score)
      }
    })
    methodScores.foreach(scoredMethod => {
      val canonicalName = scoredMethod.document.get(FieldName.CanonicalName.toString)
      if (scores.contains(canonicalName)) {
        scores += canonicalName -> scores(canonicalName).copy(methodScore = scoredMethod.score)
      } else {
        scores += canonicalName -> Score(methodScore = scoredMethod.score)
      }
    })
    logger.info("Scoring usages....")
    db.getUsageCounts(scores.keys.toSeq).foreach { case (canonicalName, count) =>{
      scores += canonicalName -> scores(canonicalName).copy(usageScore = Math.log10((count + 1).toDouble).toFloat)
    }}
    val sortedScores = scores.toSeq.sortBy(-_._2.score)
    val codeMethods = db.getCodeMethods(sortedScores.map(_._1))
    codeMethods.zip(sortedScores.map(_._2)).map { case (codeMethod, score) => {
        MethodDetailScore(codeMethod, score)
      }
    }.take(n)
  }



  case class GroupedTokens(verbs: Seq[String], nouns: Seq[String], adjs: Seq[String], others: Seq[String])

  def groupTokens(tokens: Seq[String], posMap: Map[String, String]): GroupedTokens = {
    val verbs = tokens.filter(token => {
      posMap.get(token).exists(pos => pos.startsWith("VB"))
    })
    val nouns = tokens.filter(token => {
      posMap.get(token).exists(pos => pos.startsWith("NN")) || (Seq("long", "short").contains(token.toLowerCase) && posMap.get(token).exists(pos => !pos.startsWith("JJ")))
    })
    val adjs = tokens.filter(token => {
      posMap.get(token).exists(pos => pos.startsWith("JJ"))
    })
    val others = tokens.filter(token => {
      (!verbs.contains(token)) && (!nouns.contains(token)) && (!adjs.contains(token))
    })
    GroupedTokens(verbs, nouns, adjs, others)
  }

  def search(searchText: String, n: Int = 1000): Seq[MethodScore] = {
    case class ClassScore(codeClass: CodeClass, value: Float)
    case class ScorePair(key: String, value: Float)
    val posMap = CoreNLP.getPOSMap(searchText)
    val tokens = searchText.split(" ")
    logger.info(posMap.toString)
    val groupedTokens = groupTokens(tokens, posMap)
    val filteredTokens = searchText.split(" ").filter(token => {
      groupedTokens.nouns.contains(token) || groupedTokens.verbs.contains(token) || groupedTokens.adjs.contains(token)
    })
    val filteredQueryText = filteredTokens.mkString(" ")
    logger.info(s"Filtered QueryText: ${filteredQueryText}")
    val matchingScores = methodIndexManager.searchMethod(filteredQueryText, n).map(scoredDoc => {
      if (scoredDoc.document.get(FieldName.CanonicalName.toString).startsWith("java.util.HashMap")) {
        //println(scoredDoc.document.get(FieldName.CanonicalName.toString), scoredDoc.score)
      }
      ScorePair(scoredDoc.document.get(FieldName.CanonicalName.toString), scoredDoc.score)
    })
    val mentionedNounTypes = groupedTokens.nouns.map(noun => (noun, db.findClassesWithName(noun))).filter(_._2.nonEmpty)
    val usingScores = mentionedNounTypes.flatMap(pair => {
      val (noun, codeTypes) = pair
      codeTypes.flatMap(codeClass => {
        val usages = db.getClassUsage(codeClass.fullName).size
        val relatedMethods = db.findMethodsRelated(codeClass.fullName)
        relatedMethods.map(codeMethod => {
          MethodScore(codeMethod, usages.toFloat)
        })
      }).groupBy(_.codeMethod.canonicalName).values.map(_.maxBy(x => x.value))
    }).groupBy(_.codeMethod.canonicalName).values.groupBy(methodUsages => methodUsages.size)
      .flatMap { case (nounCounts, methodUsagesList) => {
        val methodScores = methodUsagesList.map(methodUsages => {
          MethodScore(methodUsages.head.codeMethod, methodUsages.map(_.value).sum)
        })
        val maxScore = methodScores.map(_.value).max
        val normalizedMethodScores = methodScores.map(
          methodScore => MethodScore(methodScore.codeMethod, (methodScore.value + 1) / (maxScore + 1) * nounCounts / mentionedNounTypes.size))
        normalizedMethodScores
      }
    }.map(scoreMethod => ScorePair(scoreMethod.codeMethod.canonicalName, scoreMethod.value))

    val matchedNounCount = mentionedNounTypes.size
    val mentionedTypes = mentionedNounTypes.flatMap(_._2)

    println("################# Using --------------------------------------------\n\n\n")
    usingScores.toSeq.sortBy(-_.value).take(300).foreach(score => {
      println(s"${score.key}: ${score.value}")
    })
    println("################# Matching --------------------------------------------\n\n\n")
    matchingScores.take(300).foreach(score => {
      println(s"${score.key}: ${score.value}")
    })
    val methodScores = (usingScores ++ matchingScores).groupBy(_.key).mapValues(scores => {
      scores.map(_.value).sum
    }).toSeq.sortBy(_._2).reverse
    println("################# Scoring --------------------------------------------\n\n\n")
    methodScores.take(300).foreach(scorePair => {
      val (method, score) = scorePair
      println(method, score)
    })
    val codeMethods = db.getCodeMethods(methodScores.take(n).map(_._1))
    val scores =  methodScores.map(_._2)
    codeMethods.zip(scores).map(pair => MethodScore(pair._1, pair._2))
  }

  def close() = {
    db.close()
  }
}
