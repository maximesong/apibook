package com.cppdo.apibook

import com.cppdo.apibook.db.{Project, Artifacts, Projects}
import com.cppdo.apibook.repository.MavenRepository
import com.typesafe.scalalogging.LazyLogging
import slick.driver.JdbcDriver
import slick.jdbc.meta.MTable
import slick.model.ForeignKeyAction.NoAction

import scala.concurrent.Await
import scala.concurrent.duration.Duration

//import slick.driver.H2Driver.api._

/**
 * Created by song on 1/17/15.
 */
object APIBook extends LazyLogging {
  def main(args: Array[String]): Unit = {
    logger.info("Hi")
    //fetchProjects()
    testVersions()
    logger.info("Bye")
  }

  def testVersions(): Unit = {
    val t1 = "1.0.6-M1"
    val v1 = MavenRepository.Version.parse(t1)
    println(v1)
  }

  def fetchProjects() = {
    import slick.driver.SQLiteDriver.api._
    val db = Database.forConfig("sqliteDb")

    val projects = MavenRepository.getTopProjects(10)

    projects.foreach(println)

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
      val createTableActions = newTables map (table => table.schema.create)
      val insertActions = projects.map(project => projectsTable.insertOrUpdate(project))
      val setup = DBIO.seq(
        (createTableActions ++ insertActions): _*
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
