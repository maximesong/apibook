package com.cppdo.apibook.forum

import com.cppdo.apibook.db.Project
import com.typesafe.scalalogging.LazyLogging
import org.jsoup.Jsoup

import scala.io.Source
import scala.util.matching.Regex
import scala.collection.JavaConverters._


/**
 * Created by song on 7/21/15.
 */

case class QuestionSummary(id: Int, title: String, link: String, votes: Int)
object StackOverflowCrawler extends LazyLogging {
  val pageSize = 50
  val pageBaseUrl = s"http://stackoverflow.com/questions/tagged/java?sort=votes&pagesize=${pageSize}"

  def pageUrl(page: Int) = s"${pageBaseUrl}&page=${page}"

  def fetch() = {

    val html = Source.fromURL(pageUrl(1)).mkString
    //logger.info(content)

    parseListPage(html)
  }

  def fetchQuestionSummaries(count: Int): Seq[QuestionSummary] = {
    val pages = count / pageSize + 1
    val summaries = (1 to pages).flatMap(page => {
      val html = Source.fromURL(pageUrl(page)).mkString
      parseListPage(html)
    })
    //logger.info(summaries.toString())
    summaries.take(count)
  }

  def parseListPage(html: String): Seq[QuestionSummary] = {
    val document = Jsoup.parse(html)
    val summaryElements = document.select("div#questions div.question-summary").iterator().asScala
    val summaries = summaryElements.map(summaryElement => {
      //logger.info(summary.toString)
      val votes = summaryElement.select("div.stats div.vote div.votes strong").first().text().toInt
      //logger.info(votes.toString)
      val question = summaryElement.select("div.summary a.question-hyperlink").first()
      val link = question.attr("href")
      val id = link.split("/")(2).toInt
      val title = question.text()
      val summary = QuestionSummary(id, title, s"http://stackoverflow.com${link}", votes)
      summary
    })
    summaries.toSeq
  }

}
