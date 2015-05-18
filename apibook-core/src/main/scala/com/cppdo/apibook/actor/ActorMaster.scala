package com.cppdo.apibook.actor

import akka.actor.Actor.Receive
import akka.actor.{Actor, Props, ActorSystem}
import akka.pattern._
import akka.util.Timeout
import com.cppdo.apibook.db.Project
import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.Await
import scala.concurrent.duration._
import com.cppdo.apibook.actor.ActorProtocols.CollectProjects

/**
 * Created by song on 5/16/15.
 */
object ActorMaster extends LazyLogging {
  implicit val timeout = Timeout(1 hour)

  def collectProjects(count: Int): Seq[Project] = {
    val projectsFuture = mavenRepositoryMaster.ask(CollectProjects(count)).mapTo[Seq[Project]]
    val projects = Await.result(projectsFuture, Duration.Inf)
    projects
  }

  def shutdown() = {
    system.shutdown()
  }

  val system = ActorSystem()
  val mavenRepositoryMaster = system.actorOf(Props(new MavenRepositoryMaster()), "maven")
  val storageMaster = system.actorOf(Props(new DbWriteActor()), "db")

}
