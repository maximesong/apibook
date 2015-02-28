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
class MavenRepository {
  val baseUrl = "http://mvnrepository.com/popular"
  val monthYearFormat = DateTimeFormat.forPattern("(MMM, y)")
  val monthDayYearFormat = DateTimeFormat.forPattern("(MMM dd, y)")

  case class VersionLink(name: String, link: String, releaseType: String, dateTime: DateTime)

  case class LibraryDetail(artifact: Artifact, downloadLink: String, releaseDateTime: DateTime)

  case class Artifact(name: String, group: String, version: String)

  // parse a project list page, e.g. http://mvnrepository.com/popular
  private def extractProjectLinks(html: String) = {
    val document = Jsoup.parse(html)
    val links = document.select("div#maincontent div.im h2.im-title a[href]:not(.im-usage)")
    links.iterator.asScala.map(_.attr("href")).toSeq
  }

  // parse a project page, e.g. http://mvnrepository.com/artifact/com.typesafe.slick/slick_2.11
  private def extractVersions(html: String) = {
    val document = Jsoup.parse(html)
    document.select("div#maincontent table.versions tbody tr").asScala.map(row => {
      val columnSize = row.children().size
      val link = row.select("td a.vbtn")
      val releaseType = row.child(columnSize - 2)
      val dateTime = row.child(columnSize - 1)
      VersionLink(link.text, link.attr("href"), releaseType.text, DateTime.parse(dateTime.text, monthYearFormat))
    }).toSeq
  }

  // parse a project detail page, e.g. http://mvnrepository.com/artifact/joda-time/joda-time/2.7
  private def extractLibraryDetail(html: String) = {
    val document = Jsoup.parse(html)
    val tableBody = document.select("div#maincontent table tbody").first()
    val link = tableBody.child(0).select("td a.vbtn").attr("href")
    val releaseDateTime = DateTime.parse(tableBody.child(2).select("td").text(), monthDayYearFormat)

    val ivyText = document.select("div#snippets div#ivy").text()
    val ivy = Jsoup.parse(ivyText, "", Parser.xmlParser()).select("dependency")
    val artifact = Artifact(ivy.attr("name"), ivy.attr("org"), ivy.attr("rev"))

    LibraryDetail(artifact, link, releaseDateTime)
  }

  def testingExtractVersions() = {
    val url = "http://mvnrepository.com/artifact/com.typesafe.slick/slick_2.11"
    val content = Source.fromURL(url).mkString
    val versions = extractVersions(content)
    println(versions.toList)
  }

  def testingExtractProjectLinks() = {
    val url = "http://mvnrepository.com/popular"
    val content = Source.fromURL(url).mkString
    val links = extractProjectLinks(content)
    println(links.toList)
  }

  def testingExtractLibraryDetail() = {
    val url = "http://mvnrepository.com/artifact/joda-time/joda-time/2.7"
    val content = Source.fromURL(url).mkString
    val detail = extractLibraryDetail(content)
    println(detail)
  }

  def getList() = {
    testingExtractLibraryDetail()
  }
}
