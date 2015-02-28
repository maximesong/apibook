package com.cppdo.apibook.repository

import com.cppdo.apibook.repository.MavenRepository.{Artifact, LibraryDetail}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.jsoup.Jsoup
import org.jsoup.parser.Parser

import scala.collection.JavaConverters._

/**
 * Created by song on 2/28/15.
 */
object MavenWebPageParser {
  val monthYearFormat = DateTimeFormat.forPattern("(MMM, y)")
  val monthDayYearFormat = DateTimeFormat.forPattern("(MMM dd, y)")
  val ArtifactLinkRegex = "/artifact/([^/]+)/([^/]+)/([^/]+)".r()
  case class VersionLink(name: String, link: String, releaseType: String, dateTime: DateTime)

  def extractProjectLinks(html: String) : Seq[String] = {
    val document = Jsoup.parse(html)
    val links = document.select("div#maincontent div.im h2.im-title a[href]:not(.im-usage)")
    links.iterator.asScala.map(_.attr("href")).toSeq
  }

  // parse a project page, e.g. http://mvnrepository.com/artifact/com.typesafe.slick/slick_2.11
  def extractVersions(html: String)  : Seq[VersionLink] = {
    val document = Jsoup.parse(html)
    document.select("div#maincontent table.versions tbody tr").asScala.map(row => {
      val columnSize = row.children().size
      val link = row.select("td a.vbtn")
      val releaseType = row.child(columnSize - 2)
      val dateTime = row.child(columnSize - 1)
      VersionLink(link.text, link.attr("href"), releaseType.text, DateTime.parse(dateTime.text, monthYearFormat))
    }).toSeq
  }

  // parse a project page, e.g. http://mvnrepository.com/artifact/com.typesafe.slick/slick_2.11
  def extractArtifacts(html: String) : Seq[Artifact] = {
    val document = Jsoup.parse(html)
    document.select("div#maincontent table.versions tbody tr").asScala.map(row => {
      val columnSize = row.children().size
      // the link format is like "/mvnrepository.com/artifact/junit/junit/4.12-beta-3"
      val link = row.select("td a.vbtn").attr("href")
      println(link)
      link match {
        case ArtifactLinkRegex(group, name, version) => Artifact(group, name, version)
      }
    }).toSeq
  }

  // parse a project detail page, e.g. http://mvnrepository.com/artifact/joda-time/joda-time/2.7
  def extractLibraryDetail(html: String) : LibraryDetail = {
    val document = Jsoup.parse(html)
    val tableBody = document.select("div#maincontent table tbody").first()
    val link = tableBody.child(0).select("td a.vbtn").attr("href")
    val releaseDateTime = DateTime.parse(tableBody.child(2).select("td").text(), monthDayYearFormat)

    val ivyText = document.select("div#snippets div#ivy").text()
    val ivy = Jsoup.parse(ivyText, "", Parser.xmlParser()).select("dependency")
    val artifact = Artifact(ivy.attr("name"), ivy.attr("org"), ivy.attr("rev"))

    LibraryDetail(artifact, link, releaseDateTime)
  }
}
