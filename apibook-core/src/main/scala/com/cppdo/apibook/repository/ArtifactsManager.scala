package com.cppdo.apibook.repository

import com.cppdo.apibook.db.DatabaseManager
import MavenRepository.{MavenArtifact, MavenArtifactSeq, MavenProject}

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
}
