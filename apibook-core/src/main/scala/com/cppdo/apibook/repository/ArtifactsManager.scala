package com.cppdo.apibook.repository

import java.io.{FileNotFoundException, File}
import java.net.URL
import java.util.jar.JarEntry

import com.cppdo.apibook.ast.JarManager
import com.cppdo.apibook.db.{Artifact, PackageFile, DatabaseManager}
import MavenRepository.{MavenArtifact, MavenArtifactSeq, MavenProject}
import com.cppdo.apibook.index.IndexManager
import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.io.FileUtils

/**
 * Created by song on 3/1/15.
 */
object ArtifactsManager extends LazyLogging {
  object PackageType extends Enumeration {
    type PackageType = Value
    val Library, Source, Doc = Value
  }
  val baseDirectory = "repository"
  val baseDocSaveDirectory = "apibook-web/public"
  val baseDocDirectory = "doc"

  def fetchTopProjectsAndArtifactsToDb(projectsNum: Int) = {
    val projects = MavenRepository.getTopProjects(projectsNum)
    logger.info("Projects count: " + projects.size)
    projects.foreach(project => {
      val projectWithId = DatabaseManager.add(project)
      val artifacts = projectWithId.fetchArtifacts()
      artifacts.foreach(artifact => {
        val artifactWithId = DatabaseManager.add(artifact)
      })
    })
  }

  private def downloadFile(url: String, savePath: String) = {
    FileUtils.copyURLToFile(new URL(url), new File(savePath))
  }

  def downloadLatestPackages = {
    val projects = DatabaseManager.getProjects()
    projects.foreach(project => {
      val artifacts = DatabaseManager.getArtifacts(project)
      artifacts.takeLatestVersion.foreach(artifact => {
        downloadFile(artifact.libraryPackageUrl, artifact.fullLibraryPackagePath)
        DatabaseManager.add(PackageFile(artifact.id.get, PackageType.Library.toString, artifact.relativeLibraryPackagePath))
        try {
          downloadFile(artifact.sourcePackageUrl, artifact.fullSourcePackagePath)
          DatabaseManager.add(PackageFile(artifact.id.get, PackageType.Source.toString, artifact.relativeSourcePackagePath))
        } catch {
          case e: FileNotFoundException => logger.warn("Fail to download: " + artifact.toString)
        }

      })
    })
    println(projects.size)
  }

  def getLatestArtifacts: Seq[Artifact] = {
    val projects = DatabaseManager.getProjects()
    projects.flatMap(project => {
      DatabaseManager.getArtifacts(project).takeLatestVersion
    })
  }

  def getPackageFileFullPath(relativePath: String) = {
    s"${baseDirectory}/${relativePath}"
  }

  def analysisArtifact(artifact: Artifact) = {
    val libraryPackageFile = DatabaseManager.getLibraryPackageFile(artifact)
    libraryPackageFile.foreach(library => {
      val fullPath = getPackageFileFullPath(library.relativePath)
      val classNodes = JarManager.getClassNodes(fullPath)
      classNodes.foreach(classNode => {
        classNode.fields
      })
    })
  }

  implicit class RichArtifact(artifact: Artifact) {

    def fullLibraryPackagePath = s"${baseDirectory}/${artifact.relativeLibraryPackagePath}"

    def fullSourcePackagePath = s"${baseDirectory}/${artifact.relativeSourcePackagePath}"

    def fullDocPackagePath = s"${baseDirectory}/${artifact.relativeDocPackagePath}"
  }

  implicit class RichPackageFile(packageFile: PackageFile) {
    def fullPath = s"${baseDirectory}/${packageFile.relativePath}"
  }

  implicit class RichDocEntry(jarEntry: JarEntry) {
    def docSavePath = s"${baseDocSaveDirectory}/${baseDocDirectory}/${jarEntry.getName}"
    def docPath = s"${baseDocDirectory}/${jarEntry.getName}"
  }
}
