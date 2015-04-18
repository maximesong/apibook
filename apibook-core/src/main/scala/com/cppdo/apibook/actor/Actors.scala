package com.cppdo.apibook.actor

import java.io.File
import java.net.URL
import java.nio.file.Paths

import akka.actor.Actor
import akka.actor.Actor.Receive
import com.cppdo.apibook.actor.ActorProtocols.{FetchPage, FinishDownloadFile, DownloadFile}
import com.cppdo.apibook.ast.JarManager
import com.cppdo.apibook.db.{Project, DatabaseManager, Artifact}
import com.cppdo.apibook.repository.MavenRepository
import org.apache.commons.io.FileUtils
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig}
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.store.FSDirectory
import scala.concurrent._
import ExecutionContext.Implicits.global

/**
 * Created by song on 3/20/15.
 */

class TryActor extends Actor {
  override def receive: Receive = ???
}

object ActorProtocols {
  case class DownloadFile(fromUrl: String, toPath: String)
  case class FinishDownloadFile(fromUrl: String, toPath: String)
  case class FetchPage(page: Int)
}

class BuildIndexActor(indexDirectoryPath: String) extends Actor {

  var indexWriter: IndexWriter = null

  override def preStart() = {
    super.preStart()
    val directory = FSDirectory.open(Paths.get(indexDirectoryPath))
    val analyzer = new StandardAnalyzer()
    val indexWriterConfig = new IndexWriterConfig(analyzer)
    indexWriterConfig.setOpenMode(OpenMode.CREATE_OR_APPEND)
    indexWriter = new IndexWriter(directory, indexWriterConfig)
  }

  override def postStop() = {
    super.postStop()
    indexWriter.close()
  }
  override def receive: Actor.Receive = {
    ???
  }
}

class DownloadFileActor extends Actor {
  override def receive: Actor.Receive = {
    case DownloadFile(fromUrl, toPath) => {
        FileUtils.copyURLToFile(new URL(fromUrl), new File(toPath))
      sender() ! FinishDownloadFile(fromUrl, toPath)
    }
  }
}

class ArtifactCollectActor extends Actor {
  override def receive: Actor.Receive = {
    ???
  }
}

class FetchMavenProjectsActor extends Actor {
  override def receive: Actor.Receive = {
    case FetchPage(page) => {
      val projects = MavenRepository.fetchProjectsFromListPage(page)
      println(projects.size)
    }
  }
}

class FetchMavenArtifactsActor extends Actor {
  override def receive: Actor.Receive = {
    case project: Project => {
      MavenRepository.fetchArtifactsOf(project)
    }
  }
}

class ArtifactAnalyzer extends Actor {
  override def receive: Actor.Receive = {
    case artifact: Artifact => {
      val optionLibraryPackage = DatabaseManager.getLibraryPackageFile(artifact)
      optionLibraryPackage.foreach(libraryPackage => {
        val classNodes = JarManager.getClassNodes(libraryPackage.path)
        classNodes.foreach(classNode => {
          val methodNodes = JarManager.getMethodNodes(classNode)
          methodNodes.foreach(methodNode => {
            println(methodNode)
          })
        })
      })
    }
  }
}