package com.cppdo.apibook

import com.cppdo.apibook.repository.MavenRepository
import com.cppdo.apibook.repository.db.Artifacts
import slick.driver.JdbcDriver

import scala.concurrent.Await
import scala.concurrent.duration.Duration

//import slick.driver.H2Driver.api._

/**
 * Created by song on 1/17/15.
 */
object APIBook extends App {
  println("Hi")
  val projects = MavenRepository.getTopProjects()
  projects.foreach(println)

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
