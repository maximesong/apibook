package com.cppdo.apibook.actor

import java.io.{FileNotFoundException, File}
import java.net.URL
import java.nio.file.Paths

import akka.actor.{Actor, Props, ActorRef}
import akka.actor.Actor.Receive
import akka.routing.RoundRobinPool
import com.cppdo.apibook.actor.ActorProtocols._
import com.cppdo.apibook.ast.{AstTreeManager, JarManager}
import com.cppdo.apibook.db._
import com.cppdo.apibook.index.IndexManager
import com.cppdo.apibook.repository.ArtifactsManager.{PackageType, RichArtifact, RichPackageFile}
import com.cppdo.apibook.ast.AstTreeManager.{RichMethodNode, RichClassNode}
import com.cppdo.apibook.repository.{ArtifactsManager, MavenRepository}
import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.io.FileUtils
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig}
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.store.FSDirectory
import org.objectweb.asm.tree.ClassNode
import scala.concurrent._
import com.cppdo.apibook.repository.MavenRepository.{MavenArtifact, MavenArtifactSeq, MavenProject}
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
  case class FailDownloadFile(fromUrl: String, toPath: String, e: Exception)
  case class FetchProjectListPage(page: Int, receiver: Option[ActorRef] = None)
  case class FetchProjects(n: Int, receiver: Option[ActorRef] = None)
  case class FetchArtifacts(project: Project, receiver: Option[ActorRef] = None)
  case class SaveProject(project: Project, receiver: Option[ActorRef] = None)
  case class SaveArtifact(artifact: Artifact, receiver: Option[ActorRef] = None)
  case class SavePackageFile(packageFile: PackageFile, receiver: Option[ActorRef] = None)
  case class SaveClass(klass: Class, receiver: Option[ActorRef] = None)
  case class ClassSaved(klass: Class)
  case class ReadLibraryPackage(artifact: Artifact, receiver: Option[ActorRef] = None)
  case class LibraryPackageResult(artifact: Artifact, libraryPackageFile: Option[PackageFile])
  case class SaveMethod(method: Method, receiver: Option[ActorRef] = None)
  case class MethodSaved(method: Method)
  case class FetchLatestPackages()
  case class AnalyzeAndSave()
  case class BuildIndexForClass(klass: Class, receiver: Option[ActorRef] = None)
  case class BuildIndexForMethod(method: Method, receiver: Option[ActorRef] = None)
}

class BuildIndexActor() extends Actor with LazyLogging {

  var indexWriter: IndexWriter = null
  val indexDirectoryPath: String = IndexManager.indexDirectory
  var count = 0
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
    logger.info("Close")
  }
  override def receive: Actor.Receive = {
    case BuildIndexForClass(klass, receiver) => {
      val document = IndexManager.buildDocument(klass)
      indexWriter.addDocument(document)
    }
    case BuildIndexForMethod(method, receiver) => {
      //logger.info(method.name)
      val document = IndexManager.buildDocument(method)
      indexWriter.addDocument(document)
      count += 1
      logger.info(count.toString)
    }
  }
}

class DownloadFileActor extends Actor {
  override def receive: Actor.Receive = {
    case DownloadFile(fromUrl, toPath) => {
      try {
        FileUtils.copyURLToFile(new URL(fromUrl), new File(toPath))
      } catch {
        case e: FileNotFoundException => {
          sender() ! FailDownloadFile(fromUrl, toPath, e)
        }
      }

      sender() ! FinishDownloadFile(fromUrl, toPath)
    }
  }
}

class ArtifactsCollectActor(fetchActor: ActorRef, storageActor: ActorRef) extends Actor with LazyLogging {
  override def receive: Actor.Receive = {
    case project: Project => {
      logger.info(s"collecting project $project")
      storageActor ! SaveProject(project)
      fetchActor ! FetchArtifacts(project)
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

class PackageFetchActor(storageActor: ActorRef) extends Actor with LazyLogging {

  var downloadWorker: ActorRef = null

  override def preStart() = {
    logger.info("??")
    downloadWorker = context.actorOf(RoundRobinPool(5).props(Props[DownloadFileActor]))
  }

  override def receive: Actor.Receive = {
    case _: FetchLatestPackages => {
      val artifacts = ArtifactsManager.getLatestArtifacts
      artifacts.foreach(artifact => {
        self ! artifact
      })
    }
    case artifact: Artifact => {
      logger.info(s"downloading package for $artifact")
      downloadWorker ! DownloadFile(artifact.libraryPackageUrl, artifact.fullLibraryPackagePath)
      storageActor ! SavePackageFile(PackageFile(artifact.id.get, PackageType.Library.toString, artifact.relativeLibraryPackagePath))
      downloadWorker ! DownloadFile(artifact.sourcePackageUrl, artifact.fullSourcePackagePath)
      storageActor ! SavePackageFile(PackageFile(artifact.id.get, PackageType.Source.toString, artifact.relativeSourcePackagePath))
      downloadWorker ! DownloadFile(artifact.docPackageUrl, artifact.fullDocPackagePath)
      storageActor ! SavePackageFile(PackageFile(artifact.id.get, PackageType.Doc.toString, artifact.relativeDocPackagePath))
    }
    case message => {
      //logger.info(s"What $message")
    }
  }
}

class DbWriteActor extends Actor with LazyLogging {
  override def receive: Actor.Receive = {
    case SaveProject(project, receiver) => {
      logger.info(s"saving $project")
      val projectSaved = DatabaseManager.add(project)
    }
    case SaveArtifact(artifact, receiver) => {
      //logger.info(s"saving $artifact")
      val artifactSaved = DatabaseManager.add(artifact)
    }
    case SavePackageFile(packageFile, receiver) => {
      val packageFileSaved = DatabaseManager.add(packageFile)
    }
    case SaveClass(klass, receiver) => {
      //logger.info("SaveClass?")
      val klassSaved = DatabaseManager.add(klass)
      receiver.getOrElse(sender()) ! ClassSaved(klassSaved)
    }
    case SaveMethod(method, receiver) => {
      val methodSaved = DatabaseManager.add(method)
      receiver.getOrElse(sender()) ! MethodSaved(methodSaved)
    }
    case ReadLibraryPackage(artifact, receiver) => {
      val libraryPackage = DatabaseManager.getLibraryPackageFile(artifact)
      receiver.getOrElse(sender()) ! LibraryPackageResult(artifact, libraryPackage)
    }
  }
}

class ArtifactAnalyzer(storageActor: ActorRef)  extends Actor with LazyLogging {
  override def receive: Actor.Receive = {
    case _: AnalyzeAndSave => {
      val artifacts = DatabaseManager.getArtifacts()
      artifacts.foreach(artifact => {
        logger.info(s"analyzing $artifact")
        self ! artifact
      })
    }
    case artifact: Artifact => {
      storageActor ! ReadLibraryPackage(artifact)
    }
    case LibraryPackageResult(artifact, libraryPackage) => {
      try {
        libraryPackage.foreach(packageFile => {
          val classNodes = JarManager.getClassNodes(packageFile.fullPath)
          classNodes.foreach(classNode => {
            if (classNode.isPublic && classNode.isRegular) {
              val actor = context.actorOf(Props(new ClassNodeAnalyzer(artifact, classNode, storageActor)))
              actor ! AnalyzeAndSave()
            }
          })
        })
      } catch {
        case e: FileNotFoundException => {
          logger.warn(e.toString)
        }
      }
    }
  }
}

class ClassNodeAnalyzer(artifact: Artifact, classNode: ClassNode, storageActor: ActorRef) extends Actor with LazyLogging {
  override def receive: Actor.Receive = {
    case _: AnalyzeAndSave => {
      //logger.info("HAHA")
      classNode.fields
      storageActor ! SaveClass(AstTreeManager.buildFrom(classNode, artifact))
    }
    case ClassSaved(klass) => {
      logger.info(s"Analyzing ${klass.fullName}")
      AstTreeManager.methodNodesOf(classNode).foreach(methodNode => {
        if (methodNode.isRegular) {
          storageActor ! SaveMethod(AstTreeManager.buildFrom(methodNode, klass))
        }
      })
    }
    case message => {
      //logger.info(message.toString)
    }
  }
}