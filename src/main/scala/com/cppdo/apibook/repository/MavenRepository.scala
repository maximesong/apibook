package com.cppdo.apibook.repository

import java.time.format.DateTimeFormatter

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import scala.collection.JavaConverters._

import scala.io.Source

/**
 * Created by song on 1/19/15.
 */
object MavenRepository {
  val projectListBaseUrl = "http://mvnrepository.com/popular"
  val baseUrl = "http://mvnrepository.com"

  val projectsPerPage = 10

  case class LibraryDetail(artifact: Artifact, downloadLink: String, releaseDateTime: DateTime)

  case class Artifact(name: String, group: String, version: String)

  def getTopArtifacts(count: Int = 10) : Seq[Artifact] = {
    val pages = count / projectsPerPage
    (1 to pages).flatMap(page => {
      getArtifactsOnPage(page)
    })
  }

  def getArtifactsOnPage(page: Int) : Seq[Artifact] = {
    val pageUrl = s"${projectListBaseUrl}?page=${page-1}"
    val pageText = Source.fromURL(pageUrl).mkString
    MavenWebPageParser.extractProjectLinks(pageText).flatMap(projectLink=> {
      val projectText = Source.fromURL(s"${baseUrl}/${projectLink}").mkString
      val artifacts = MavenWebPageParser.extractArtifacts(projectText)
      artifacts
    })
  }

  def testingExtractVersions() = {
    val url = "http://mvnrepository.com/artifact/com.typesafe.slick/slick_2.11"
    val content = Source.fromURL(url).mkString
    val versions = MavenWebPageParser.extractVersions(content)
    println(versions.toList)
  }

  def testingExtractProjectLinks() = {
    val url = "http://mvnrepository.com/popular"
    val content = Source.fromURL(url).mkString
    val links = MavenWebPageParser.extractProjectLinks(content)
    println(links.toList)
  }

  def testingExtractLibraryDetail() = {
    val url = "http://mvnrepository.com/artifact/joda-time/joda-time/2.7"
    val content = Source.fromURL(url).mkString
    val detail = MavenWebPageParser.extractLibraryDetail(content)
    println(detail)
  }
}
