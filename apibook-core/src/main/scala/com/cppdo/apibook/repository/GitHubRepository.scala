package com.cppdo.apibook.repository

import com.typesafe.scalalogging.LazyLogging

import scala.io.Source

/**
 * Created by song on 5/3/15.
 */
object GitHubRepository extends LazyLogging {

  val baseUrl = "https://api.github.com/search/repositories?q=+language:java&sort=stars&order=desc&per_page=%d&page=%d"
  val perPage = 100

  def getTopProjects() = {
    val url = repositoryListUrlOfPage(1)
    logger.info(url)
    val content = Source.fromURL(url).mkString
    val json = Json.parse(content)
    logger.info(content)
  }

  def repositoryListUrlOfPage(page: Int) = {
    baseUrl.format(perPage, page)
  }
}
