package com.cppdo.apibook.index

import java.nio.file.Paths
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.{TermQuery, BooleanQuery, MatchAllDocsQuery, IndexSearcher}

import scala.collection.JavaConverters._
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document._
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.index._
import org.apache.lucene.store.FSDirectory
import org.objectweb.asm.tree.ClassNode
import com.cppdo.apibook.db.{Class, Method}
import com.typesafe.config.ConfigFactory

/**
 * Created by song on 3/4/15.
 */
object IndexManager {
  val indexDirectory = "data"
  val fieldName = "name"

  object FieldName extends Enumeration {
    type FieldName = Value

    val Name, Type, DbId, EnclosingClassDbId, FieldNames = Value
  }

  object DocumentType extends Enumeration {
    type DocumentType = Value

    val Class, Method = Value
  }
  def buildIndex(classNodes: Seq[ClassNode]) = {
    val directory = FSDirectory.open(Paths.get(indexDirectory))
    val analyzer = new StandardAnalyzer()
    val indexWriterConfig = new IndexWriterConfig(analyzer)
    indexWriterConfig.setOpenMode(OpenMode.CREATE_OR_APPEND)
    val indexWriter = new IndexWriter(directory, indexWriterConfig)
    val documents = classNodes.map(buildDocument(_))
    indexWriter.addDocuments(documents.asJava)
    indexWriter.close()
  }

  def buildIndex(classNode: ClassNode) = {
    val directory = FSDirectory.open(Paths.get(indexDirectory))
    val analyzer = new StandardAnalyzer()
    val indexWriterConfig = new IndexWriterConfig(analyzer)
    indexWriterConfig.setOpenMode(OpenMode.CREATE)
    val indexWriter = new IndexWriter(directory, indexWriterConfig)
    val document = buildDocument(classNode)
    indexWriter.addDocument(document)
    indexWriter.close()
  }


  def search(queryText: String): Seq[Document] = {
    val directory = FSDirectory.open(Paths.get(indexDirectory))
    val reader = DirectoryReader.open(directory)
    val searcher = new IndexSearcher(reader)
    val analyzer = new StandardAnalyzer()
    val parser = new QueryParser(fieldName, analyzer)
    val booleanQuery = new BooleanQuery()
    //val q = new MatchAllDocsQuery()
    val query = parser.parse(queryText)
    val topDocs = searcher.search(query, null, 100)

    //val topDocs = searcher.search(q, null, 100)
    println("Total hits: " + topDocs.totalHits)
    topDocs.scoreDocs.map(scoreDoc => {
      searcher.doc(scoreDoc.doc)
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
    val enclosingClassDbIdField = new StoredField(FieldName.EnclosingClassDbId.toString, method.enclosingClassId)
    document.add(nameField)
    document.add(typeField)
    document.add(dbIdField)
    document.add(enclosingClassDbIdField)
    document
  }
}
