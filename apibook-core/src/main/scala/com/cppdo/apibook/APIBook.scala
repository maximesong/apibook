package com.cppdo.apibook

import java.io.File
import java.net.URL
import java.nio.file.{Path, Paths, Files}

import com.cppdo.apibook.ast.JarManager
import com.cppdo.apibook.db._
import com.cppdo.apibook.index.IndexManager
import com.cppdo.apibook.repository.MavenRepository
import com.cppdo.apibook.repository.MavenRepository.{MavenArtifact, MavenArtifactSeq, MavenProject}
import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.io.FileUtils
import slick.driver.JdbcDriver
import slick.jdbc.meta.MTable
import slick.model.ForeignKeyAction.NoAction

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.Source

import sys.process._
//import slick.driver.H2Driver.api._

/**
 * Created by song on 1/17/15.
 */
object APIBook extends LazyLogging {
  def main(args: Array[String]): Unit = {
    logger.info("Hi")
    fetchProjects()
    //testVersions()
    //testJar()
    logger.info("Bye")
  }

  def testVersions(): Unit = {
    val v = MavenRepository.Version(0)
    //println(v.matchQualifierToVersion("M-1"))
    //println(v.matchQualifierToVersion("abc"))
    /*
    val t1 = "1.0.6-M1"
    val t2 = "1.0.6"
    val v1 = MavenRepository.Version.parse(t1)
    val v2 = MavenRepository.Version.parse(t2)
    //println(v1 compare v2)
    */
    val projects = MavenRepository.getTopProjects(500)
    projects.flatMap(project => {
      val artifacts = project.fetchArtifacts
      artifacts.flatMap(artifact => {
        MavenRepository.Version.tryParse(artifact.version).flatMap(_.qualifier)
      })
    }).toSet.toList.sortBy(v.matchQualifierToVersion).foreach(println)

  }

  def testJar() = {
    val nodes = JarManager.getClassNodes("/home/song/Downloads/asm-5.0.3.jar")
    nodes.foreach(node => println(node.name))
    IndexManager.buildIndex(nodes)
  }

  def fetchProjects() = {
    val baseDirectory = "repository"
    val projects = MavenRepository.getTopProjects(10)
    val latestArtifacts = projects.flatMap(_.fetchArtifacts.takeLatestVersion)
    latestArtifacts.foreach(artifact => {
      println(s"Downloading artifacts of ${artifact.name}...")
      FileUtils.copyURLToFile(new URL(artifact.libraryPackageUrl), new File(s"${baseDirectory}/${artifact.libraryPackagePath}"))
      FileUtils.copyURLToFile(new URL(artifact.sourcePackageUrl), new File(s"${baseDirectory}/${artifact.sourcePackagePath}"))
      //new URL(artifact.libraryPackageUrl) #> new File(artifact.libraryPackagePath) !!
    })
      //val insertActions = projects.map(project => projectsTable.insertOrUpdate(project))
      //Await.result(db.run(setup), Duration.Inf)
  }


  /*
  import slick.driver.SQLiteDriver.api._
  val db = Database.forConfig("sqliteDb")
  val artifactsTable = TableQuery[Artifacts]
  val setup = DBIO.seq(
    artifactsTable.schema.create
  )
  Await.result(db.run(setup), Duration.Inf)
  //val db = Database.forConfig("h2mem1")
  */
}
