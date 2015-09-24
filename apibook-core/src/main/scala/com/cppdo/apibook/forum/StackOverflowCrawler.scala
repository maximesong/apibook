package com.cppdo.apibook.forum

import com.cppdo.apibook.db.Project
import com.typesafe.scalalogging.LazyLogging
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

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
  val userAgent = "apibook"

  def pageUrl(page: Int) = s"${pageBaseUrl}&page=${page}"

  def fetch() = {

    logger.info("Feftch")
    val doc = Jsoup.connect(pageUrl(1)).userAgent(userAgent).get()
    logger.info(pageUrl(1))
    //val html = Source.fromURL("https://stackoverflow.com/").mkString
    logger.info(doc.toString)
    //logger.info(content)

    //parseListPage(html)
  }

  def fetchQuestionSummaries(count: Int): Seq[QuestionSummary] = {
    val pages = count / pageSize + 1
    val summaries = (1 to pages).flatMap(page => {
      logger.info(pageUrl(page))
      val document = Jsoup.connect(pageUrl(page)).userAgent(userAgent).get()
      parseListPage(document)
    })
    //logger.info(summaries.toString())
    summaries.take(count)
  }

  def parseListPage(document: Document): Seq[QuestionSummary] = {
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

  def parseListPage(html: String): Seq[QuestionSummary] = {
    val document = Jsoup.parse(html)
    parseListPage(document)
  }

}
