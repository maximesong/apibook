package com.cppdo.apibook.index

import com.cppdo.apibook.db.{MethodInfo, CodeMethod, CodeClass}
import com.cppdo.apibook.db.Imports._
import com.cppdo.apibook.index.IndexManager.FieldName
import org.apache.lucene.document.{TextField, Field, StringField, Document}
import org.apache.lucene.index.{Term, DirectoryReader}
import org.apache.lucene.search._

/**
 * Created by song on 11/4/15.
 */
class MethodNameIndexManager(indexDirectory: String) extends IndexManager(indexDirectory) {
  override def buildDocument(codeClass: CodeClass, codeMethod: CodeMethod, methodInfo: Option[MethodInfo]): Document = {
    val document = new Document
    // fields for identify
    val canonicalNameField = new StringField(FieldName.CanonicalName.toString, codeMethod.canonicalName, Field.Store.YES)

    // fields for scoring

    val methodNameField = new TextField(FieldName.MethodFullName.toString, codeMethod.fullName, Field.Store.YES)

    document.add(canonicalNameField)
    document.add(methodNameField)

    document
  }

  def searchMethod(queryText: String, maxCount: Int = 1000, explain: Boolean = false) = {
    val directory = openIndexDirectory(indexDirectory)
    val reader = DirectoryReader.open(directory)
    val searcher = new IndexSearcher(reader)

    val analyzedTerms = tokenizeQueryText(queryText, new SourceCodeAnalyzer())
    logger.info(s"analyzed terms:${analyzedTerms.mkString(" ")}")
    val booleanQuery = buildBooleanQuery(analyzedTerms)

    val topDocs = searcher.search(booleanQuery, maxCount)
    if (explain) {
      logger.info(s"Explain search method types:")
      topDocs.scoreDocs.foreach(scoreDoc => {
        val explanation = searcher.explain(booleanQuery, scoreDoc.doc)
        println(explanation)
      })
    }

    println("Total hits: " + topDocs.totalHits)
    topDocs.scoreDocs.map(scoreDoc => {
      ScoredDocument(searcher.doc(scoreDoc.doc), scoreDoc.score)
    })
  }

  override def buildBooleanQuery(terms: Seq[String]) = {
    val booleanQueryBuilder = new BooleanQuery.Builder()

    terms.foreach(term => {
        booleanQueryBuilder.add(new TermQuery(new Term(FieldName.MethodFullName.toString, term)), BooleanClause.Occur.SHOULD)
    })
    booleanQueryBuilder.build()
  }
}
