package com.cppdo.apibook.index

import java.io.File
import java.nio.file.Paths
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.document.Field
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search._
import org.apache.lucene.util.Version

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
  val indexDirectory = "data"
  val fieldName = "Name"

  object FieldName extends Enumeration {
    type FieldName = Value

    val Name, DbId, EnclosingClassDbId, FieldNames, Signature, Parameters, // field names in this line is deprecated
      Type, MethodName, ClassName, ClassFullName, MethodFullName, CanonicalName, ParameterTypes, ParameterNames,
      CommentText, ReturnType= Value
  }

  object DocumentType extends Enumeration {
    type DocumentType = Value

    val Class, Method = Value
  }

  private def openIndexDirectory(path: String) = {
    // for lucene 5.x
    FSDirectory.open(Paths.get(indexDirectory))
    // for lucene 4.x
    //FSDirectory.open(new File(path))
  }

  private def createIndexWriterConfig(analyzer: Analyzer) = {
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

  def buildIndex(classNodes: Seq[ClassNode]) = {
    val directory = openIndexDirectory(indexDirectory)
    val analyzer = new SourceCodeAnalyzer()
    val indexWriterConfig = createIndexWriterConfig(analyzer)
    val indexWriter = new IndexWriter(directory, indexWriterConfig)
    val documents = classNodes.map(buildDocument(_))
    indexWriter.addDocuments(documents.asJava)
    indexWriter.close()
  }

  def addDocuments(documents: Seq[Document]) = {
    val directory = openIndexDirectory(indexDirectory)
    val analyzer = new SourceCodeAnalyzer()
    val indexWriterConfig = createIndexWriterConfig(analyzer)
    val indexWriter = new IndexWriter(directory, indexWriterConfig)
    indexWriter.addDocuments(documents.asJava)
    indexWriter.close()
  }

  def search(queryText: String, count: Int = 10): Seq[Document] = {
    val directory = openIndexDirectory(indexDirectory)
    val reader = DirectoryReader.open(directory)
    val searcher = new IndexSearcher(reader)
    val analyzer = new SourceCodeAnalyzer()
    val parser = new QueryParser(fieldName, analyzer)
    val booleanQuery = new BooleanQuery()
    //val q = new MatchAllDocsQuery()
    val query = parser.parse(queryText)
    val topDocs = searcher.search(query, count)

    //val topDocs = searcher.search(q, null, 100)
    println("Total hits: " + topDocs.totalHits)
    topDocs.scoreDocs.map(scoreDoc => {
      searcher.doc(scoreDoc.doc)
    })
  }

  def trivial_search(queryText: String): Seq[Document] = {
    val directory = openIndexDirectory(indexDirectory)
    val reader = DirectoryReader.open(directory)
    val searcher = new IndexSearcher(reader)

    val terms = queryText.split(" ")

    val booleanQuery = new BooleanQuery()

    terms.foreach(term => {
      Array(FieldName.Name, FieldName.Type, FieldName.FieldNames).foreach(name => {
        val query = new TermQuery(new Term(name.toString, term))
        booleanQuery.add(query, BooleanClause.Occur.SHOULD)
      })
    })
    //val q = new MatchAllDocsQuery()
    val topDocs = searcher.search(booleanQuery, 100)

    //val topDocs = searcher.search(q, null, 100)
    println("Total hits: " + topDocs.totalHits)
    topDocs.scoreDocs.map(scoreDoc => {
      searcher.doc(scoreDoc.doc)
    })
  }

  def buildBooleanQuery(queryText: String) = {
    val terms = queryText.split(" ")

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
  def searchMethod(queryText: String, canonicalName: Option[String] = None, typeFullName: Option[String] = None): Seq[(Document, Float)] = {
    val directory = openIndexDirectory(indexDirectory)
    val reader = DirectoryReader.open(directory)
    val searcher = new IndexSearcher(reader)

    val booleanQuery = buildBooleanQuery(queryText: String)
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
    val topDocs = searcher.search(booleanQuery, 100)

    //val topDocs = searcher.search(q, null, 100)
    println("Total hits: " + topDocs.totalHits)
    topDocs.scoreDocs.map(scoreDoc => {
      (searcher.doc(scoreDoc.doc), scoreDoc.score)
    })
  }

  private def buildDocument(classNode: ClassNode): Document = {
    val document = new Document
    val nameField = new TextField(fieldName, classNode.name, Field.Store.YES)
    document.add(nameField)
    document
  }

  def buildDocument(klass: Class): Document = {
    val document = new Document
    val nameField = new TextField(FieldName.Name.toString, klass.fullName, Field.Store.YES)
    val typeField = new StringField(FieldName.Type.toString, DocumentType.Class.toString, Field.Store.YES)
    val dbIdField = new StoredField(FieldName.DbId.toString, klass.id.get)
    val fieldNamesField = new TextField(FieldName.FieldNames.toString, klass.fieldNames, Field.Store.NO)
    document.add(nameField)
    document.add(typeField)
    document.add(dbIdField)
    document.add(fieldNamesField)
    document
  }

  def buildDocument(method: Method): Document = {
    val document = new Document
    val nameField = new TextField(FieldName.Name.toString, method.name, Field.Store.YES)
    val typeField = new StringField(FieldName.Type.toString, DocumentType.Method.toString, Field.Store.YES)
    val dbIdField = new StoredField(FieldName.DbId.toString, method.id.get)
    val signatureField = new TextField(FieldName.Signature.toString, method.signature, Field.Store.YES)
    val parameterField = new TextField(FieldName.Parameters.toString, method.parameters, Field.Store.YES)
    val enclosingClassDbIdField = new StoredField(FieldName.EnclosingClassDbId.toString, method.enclosingClassId)
    document.add(nameField)
    document.add(typeField)
    document.add(dbIdField)
    document.add(enclosingClassDbIdField)
    document.add(signatureField)
    document.add(parameterField)
    document
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
      document.add(parameterField)
      document.add(commentField)
    })
    //document.add(parameterField)
    document
  }
}
