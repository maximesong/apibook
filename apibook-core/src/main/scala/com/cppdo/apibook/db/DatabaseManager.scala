package com.cppdo.apibook.db

import slick.driver.SQLiteDriver.api._
import slick.jdbc.meta.MTable

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
 * Created by song on 3/10/15.
 */
object DatabaseManager {

  val db = Database.forConfig("sqliteDb")

  val artifactsTable = TableQuery[Artifacts]
  val projectsTable = TableQuery[Projects]
  val classesTable = TableQuery[Classes]
  val methodsTable = TableQuery[Methods]
  val packageFilesTable = TableQuery[PackageFiles]
  val tableList  = List(
    (projectsTable, "PROJECTS"),
    (artifactsTable, "ARTIFACTS"),
    (classesTable, "CLASSES"),
    (methodsTable, "METHODS"),
    (packageFilesTable, "PACKAGE_FILES")
  )

  def createTables = {
    val result = Await.result(db.run(MTable.getTables("")), Duration.Inf)
    val tableMap = (result map (table => (table.name.name, table))).toMap
    val newTables = tableList filter { case (_, name) => !tableMap.contains(name) } map { case (table, _) => table }
    val createTableActions = newTables map (table => table.schema.create)
    val setup = DBIO.seq(
      createTableActions: _*
    )
    Await.result(db.run(setup), Duration.Inf)
  }

  def add(project: Project): Project = {
    val insertAction = projectsTable insertOrUpdate project
    Await.result(db.run(insertAction), Duration.Inf)
    project
  }

  def add(artifact: Artifact): Artifact = {
    if (exists(artifact)) {
      artifact
    }
    else {
      val insertAction = (artifactsTable returning artifactsTable.map(_.id)
        into ((a, id) => a.copy(id=Some(id)))) insertOrUpdate artifact
      val result: Option[Artifact] = Await.result(db.run(insertAction), Duration.Inf)
      result.getOrElse(artifact)
    }
  }

  def add(packageFile: PackageFile): PackageFile = {
    val insertAction = (packageFilesTable returning packageFilesTable.map(_.id)
      into ((packageFile, id) => packageFile.copy(id=Some(id)))) insertOrUpdate packageFile
    val result: Option[PackageFile] = Await.result(db.run(insertAction), Duration.Inf)
    result.getOrElse(packageFile)
  }

  def exists(artifact: Artifact): Boolean = {
    val countQuery = artifactsTable.filter(a => a.group === artifact.group &&
      a.name === artifact.name && a.version === artifact.version)
    val result  = Await.result(db.run(countQuery.result), Duration.Inf)
    result.size > 0
  }

  def getProjects() : Seq[Project] = {
    Await.result(db.run(projectsTable.result), Duration.Inf)
  }

  def getArtifacts(project: Project) : Seq[Artifact] = {
    val query = artifactsTable.filter(artifact => artifact.group === project.group && artifact.name === project.name)
    Await.result(db.run(query.result),Duration.Inf)
  }

  def getArtifacts() : Seq[Artifact] = {
    val query = artifactsTable
    Await.result(db.run(query.result),Duration.Inf)
  }

  def getPackageFiles(artifact: Artifact): Seq[PackageFile] = {
    val query = packageFilesTable.filter(packageFile => packageFile.artifactId === artifact.id)
    Await.result(db.run(query.result), Duration.Inf)
  }

  def getLibraryPackageFile(artifact: Artifact): Option[PackageFile] = {
    val query = packageFilesTable.filter(packageFile => packageFile.artifactId === artifact.id)
    val packageFiles = Await.result(db.run(query.result), Duration.Inf)
    ???
  }

  createTables
}
