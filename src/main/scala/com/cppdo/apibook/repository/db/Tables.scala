package com.cppdo.apibook.repository.db

import slick.driver.SQLiteDriver.api._
import slick.lifted.Tag



/**
 * Created by song on 3/1/15.
 */
case class Project(id: Option[Int], name: String, group: String)

class Projects(tag: Tag) extends Table[Project](tag, "PROJECTS") {
  def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
  def name = column[String]("NAME")
  def group = column[String]("GROUP")

  def * = (id.?, name, group) <> (Project.tupled, Project.unapply)
}

case class Artifact(id: Option[Int], name: String, group: String, version: String)

class Artifacts(tag: Tag) extends Table[Artifact](tag, "ARTIFACTS") {
  def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
  def name = column[String]("NAME")
  def group = column[String]("GROUP")
  def version = column[String]("VERSION")

  def * = (id.?, name, group, version) <> (Artifact.tupled, Artifact.unapply)
}



