package com.cppdo.apibook.db

import com.cppdo.apibook.repository.ArtifactsManager.PackageType
import com.sun.xml.internal.bind.v2.schemagen.episode.Klass
import com.typesafe.scalalogging.LazyLogging
import slick.driver.SQLiteDriver.api._
import slick.jdbc.meta.MTable

import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by song on 3/10/15.
 */
object DatabaseManager extends LazyLogging {

  val db = Database.forConfig("sqliteDb")

  val artifactsTable = TableQuery[Artifacts]
  val projectsTable = TableQuery[Projects]
  val classesTable = TableQuery[Classes]
  val methodsTable = TableQuery[Methods]
  val packageFilesTable = TableQuery[PackageFiles]
  val gitHubRepositoriesTable = TableQuery[GitHubRepositories]
  val packageDeclarationsTable = TableQuery[PackageDeclarations]
  val packageReferencesTable = TableQuery[PackageReferences]

  val tableList  = List(
    (projectsTable, "PROJECTS"),
    (artifactsTable, "ARTIFACTS"),
    (classesTable, "CLASSES"),
    (methodsTable, "METHODS"),
    (packageFilesTable, "PACKAGE_FILES"),
    (packageDeclarationsTable, "PACKAGE_DECLARATIONS"),
    (packageReferencesTable, "PACKAGE_REFERENCES"),
    (gitHubRepositoriesTable, "GITHUB_REPOSITORIES")
  )

  def createTables = {
    val result = Await.result(db.run(MTable.getTables("")), Duration.Inf)
    val tableMap = (result map (table => (table.name.name, table))).toMap
    val newTables = tableList filter { case (_, name) => !tableMap.contains(name) } map { case (table, _) => table }
    val createTableActions = newTables map (table => table.schema.create)
    val setup = DBIO.seq(
      createTableActions: _*
    )
    logger.info("CREATE TABLES !")
    logger.info(setup.toString)
    Await.result(db.run(setup), Duration.Inf)
  }

  def addAsync(project: Project): Future[Project] = {
    val insertAction = (projectsTable += project)
    db.run(insertAction).map(_ => project)
  }

  def addAsync(artifact: Artifact): Future[Artifact] = {
    val insertAction = (artifactsTable returning artifactsTable.map(_.id)
      into ((a, id) => a.copy(id=Some(id)))) += artifact
    db.run(insertAction)
  }

  def add(project: Project): Project = {
    val insertAction = projectsTable insertOrUpdate project
    Await.result(db.run(insertAction), Duration.Inf)
    project
  }

  def add(artifact: Artifact): Artifact = {
    existing(artifact).getOrElse({
      val insertAction = (artifactsTable returning artifactsTable.map(_.id)
        into ((a, id) => a.copy(id=Some(id)))) insertOrUpdate artifact
      val result: Option[Artifact] = Await.result(db.run(insertAction), Duration.Inf)
      result.getOrElse(artifact)
    })
  }

  def add(packageFile: PackageFile): PackageFile = {
    existing(packageFile).getOrElse({
      val insertAction = (packageFilesTable returning packageFilesTable.map(_.id)
        into ((packageFile, id) => packageFile.copy(id=Some(id)))) insertOrUpdate packageFile
      val result: Option[PackageFile] = Await.result(db.run(insertAction), Duration.Inf)
      result.getOrElse(packageFile)
    })
  }

  def add(klass: Class): Class = {
    existing(klass).getOrElse({
      val insertAction = (classesTable returning classesTable.map(_.id)
        into ((klass, id) => klass.copy(id=Some(id)))) insertOrUpdate klass
      val result: Option[Class] = Await.result(db.run(insertAction), Duration.Inf)
      result.getOrElse(klass)
    })
  }

  def add(method: Method): Method = {
    existing(method).getOrElse({
      val insertAction = (methodsTable returning methodsTable.map(_.id)
        into ((method, id) => method.copy(id=Some(id)))) insertOrUpdate method
      val result: Option[Method] = Await.result(db.run(insertAction), Duration.Inf)
      result.getOrElse(method)
    })
  }

  def add(gitHubRepository: GitHubRepository): GitHubRepository = {
    val insertAction = gitHubRepositoriesTable.insertOrUpdate(gitHubRepository)
    Await.result(db.run(insertAction), Duration.Inf)
    return gitHubRepository
  }

  def add(packageReference: PackageReference): PackageReference = {
    val insertAction = packageReferencesTable.insertOrUpdate(packageReference)
    Await.result(db.run(insertAction), Duration.Inf)
    return packageReference
  }

  def existing(artifact: Artifact): Option[Artifact] = {
    val countQuery = artifactsTable.filter(a => a.group === artifact.group &&
      a.name === artifact.name && a.version === artifact.version)
    val result  = Await.result(db.run(countQuery.result), Duration.Inf)
    result.headOption
  }

  def existing(packageFile: PackageFile): Option[PackageFile] = {
    val countQuery = packageFilesTable.filter(pf => pf.artifactId === packageFile.artifactId &&
      pf.packageType === packageFile.packageType)
    val result  = Await.result(db.run(countQuery.result), Duration.Inf)
    result.headOption
  }

  def existing(klass: Class): Option[Class] = {
    val countQuery = classesTable.filter(cl => cl.artifactId === klass.artifactId &&
      cl.fullName === klass.fullName)
    val result  = Await.result(db.run(countQuery.result), Duration.Inf)
    result.headOption
  }

  def existing(method: Method): Option[Method] = {
    val countQuery = methodsTable.filter(m => m.classId === method.enclosingClassId &&
      m.name === method.name && m.signature === method.signature)
    val result  = Await.result(db.run(countQuery.result), Duration.Inf)
    result.headOption
  }

  def getProjects() : Seq[Project] = {
    Await.result(db.run(projectsTable.result), Duration.Inf)
  }

  def getArtifacts(project: Project) : Seq[Artifact] = {
    val query = artifactsTable.filter(artifact => artifact.group === project.group && artifact.name === project.name)
    Await.result(db.run(query.result), Duration.Inf)
  }

  def getArtifacts() : Seq[Artifact] = {
    val query = artifactsTable
    Await.result(db.run(query.result), Duration.Inf)
  }

  def getClasses(artifact: Artifact): Seq[Class] = {
    val query = classesTable.filter(klass => klass.artifactId === artifact.id)
    Await.result(db.run(query.result), Duration.Inf)
  }

  def getClasses(): Seq[Class] = {
    Await.result(db.run(classesTable.result), Duration.Inf)
  }

  def getMethods(klass: Class): Seq[Method] = {
    val query = methodsTable.filter(method => method.classId === klass.id)
    Await.result(db.run(query.result), Duration.Inf)
  }

  def getMethods(): Seq[Method] = {
    Await.result(db.run(methodsTable.result), Duration.Inf)
  }

  def getPackageFiles(artifact: Artifact): Seq[PackageFile] = {
    val query = packageFilesTable.filter(packageFile => packageFile.artifactId === artifact.id)
    Await.result(db.run(query.result), Duration.Inf)
  }

  def getLibraryPackageFile(artifact: Artifact): Option[PackageFile] = {
    val query = packageFilesTable.filter(packageFile => packageFile.artifactId === artifact.id)
    val packageFiles : Seq[PackageFile] = Await.result(db.run(query.result), Duration.Inf)
    val libraryPackageFiles = packageFiles.filter(file => file.packageType == PackageType.Library.toString)
    libraryPackageFiles.headOption
  }

  def getSourcePackageFile(artifact: Artifact): Option[PackageFile] = {
    val query = packageFilesTable.filter(packageFile => packageFile.artifactId === artifact.id)
    val packageFiles : Seq[PackageFile] = Await.result(db.run(query.result), Duration.Inf)
    val sourcePackageFiles = packageFiles.filter(file => file.packageType == PackageType.Source.toString)
    sourcePackageFiles.headOption
  }

  def getMethod(methodId: Int): Method = {
    val query = methodsTable.filter(method => method.id === methodId)
    val result: Seq[Method] = Await.result(db.run(query.result), Duration.Inf)
    result.head
  }

  def getKlass(klassId: Int): Class = {
    val query = classesTable.filter(klass => klass.id === klassId)
    val result: Seq[Class] = Await.result(db.run(query.result), Duration.Inf)
    result.head
  }



  def getArtifact(artifactId: Int): Artifact = {
    val query = artifactsTable.filter(artifact => artifact.id === artifactId)
    val result: Seq[Artifact] = Await.result(db.run(query.result), Duration.Inf)
    result.head
  }
  createTables
}
