package com.cppdo.apibook

import com.cppdo.apibook.actor.ActorMaster
import com.cppdo.apibook.actor.ActorProtocols.{SaveMethod, SaveClass}
import com.cppdo.apibook.ast.{AstTreeManager, JarManager}
import com.cppdo.apibook.db.{Method, Class, DatabaseManager}
import com.cppdo.apibook.repository.ArtifactsManager.RichArtifact
import com.cppdo.apibook.repository.MavenRepository
import MavenRepository.{MavenArtifact, MavenArtifactSeq, MavenProject}
import com.cppdo.apibook.repository.ArtifactsManager.RichPackageFile

import scala.concurrent.Future

/**
 * Created by song on 5/25/15.
 */
object Master {
  def analyzeSource() = {
    val projects = DatabaseManager.getProjects()
    projects.foreach(project => {
      val artifacts = DatabaseManager.getArtifacts(project)
      val latestArtifact = artifacts.takeLatestVersion
      latestArtifact.foreach(artifact => {
        val sourcePackage = DatabaseManager.getSourcePackageFile(artifact)
        sourcePackage.foreach(packageFile => {
          val compilationUnits = JarManager.getCompilationUnits(packageFile.fullPath)
          val types = compilationUnits.flatMap(cu => AstTreeManager.typeDeclarationsOf(cu))
          types.foreach(typeDeclaration => {
            val klass = AstTreeManager.buildFrom(typeDeclaration, artifact)
            val savedClass = DatabaseManager.add(klass)
            typeDeclaration.getMethods.foreach(methodDeclaration => {
              val method = AstTreeManager.buildFrom(methodDeclaration, klass)
              DatabaseManager.add(method)
            })
          })
        })
      })
    })
  }
}
