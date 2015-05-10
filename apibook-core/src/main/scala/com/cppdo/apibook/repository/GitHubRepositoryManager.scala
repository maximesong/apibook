package com.cppdo.apibook.repository

import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.{JsValue, Json}

import scala.io.Source

/**
 * Created by song on 5/3/15.
 */
object GitHubRepositoryManager extends LazyLogging {

  val baseUrl = "https://api.github.com/search/repositories?q=+language:java&sort=stars&order=desc&per_page=%d&page=%d"
  val baseArchiveUrl = "https://github.com/%s/archive/master.zip"
  val perPage = 100

  def getTopProjects() = {
    val url = repositoryListUrlOfPage(1)
    logger.info(url)
    val content = Source.fromURL(url).mkString
    val json = Json.parse(content)
    val items = (json \ "items").as[Seq[JsValue]]
    items.foreach(item => {
      val name = (item \ "name").as[String]
      val fullName = (item \ "full_name").as[String]
      val stars = (item \ "stargazers_count").as[Int]
      logger.info(name)
      logger.info(archiveUrl(fullName))
    })
    logger.info(items.size.toString)
  }

  def repositoryListUrlOfPage(page: Int) = {
    baseUrl.format(perPage, page)
  }

  def archiveUrl(fullName: String) = {
    baseArchiveUrl.format(fullName)
  }
}
