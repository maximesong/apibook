package com.cppdo.apibook.search

import com.cppdo.apibook.ast.{AstTreeManager, JarManager}
import com.cppdo.apibook.db.{CodeClass, CodeMethod, CodeMongoDb}
import com.cppdo.apibook.index.IndexManager
import com.cppdo.apibook.index.IndexManager.FieldName
import com.cppdo.apibook.nlp.CoreNLP
import com.typesafe.scalalogging.LazyLogging
import scala.collection.JavaConverters._
import play.api.libs.json.{JsArray, Json, JsValue}

/**
 * Created by song on 10/13/15.
 */
case class MethodScore(codeMethod: CodeMethod, value: Float)

class SearchManager(mongoHost: String, mongoDatabase: String, classLoader: Option[ClassLoader] = None) extends LazyLogging{

  val db = new CodeMongoDb(mongoHost, mongoDatabase, classLoader = classLoader)

  def toJson(methodScores: Seq[MethodScore]): Seq[JsValue] = {
    methodScores.map(methodScore => {
      //Json.parse(grater[MethodScore].toPrettyJSON(methodScore))
      //JsValue()
      ""
    })
    Seq[JsValue]()
  }

  def findUsageSnippets(methodFullName: String) = {
    val typeFullNames = db.getMethodUsage(methodFullName)
    typeFullNames.foreach(typeFullName => {
      val optionalArtifacts = db.getClassArtifacts(typeFullName)
      optionalArtifacts.foreach(artifacts => {
        println(typeFullName)
        if (artifacts.byteCodeJarPath.nonEmpty && artifacts.sourceCodeFilePath.nonEmpty) {
          val classNodes = JarManager.getClassNodes(artifacts.byteCodeJarPath.get)
          classNodes.foreach(classNode => {


          })
          artifacts.sourceCodeFilePath.get
        }
        println(artifacts.byteCodeJarPath)
        println(artifacts.sourceCodeFilePath)
      })
    })
  }

  def searchMethodAndReturnJson(searchText: String, n:Int = 1000): Seq[JsValue] = {
    val methodScores = searchMethod(searchText, n)
    methodScores.map(score => {
      Json.parse(db.toJson(score))
    })
  }
  def searchMethod(searchText: String, n: Int = 1000): Seq[MethodScore] = {
    case class ClassScore(codeClass: CodeClass, value: Float)
    case class ScorePair(key: String, value: Float)
    val posMap = CoreNLP.getPOSMap(searchText)
    logger.info(posMap.toString)
    var verbs = Seq[String]()
    var nouns = Seq[String]()
    var adjs = Seq[String]()
    val filteredTokens = searchText.split(" ").filter(token => {
      posMap.get(token).exists(tag => {
        if (tag.startsWith("VB")) {
          verbs :+= token
          true
        }
        else if (tag.startsWith("NN")) {
          nouns :+= token
          true
        }
        else if (tag.startsWith("JJ")) {
          adjs :+= token
          true
        }
        else false
      })
    })
    val filteredQueryText = filteredTokens.mkString(" ")
    logger.info(s"Filtered QueryText: ${filteredQueryText}")
    val matchingScores = IndexManager.searchMethod(filteredQueryText).map(scoredDoc => {
      if (scoredDoc.document.get(FieldName.CanonicalName.toString).startsWith("java.util.HashMap")) {
        //println(scoredDoc.document.get(FieldName.CanonicalName.toString), scoredDoc.score)
      }
      ScorePair(scoredDoc.document.get(FieldName.CanonicalName.toString), scoredDoc.score)
    })
    val mentionedNounTypes = nouns.map(noun => (noun, db.findClassOfName(noun))).filter(_._2.nonEmpty)
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
