package com.cppdo.apibook.db

import slick.driver.SQLiteDriver.api._
import slick.lifted.Tag



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



