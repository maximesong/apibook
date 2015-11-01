package com.cppdo.apibook.index

import java.io.File
import java.nio.file.Paths
import com.cppdo.apibook.index.IndexManager.{DocumentType, FieldName}
import com.typesafe.scalalogging.LazyLogging
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.en.KStemmer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.document.Field
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search._
import org.apache.lucene.util.Version
import org.tartarus.snowball.ext.PorterStemmer

import scala.collection.JavaConverters._
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document._
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.index._
import org.apache.lucene.store.FSDirectory
import org.objectweb.asm.tree.ClassNode
import com.cppdo.apibook.db.{Field => ClassField, Class, Method, CodeMethod, CodeClass, MethodInfo}
import com.cppdo.apibook.db.Imports._
import com.typesafe.config.ConfigFactory

/**
 * Created by song on 3/4/15.
 */

object IndexManager {
  object FieldName extends Enumeration {
    type FieldName = Value

    val Name, DbId, EnclosingClassDbId, FieldNames, Signature, Parameters, // field names in this line is deprecated
    Type, MethodName, ClassName, ClassFullName, MethodFullName, CanonicalName, ParameterTypes, ParameterNames,
    CommentText, ReturnType, ParameterTags, ReturnTags = Value
  }

  object DocumentType extends Enumeration {
    type DocumentType = Value

    val Class, Method = Value
  }
}
class IndexManager(indexDirectory: String) extends LazyLogging{
  val fieldName = "Name"

  protected def openIndexDirectory(path: String) = {
    // for lucene 5.x
    FSDirectory.open(Paths.get(indexDirectory))
    // for lucene 4.x
    //FSDirectory.open(new File(path))
  }

  protected def createIndexWriterConfig(analyzer: Analyzer) = {
    // for lucene 5.x
    val indexWriterConfig = new IndexWriterConfig(analyzer)
    // for lucene 4.x
    // val indexWriterConfig = new IndexWriterConfig(Version.LUCENE_CURRENT, analyzer)
    indexWriterConfig.setOpenMode(OpenMode.CREATE_OR_APPEND)
    indexWriterConfig
  }

  def createIndexWriter() = {
    val directory = openIndexDirectory(indexDirectory)
    val analyzer = new SourceCodeAnalyzer()
    val indexWriterConfig = createIndexWriterConfig(analyzer)
    val indexWriter = new IndexWriter(directory, indexWriterConfig)
    indexWriter
  }

  def addDocuments(documents: Seq[Document]) = {
    val directory = openIndexDirectory(indexDirectory)
    val indexWriter = createIndexWriter()
    indexWriter.addDocuments(documents.asJava)
    indexWriter.close()
  }

  def buildBooleanQuery(terms: Seq[String]) = {
    val booleanQuery = new BooleanQuery()

    terms.foreach(term => {
      Array(FieldName.MethodName, FieldName.ClassName, FieldName.ParameterTypes,
        FieldName.ReturnType, FieldName.ParameterNames, FieldName.CommentText).foreach(name => {
        val query = new TermQuery(new Term(name.toString, term))
        booleanQuery.add(query, BooleanClause.Occur.SHOULD)
      })
    })
    booleanQuery
  }
  def searchMethod(queryText: String, n: Int = 10000, canonicalName: Option[String] = None, typeFullName: Option[String] = None): Seq[ScoredDocument] = {
    val directory = openIndexDirectory(indexDirectory)
    val reader = DirectoryReader.open(directory)
    val searcher = new IndexSearcher(reader)
    val analyzer = new SourceCodeAnalyzer()
    /*
    val queryParser = new QueryParser(null, analyzer)
    val q = queryParser.createBooleanQuery("a", "iterate a file running runs HashMap iteration")
    println(q.toString)
    */
    val stream = analyzer.tokenStream(null, queryText)
    val cattr = stream.addAttribute(classOf[CharTermAttribute])
    stream.reset()
    var analyzedTerms = Seq[String]()
    while (stream.incrementToken()) {
      analyzedTerms :+= cattr.toString
    }
    stream.end()
    stream.close()
    logger.info(s"analyzed terms:${analyzedTerms.mkString(" ")}")
    val booleanQuery = buildBooleanQuery(analyzedTerms)
    typeFullName.foreach(fullName => {
      val query = new TermQuery(new Term(FieldName.ClassName.toString, fullName))
      booleanQuery.add(query, BooleanClause.Occur.MUST)
    })
    canonicalName.foreach(canonicalName => {
      println(s"search: $canonicalName")
      val query = new TermQuery(new Term(FieldName.CanonicalName.toString, canonicalName))
      booleanQuery.add(query, BooleanClause.Occur.MUST)
    })
    //val q = new MatchAllDocsQuery()
    val topDocs = searcher.search(booleanQuery, n)

    //val topDocs = searcher.search(q, null, 100)
    println("Total hits: " + topDocs.totalHits)
    topDocs.scoreDocs.map(scoreDoc => {
      ScoredDocument(searcher.doc(scoreDoc.doc), scoreDoc.score)
    })
  }


  def buildDocument(codeClass: CodeClass, codeMethod: CodeMethod, methodInfo: Option[MethodInfo]): Document = {
    val document = new Document
    val canonicalNameField = new StringField(FieldName.CanonicalName.toString, codeMethod.canonicalName, Field.Store.YES)
    val methodFullNameField = new StringField(FieldName.MethodFullName.toString, codeMethod.fullName, Field.Store.YES)
    val methodNameField = new TextField(FieldName.MethodName.toString, codeMethod.name, Field.Store.YES)
    val classFullNameField = new StringField(FieldName.ClassFullName.toString, codeMethod.typeFullName, Field.Store.YES)
    val classNameField = new TextField(FieldName.ClassName.toString, codeMethod.typeName, Field.Store.YES)
    val parameterTypesField = new TextField(FieldName.ParameterTypes.toString, codeMethod.parameterTypes.mkString(" "),
        Field.Store.YES)
    val returnTypeField = new TextField(FieldName.ReturnType.toString, codeMethod.returnType, Field.Store.YES)

    val typeField = new StringField(FieldName.Type.toString, DocumentType.Method.toString, Field.Store.YES)
    document.add(canonicalNameField)
    document.add(methodFullNameField)
    document.add(methodNameField)
    document.add(classNameField)
    document.add(classFullNameField)
    document.add(parameterTypesField)
    document.add(returnTypeField)
    document.add(typeField)


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
}

case class ScoredDocument(document: Document, score: Float)
