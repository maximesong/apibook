package com.cppdo.apibook

import com.cppdo.apibook.repository.MavenRepository
import com.cppdo.apibook.repository.db.{Projects, Artifacts}
import slick.driver.JdbcDriver

import scala.concurrent.Await
import scala.concurrent.duration.Duration

//import slick.driver.H2Driver.api._

/**
 * Created by song on 1/17/15.
 */
object APIBook {
  def main(args: Array[String]): Unit = {
    println("Hi")
    val projects = MavenRepository.getTopProjects(10)

    println(projects.size)
    projects.foreach(println)
    println("Bye")
  }

  def fetchProjects() = {
    import slick.driver.SQLiteDriver.api._
    val db = Database.forConfig("sqliteDb")
    try {
      val artifactsTable = TableQuery[Artifacts]
      val projectsTable = TableQuery[Projects]
      val setup = DBIO.seq(
        (projectsTable.schema ++ artifactsTable.schema).create,
      )
    } finally {
      db.close()
    }

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
