package com.cppdo.apibook.actor

import java.io.File
import java.net.URL
import java.nio.file.Paths

import akka.actor.{Actor, ActorRef}
import akka.actor.Actor.Receive
import com.cppdo.apibook.actor.ActorProtocols._
import com.cppdo.apibook.ast.JarManager
import com.cppdo.apibook.db.{Project, DatabaseManager, Artifact}
import com.cppdo.apibook.repository.MavenRepository
import com.typesafe.scalalogging.LazyLogging
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
  case class FetchProjectListPage(page: Int, receiver: Option[ActorRef] = None)
  case class FetchProjects(n: Int, receiver: Option[ActorRef] = None)
  case class FetchArtifacts(project: Project, receiver: Option[ActorRef] = None)
  case class SaveProject(project: Project)
  case class SaveArtifact(artifact: Artifact)
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

class ArtifactsCollectActor(fetchActor: ActorRef, storageActor: ActorRef) extends Actor with LazyLogging {
  override def receive: Actor.Receive = {
    case project: Project => {
      logger.info(s"collecting project $project")
      storageActor ! SaveProject(project)
      //fetchActor ! FetchArtifacts(project)
    }
    case artifact: Artifact => {
      //logger.info(artifact.toString)
      storageActor ! SaveArtifact(artifact)
    }
  }
}

class MavenFetchActor extends Actor with LazyLogging {
  override def receive: Actor.Receive = {
    case FetchProjectListPage(page, receiver) => {
      val projects = MavenRepository.fetchProjectsFromListPage(page)
      projects.foreach(project => {
        receiver.getOrElse(sender()) ! project
      })
    }
    case FetchProjects(n, receiver) => {
      MavenRepository.pagesForTopProjects(n).foreach(page => {
        self ! FetchProjectListPage(page, receiver)
      })
    }
    case FetchArtifacts(project, receiver) => {
      val artifacts = MavenRepository.fetchArtifactsOf(project)
      artifacts.foreach(artifact => {
        //logger.info(artifact.name)
        receiver.getOrElse(sender()) ! artifact
      })
    }
  }
}

class DbWriteActor extends Actor with LazyLogging {
  override def receive: Actor.Receive = {
    case SaveProject(project) => {
      logger.info(s"saving $project")
      DatabaseManager.add(project)
    }
    case SaveArtifact(artifact) => {
      //logger.info(s"saving $artifact")
      DatabaseManager.add(artifact)
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