package com.cppdo.apibook.search

import com.cppdo.apibook.db.CodeMongoDb
import com.cppdo.apibook.index.IndexManager
import com.cppdo.apibook.nlp.CoreNLP
import com.typesafe.scalalogging.LazyLogging

/**
 * Created by song on 10/13/15.
 */
class SearchManager(mongoHost: String, mongoDatabase: String) extends LazyLogging{

  val db = new CodeMongoDb(mongoHost, mongoDatabase)

  def searchMethod(searchText: String): Seq[String] = {
    val posMap = CoreNLP.getPOSMap(searchText)
    val db = new CodeMongoDb("localhost", "apibook")
    logger.info(posMap.toString)
    var verbs = Seq[String]()
    var nouns = Seq[String]()
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
        else false
      })
    })
    nouns.foreach(noun => {
      val codeClasses = db.findClassOfName(noun)
      codeClasses.foreach(codeClass => {
        val usages = db.getClassUsage(codeClass.fullName)
        println(s"${codeClass.fullName}:${usages.size}")
        val relatedClasses = db.findClassRelated(codeClass.fullName)
      })
    })
    val filteredQueryText = filteredTokens.mkString(" ")
    logger.info(s"Filtered QueryText: ${filteredQueryText}")
    val methodNames = IndexManager.searchMethod(filteredQueryText).map(docScored => {
      val (document, _) = docScored
      document.get(IndexManager.FieldName.FullName.toString)
    })
    methodNames
  }

  def close() = {
    db.close()
  }
}
