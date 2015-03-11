package com.cppdo.apibook.db

import slick.driver.SQLiteDriver.api._
import slick.lifted.{ProvenShape, Tag}



/**
 * Created by song on 3/1/15.
 */
case class Project(name: String, group: String)

class Projects(tag: Tag) extends Table[Project](tag, "PROJECTS") {
  def name = column[String]("NAME")
  def group = column[String]("GROUP")

  def pk = primaryKey("PROJECT_PK", (name, group))

  def * = (name, group) <> (Project.tupled, Project.unapply)
}

case class Artifact(name: String, group: String, version: String, id: Option[Int] = None)

class Artifacts(tag: Tag) extends Table[Artifact](tag, "ARTIFACTS") {
  def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
  def name = column[String]("NAME")
  def group = column[String]("GROUP")
  def version = column[String]("VERSION")

  def project = foreignKey("PROJECT_FK", (name, group), TableQuery[Projects])(t => (t.name, t.group))
  def idx = index("IDX_ARTIFACT", (name, group, version), unique=true)

  def * = (name, group, version, id.?) <> (Artifact.tupled, Artifact.unapply)
}

case class Class(fullName: String, artifactId: Int, id: Option[Int] = None)

class Classes(tag: Tag) extends Table[Class](tag, "CLASSES") {
  def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
  def fullName = column[String]("FULL_NAME")
  def artifactId = column[Int]("ARTIFACT_ID")

  def artifact = foreignKey("ARTIFACT_FK", artifactId, TableQuery[Artifacts])(t => t.id)

  def * = (fullName, artifactId, id.?) <> (Class.tupled, Class.unapply)
}

case class Method(name: String, signature: String, enclosingClassId: Int, id: Option[Int] = None)


class Methods(tag: Tag) extends Table[Method](tag, "METHODS") {
  def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
  def name = column[String]("NAME")
  def signature = column[String]("SIGNATURE")
  def classId = column[Int]("CLASS_ID")

  def klass = foreignKey("CLASS_FK", classId, TableQuery[Classes])(t => t.id)

  def * = (name, signature, classId, id.?) <> (Method.tupled, Method.unapply)
}

case class PackageFile(artifactId: Int, packageType: String, path: String, id: Option[Int] = None)

class PackageFiles(tag: Tag) extends Table[PackageFile](tag, "PACKAGE_FILES") {
  def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
  def artifactId = column[Int]("ARTIFACT_ID")
  def packageType = column[String]("PACKAGE_TYPE")
  def path = column[String]("PATH")

  def artifact = foreignKey("ARTIFACT_FK", artifactId, TableQuery[Artifacts])(t => t.id)
  def idx = index("IDX_PACKAGE_FILE", (artifactId, packageType), unique=true)

  def * = (artifactId, packageType, path, id.?) <> (PackageFile.tupled, PackageFile.unapply)
}