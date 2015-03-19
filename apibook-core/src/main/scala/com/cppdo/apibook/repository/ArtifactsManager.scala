package com.cppdo.apibook.repository

import java.io.{FileNotFoundException, File}
import java.net.URL

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
  val baseDirectory = "repository"

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
    FileUtils.copyURLToFile(new URL(url), new File(getPackageFileFullPath(savePath)))
  }

  def downloadLatestPackages = {
    val projects = DatabaseManager.getProjects()
    projects.foreach(project => {
      val artifacts = DatabaseManager.getArtifacts(project)
      artifacts.takeLatestVersion.foreach(artifact => {
        downloadFile(artifact.libraryPackageUrl, artifact.libraryPackagePath)
        DatabaseManager.add(PackageFile(artifact.id.get, "library", artifact.libraryPackagePath))
        try {
          downloadFile(artifact.sourcePackageUrl, artifact.sourcePackagePath)
          DatabaseManager.add(PackageFile(artifact.id.get, "source", artifact.sourcePackagePath))
        } catch {
          case e: FileNotFoundException => logger.warn("Fail to download: " + artifact.toString)
        }

      })
    })
    println(projects.size)
  }

  def buildIndex(artifact: Artifact) =  {
    val packageFiles = DatabaseManager.getPackageFiles(artifact)
    packageFiles.filter(_.packageType == "library").foreach(packageFile => {
      val fullPath = getPackageFileFullPath(packageFile.path)
      val classNodes = JarManager.getClassNodes(fullPath)
      IndexManager.buildIndex(classNodes)
    })
  }

  private def getPackageFileFullPath(relativePath: String) = {
    s"${baseDirectory}/${relativePath}"
  }

  def buildIndexForArtifacts = {
    val artifacts = DatabaseManager.getArtifacts()
    artifacts.foreach(buildIndex(_))
  }

  def analysisArtifact(artifact: Artifact) = {
    val libraryPackageFile = DatabaseManager.getLibraryPackageFile(artifact)
    libraryPackageFile.foreach(library => {
      val fullPath = getPackageFileFullPath(library.path)
      val classNodes = JarManager.getClassNodes(fullPath)
      classNodes.foreach(classNode => {
        classNode.fields
      })
    })
  }
}
