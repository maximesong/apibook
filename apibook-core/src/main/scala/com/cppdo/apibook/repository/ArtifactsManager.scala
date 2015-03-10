package com.cppdo.apibook.repository

import java.io.File
import java.net.URL

import com.cppdo.apibook.db.DatabaseManager
import MavenRepository.{MavenArtifact, MavenArtifactSeq, MavenProject}
import org.apache.commons.io.FileUtils

/**
 * Created by song on 3/1/15.
 */
object ArtifactsManager {
  val baseDirectory = "repository"

  def fetchTopProjectsAndArtifactsToDb(projectsNum: Int) = {
    val projects = MavenRepository.getTopProjects(projectsNum)
    projects.foreach(project => {
      val projectWithId = DatabaseManager.add(project)
      val artifacts = projectWithId.fetchArtifacts()
      artifacts.foreach(artifact => {
        val artifactWithId = DatabaseManager.add(artifact)
      })
    })
  }

  private def downloadFile(url: String, savePath: String) = {
    FileUtils.copyURLToFile(new URL(url), new File(s"${baseDirectory}/${savePath}"))
  }

  def downloadLatestPackages = {
    val projects = DatabaseManager.getProjects()
    projects.foreach(project => {
      val artifacts = DatabaseManager.getArtifacts(project)
      artifacts.takeLatestVersion.foreach(artifact => {
        downloadFile(artifact.libraryPackageUrl, artifact.libraryPackagePath)
        downloadFile(artifact.sourcePackageUrl, artifact.sourcePackagePath)
      })
    })
    println(projects.size)
  }
}
