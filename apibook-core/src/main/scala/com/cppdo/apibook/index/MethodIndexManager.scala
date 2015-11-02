package com.cppdo.apibook.index

import com.cppdo.apibook.db.{MethodInfo, CodeMethod, CodeClass}
import com.cppdo.apibook.index.IndexManager.{DocumentType, FieldName}
import com.cppdo.apibook.db.{Field => ClassField, Class, Method, CodeMethod, CodeClass, MethodInfo}
import com.cppdo.apibook.db.Imports._
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.document.{TextField, Field, StringField, Document}
import org.apache.lucene.index.{Term, DirectoryReader, IndexWriter}
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.BooleanQuery.Builder
import org.apache.lucene.search._
import org.objectweb.asm.tree.ClassNode

/**
 * Created by song on 10/31/15.
 */
class MethodIndexManager(indexDirectory: String) extends IndexManager(indexDirectory) {

  override def buildDocument(codeClass: CodeClass, codeMethod: CodeMethod, methodInfo: Option[MethodInfo]): Document = {
    val document = new Document
    // fields for identify
    val canonicalNameField = new StringField(FieldName.CanonicalName.toString, codeMethod.canonicalName, Field.Store.YES)

    // fields for scoring

    val methodNameField = new TextField(FieldName.MethodName.toString, codeMethod.name, Field.Store.YES)
    methodNameField.setBoost(2.0F)
    val classFullNameField = new TextField(FieldName.ClassFullName.toString, codeMethod.typeFullName, Field.Store.YES)

    val parameterTypesField = new TextField(FieldName.ParameterTypes.toString, codeMethod.parameterTypes.mkString(" "),
      Field.Store.YES)
    val returnTypeField = new TextField(FieldName.ReturnType.toString, codeMethod.returnType, Field.Store.YES)

    document.add(canonicalNameField)
    document.add(methodNameField)
    document.add(classFullNameField)
    document.add(parameterTypesField)
    document.add(returnTypeField)


    methodInfo.foreach(info => {
      val parameterNames = info.parameters.map(_.name)
      val parameterField = new TextField(FieldName.ParameterNames.toString, parameterNames.mkString(" "), Field.Store.YES)
      val commentField = new TextField(FieldName.CommentText.toString, info.commentText, Field.Store.YES)
      val parameterTagsField = new TextField(FieldName.ParameterTags.toString,
        info.parameterTags.map(_.text).mkString(" "), Field.Store.YES)
      document.add(parameterField)
      document.add(commentField)
      document.add(parameterTagsField)
      info.returnTag.foreach(returnTag => {
        val returnTagField = new TextField(FieldName.ReturnTags.toString,
          returnTag.text, Field.Store.YES)
        document.add(returnTagField)
      })
    })
    //document.add(parameterField)
    document
  }

  def searchMethodV2(queryText: String, maxCount: Int, explain: Boolean = false) = {
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
      val blendedTermQueryBuilder = new BlendedTermQuery.Builder()
      Array(FieldName.MethodName, FieldName.ClassFullName,
        FieldName.ParameterTypes, FieldName.ReturnType, FieldName.ParameterNames,
        FieldName.CommentText, FieldName.ParameterTags, FieldName.ReturnTags).foreach(name => {
        blendedTermQueryBuilder.add(new Term(name.toString, term))
      })
      booleanQueryBuilder.add(blendedTermQueryBuilder.build(), BooleanClause.Occur.SHOULD)
    })
    booleanQueryBuilder.build()
  }
}
