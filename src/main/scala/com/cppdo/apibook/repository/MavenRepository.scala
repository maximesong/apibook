package com.cppdo.apibook.repository


import com.cppdo.apibook.repository.db.{Project, Artifact}
import org.joda.time.DateTime

import scala.io.Source

/**
 * Created by song on 1/19/15.
 */
object MavenRepository {
  val projectListBaseUrl = "http://mvnrepository.com/popular"
  val baseUrl = "http://mvnrepository.com"

  val projectsPerPage = 20

  case class LibraryDetail(artifact: Artifact, downloadLink: String, releaseDateTime: DateTime)


  def getTopProjects(n: Int = projectsPerPage) : Seq[Project] = {
    val pages = Math.ceil(n.toDouble / projectsPerPage).toInt
    val projects = (1 to pages).flatMap(page => {
      getOnPage(page, MavenWebPageParser.parseProjects)
    })
    projects.take(n)
  }

  def getOnPage[A](page: Int, f: String => A) = {
    val pageUrl = s"${projectListBaseUrl}?page=${page-1}"
    val pageText = Source.fromURL(pageUrl).mkString
    f(pageText)
  }

  def getArtifactsOnPage(page: Int) : Seq[Artifact] = {
    val pageUrl = s"${projectListBaseUrl}?page=${page-1}"
    val pageText = Source.fromURL(pageUrl).mkString
    MavenWebPageParser.parseProjectLinks(pageText).flatMap(projectLink=> {
      val projectText = Source.fromURL(s"${baseUrl}/${projectLink}").mkString
      val artifacts = MavenWebPageParser.parseArtifacts(projectText)
      artifacts
    })
  }

  def testingExtractVersions() = {
    val url = "http://mvnrepository.com/artifact/com.typesafe.slick/slick_2.11"
    val content = Source.fromURL(url).mkString
    val versions = MavenWebPageParser.parseVersions(content)
    println(versions.toList)
  }

  def testingExtractProjectLinks() = {
    val url = "http://mvnrepository.com/popular"
    val content = Source.fromURL(url).mkString
    val links = MavenWebPageParser.parseProjectLinks(content)
    println(links.toList)
  }

  def testingExtractLibraryDetail() = {
    val url = "http://mvnrepository.com/artifact/joda-time/joda-time/2.7"
    val content = Source.fromURL(url).mkString
    val detail = MavenWebPageParser.parseLibraryDetail(content)
    println(detail)
  }
}
