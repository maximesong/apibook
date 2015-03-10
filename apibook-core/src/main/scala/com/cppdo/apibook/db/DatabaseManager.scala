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
  val tableList  = List(
    (projectsTable, "PROJECTS"),
    (artifactsTable, "ARTIFACTS"),
    (classesTable, "Classes"),
    (methodsTable, "Mehtods")
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
}
