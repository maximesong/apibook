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
      val views = summaryElement.select("div.views").attr("title").replaceAll("\\D", "").toInt
      val answers = summaryElement.select("div.stats div.status strong").first().text().toInt
      println(views)
      val summary = QuestionSummary(id, title, s"http://stackoverflow.com${link}", votes, views, answers)
      summary
    })
    summaries.toSeq
  }

  def parseListPage(html: String): Seq[QuestionSummary] = {
    val document = Jsoup.parse(html)
    parseListPage(document)
  }

  def parseDetailPage(document: Document): Seq[Answer] = {
    val postText = document.select("div.post-text").first()
    val answerElements = document.select("div#answers div.answer").iterator().asScala
    val answers = answerElements.map(answerElement => {
      println("Hi")
      val id = answerElement.attr("data-answerid").toInt
      val votes = answerElement.select("div.vote span.vote-count-post").first().text().toInt
      val accepted = answerElement.hasClass("accepted-answer")
      val codeSections = answerElement.select("div.post-text code").size()
      val links = answerElement.select(".answercell div.post-text a").size()
      println(answerElement.select(".answercell .user-info .reputation-score"))
      val authorReputation = Option(answerElement.select(".answercell .user-info .reputation-score").first()).map(reputationElement => {
        val reputationText = reputationElement.attr("title").replaceAll("\\D", "")
        if (reputationText.size > 0) {
          reputationText.toInt
        } else {
          reputationElement.text().replaceAll("\\D", "").toInt
        }
      }).getOrElse(0)
      val answer = Answer(id, votes, accepted, codeSections, links, authorReputation)
      logger.info(answer.toString)
      answer
    })
    println(answers.size)
    answers.toSeq
  }
}
