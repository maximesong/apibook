package com.cppdo.apibook.repository

import com.cppdo.apibook.db.GitHubRepository
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.{JsValue, Json}

import scala.io.Source

/**
 * Created by song on 5/3/15.
 */
object GitHubRepositoryManager extends LazyLogging {

  val baseUrl = "https://api.github.com/search/repositories?q=+language:java&sort=stars&order=desc&per_page=%d&page=%d"
  val baseArchiveUrl = "https://github.com/%s/archive/master.zip"
  val baseDirectory = "client-repository"
  val perPage = 100


  def getTopRepositories(count: Int): Seq[GitHubRepository] = {
    val pages = (count.toDouble / perPage.toDouble).ceil.toInt
    (1 to pages).flatMap(page => {
      getRepositoriesOnPage(page)
    }).take(count)
  }

  def getRepositoriesOnPage(page: Int): Seq[GitHubRepository] = {
    val url = repositoryListUrlOfPage(page)
    logger.info(url)
    val content = Source.fromURL(url).mkString
    val json = Json.parse(content)
    val items = (json \ "items").as[Seq[JsValue]]
    val repositories = items.map(item => {
      val fullName = (item \ "full_name").as[String]
      val name = (item \ "name").as[String]
      val stars = (item \ "stargazers_count").as[Int]
      val repository = GitHubRepository(fullName, name, stars)
      repository
    })
    repositories
  }

  def repositoryListUrlOfPage(page: Int) = {
    baseUrl.format(perPage, page)
  }

  def archiveUrl(fullName: String) = {
    baseArchiveUrl.format(fullName)
  }
  implicit class RichGitHubRepository(repository: GitHubRepository) {
    def fullSourcePath: String = s"${baseDirectory}/${repository.fullName}/master.zip"

    def archiveUrl: String = GitHubRepositoryManager.archiveUrl(repository.fullName)
  }
}
