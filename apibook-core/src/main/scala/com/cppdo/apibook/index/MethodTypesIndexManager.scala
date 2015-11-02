package com.cppdo.apibook.index

import com.cppdo.apibook.db.{MethodInfo, CodeMethod, CodeClass}
import com.cppdo.apibook.index.IndexManager.{DocumentType, FieldName}
import org.apache.lucene.analysis.core.WhitespaceAnalyzer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.document.{TextField, Field, StringField, Document}
import org.apache.lucene.index.{IndexWriter, Term, DirectoryReader}
import org.apache.lucene.search.{BooleanQuery, BooleanClause, TermQuery, IndexSearcher}

/**
 * Created by song on 10/31/15.
 */
class MethodTypesIndexManager(indexDirectory: String) extends IndexManager(indexDirectory) {
  override def buildDocument(codeClass: CodeClass, codeMethod: CodeMethod, methodInfo: Option[MethodInfo]) = {
    val document = new Document
    // fields for identify
    val canonicalNameField = new StringField(FieldName.CanonicalName.toString, codeMethod.canonicalName, Field.Store.YES)
    val methodFullNameField = new StringField(FieldName.MethodFullName.toString, codeMethod.fullName, Field.Store.YES)

    // fields for scoring
    val classFullNameField = new StringField(FieldName.ClassFullName.toString, codeMethod.typeFullName, Field.Store.YES)
    val parameterTypesField = new TextField(FieldName.ParameterTypes.toString, codeMethod.parameterTypes.mkString(" "),
      Field.Store.YES)
    val returnTypeField = new StringField(FieldName.ReturnType.toString, codeMethod.returnType, Field.Store.YES)

    document.add(canonicalNameField)
    document.add(methodFullNameField)
    document.add(classFullNameField)
    document.add(parameterTypesField)
    document.add(returnTypeField)
    document
  }

  override def createIndexWriter() = {
    val directory = openIndexDirectory(indexDirectory)
    val analyzer = new WhitespaceAnalyzer()
    val indexWriterConfig = createIndexWriterConfig(analyzer)
    indexWriterConfig.setSimilarity(new MethodTypesSimilarity)
    val indexWriter = new IndexWriter(directory, indexWriterConfig)
    indexWriter
  }

  override def buildBooleanQuery(terms: Seq[String]) = {
    val booleanQuery = new BooleanQuery()

    terms.foreach(term => {
      Array(FieldName.ClassFullName, FieldName.ParameterTypes,
        FieldName.ReturnType).foreach(name => {
        val query = new TermQuery(new Term(name.toString, term))
        booleanQuery.add(query, BooleanClause.Occur.SHOULD)
      })
    })
    booleanQuery
  }

  def searchMethodTypes(typeFullNames: Seq[String], n: Int = 10000, explain: Boolean = false): Seq[ScoredDocument] = {
    val directory = openIndexDirectory(indexDirectory)
    val reader = DirectoryReader.open(directory)
    val searcher = new IndexSearcher(reader)
    searcher.setSimilarity(new MethodTypesSimilarity)

    val booleanQuery = buildBooleanQuery(typeFullNames)

    val topDocs = searcher.search(booleanQuery, n)
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
}
