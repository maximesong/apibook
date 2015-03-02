package com.cppdo.apibook

import com.cppdo.apibook.repository.MavenRepository
import com.cppdo.apibook.repository.db.{Projects, Artifacts}
import com.typesafe.scalalogging.LazyLogging
import slick.driver.JdbcDriver
import slick.jdbc.meta.MTable

import scala.concurrent.Await
import scala.concurrent.duration.Duration

//import slick.driver.H2Driver.api._

/**
 * Created by song on 1/17/15.
 */
object APIBook extends LazyLogging {
  def main(args: Array[String]): Unit = {
    logger.info("Hi")
    val projects = MavenRepository.getTopProjects(10)

    println(projects.size)
    projects.foreach(println)
    logger.info("Bye")
    fetchProjects()
  }

  def fetchProjects() = {
    import slick.driver.SQLiteDriver.api._
    val db = Database.forConfig("sqliteDb")

    try {
      val artifactsTable = TableQuery[Artifacts]
      val projectsTable = TableQuery[Projects]
      val tableList  = List(
        (projectsTable, "PROJECTS"),
        (artifactsTable, "ARTIFACTS")
      )
      val result = Await.result(db.run(MTable.getTables("")), Duration.Inf)
      val tableMap = (result map (table => (table.name.name, table))).toMap
      val newTables = tableList filter { case (_, name) => !tableMap.contains(name) } map { case (table, _) => table }
      val actions = newTables map (table => table.schema.create)
      val setup = DBIO.seq(
        actions: _*
      )
      Await.result(db.run(setup), Duration.Inf)
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
