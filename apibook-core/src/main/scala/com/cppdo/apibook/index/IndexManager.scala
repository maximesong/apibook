package com.cppdo.apibook.index

import java.nio.file.Paths
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.{TermQuery, BooleanQuery, MatchAllDocsQuery, IndexSearcher}

import scala.collection.JavaConverters._
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.{TextField, Field, StringField, Document}
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.index._
import org.apache.lucene.store.FSDirectory
import org.objectweb.asm.tree.ClassNode
import com.typesafe.config.ConfigFactory

/**
 * Created by song on 3/4/15.
 */
object IndexManager {
  val indexDirectory = "data"
  val fieldName = "name"
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

  private def buildDocument(classNode: ClassNode) : Document = {
    val document = new Document
    val nameField = new TextField(fieldName, classNode.name, Field.Store.YES)
    document.add(nameField)
    document
  }


}
