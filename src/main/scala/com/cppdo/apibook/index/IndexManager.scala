package com.cppdo.apibook.index

import java.nio.file.Paths
import scala.collection.JavaConverters._
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.{Field, StringField, Document}
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.index.{IndexWriterConfig, IndexWriter}
import org.apache.lucene.store.FSDirectory
import org.objectweb.asm.tree.ClassNode

/**
 * Created by song on 3/4/15.
 */
object IndexManager {
  val indexDirectory = "data"
  def buildIndex(classNodes: Seq[ClassNode]) = {
    val directory = FSDirectory.open(Paths.get(indexDirectory))
    val analyzer = new StandardAnalyzer()
    val indexWriterConfig = new IndexWriterConfig(analyzer)
    indexWriterConfig.setOpenMode(OpenMode.CREATE)
    val indexWriter = new IndexWriter(directory, indexWriterConfig)
    val documents = classNodes.map(buildDocument(_))
    indexWriter.addDocuments(documents.asJava)
    indexWriter.close()
  }

  private def buildDocument(classNode: ClassNode) : Document = {
    val document = new Document
    val nameField = new StringField("name", classNode.name, Field.Store.YES)
    document.add(nameField)
    document
  }


}
