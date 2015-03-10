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

case class Artifact(name: String, group: String, version: String)

class Artifacts(tag: Tag) extends Table[Artifact](tag, "ARTIFACTS") {
  def name = column[String]("NAME")
  def group = column[String]("GROUP")
  def version = column[String]("VERSION")

  def project = foreignKey("PROJECT_FK", (name, group), TableQuery[Projects])(t => (t.name, t.group))

  def pk = primaryKey("ARTIFACT_PK", (name, group, version))

  def * = (name, group, version) <> (Artifact.tupled, Artifact.unapply)
}

case class Class(id: Option[Int], artifact: Artifact, fullName: String)

class Classes(tag: Tag) extends Table[Class](tag, "CLASSES") {
  def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)

  def artifactName = column[String]("ARTIFACT_NAME")
  def artifactGroup = column[String]("ARTIFACT_GROUP")
  def artifactVersion = column[String]("ARTIFACT_VERSION")

  def fullName = column[String]("FULL_NAME")

  def artifact = foreignKey("ARTIFACT_FK", (artifactName, artifactGroup, artifactVersion),
    TableQuery[Artifacts])(t => (t.name, t.group, t.version))

  def * = (id.?, artifactName, artifactGroup, artifactVersion, fullName).shaped <>
    (c => Class(c._1, Artifact(c._2, c._3, c._4), c._5),
      (klass: Class) =>  Some((klass.id, klass.fullName, klass.artifact.name, klass.artifact.group, klass.artifact.version)))
}

case class Method(name: String, signature: String, enclosingClassId: Int)


class Methods(tag: Tag) extends Table[Method](tag, "METHODS") {
  def name = column[String]("NAME")

  def signature = column[String]("SIGNATURE")

  def classId = column[Int]("CLASS_ID")

  def klass = foreignKey("CLASS_FK", classId, TableQuery[Classes])(t => t.id)

  def * = (name, signature, classId) <> (Method.tupled, Method.unapply)
}