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
  val questionBaseUrl = s"http://stackoverflow.com/questions"
  val userAgent = "apibook"
  val connectTimeout = 30 * 1000 // 10s for jsoup

  def pageUrl(page: Int) = s"${pageBaseUrl}&page=${page}"

  def questionUrl(questionId: Int) = s"${questionBaseUrl}/${questionId}"

  def fetch() = {
  }

  def fetchQuestionOverviews(count: Int): Seq[QuestionOverview] = {
    val pages = if (count % pageSize == 0) count / pageSize else count / pageSize + 1
    val overviews = (1 to pages).flatMap(page => {
      logger.info(pageUrl(page))
      val document = Jsoup.connect(pageUrl(page)).userAgent(userAgent).timeout(connectTimeout).get()
      parseListPage(document)
    })
    //logger.info(summaries.toString())
    overviews.take(count)
  }

  def fetchQuestion(questionId: Int): Question = {
    fetchQuestion(questionUrl(questionId))
  }

  def fetchQuestion(url: String): Question = {
    val doc = Jsoup.connect(url).userAgent(userAgent).timeout(connectTimeout).get()
    parseQuestionPage(doc)
  }

  def parseListPage(document: Document): Seq[QuestionOverview] = {
    val summaryElements = document.select("div#questions div.question-summary").iterator().asScala
    val summaries = summaryElements.map(summaryElement => {
      val votes = summaryElement.select("div.stats div.vote div.votes strong").first().text().toInt
      val question = summaryElement.select("div.summary a.question-hyperlink").first()
      val link = question.attr("href")
      val id = link.split("/")(2).toInt
      val title = question.text()
      val views = summaryElement.select("div.views").attr("title").replaceAll("\\D", "").toInt
      val answers = summaryElement.select("div.stats div.status strong").first().text().toInt
      val summary = QuestionOverview(id, title, s"http://stackoverflow.com${link}", votes, views, answers)
      summary
    })
    summaries.toSeq
  }

  def parseListPage(html: String): Seq[QuestionOverview] = {
    val document = Jsoup.parse(html)
    parseListPage(document)
  }

  def parseQuestionPage(document: Document): Question = {
    val viewNum = document.select("#qinfo tr:nth-child(2) td:nth-child(2) p b").text().replaceAll("\\D", "").toInt
    val voteNum = document.select("#question .votecell .vote-count-post").text().toInt
    val id = document.select("#question").attr("data-questionid").toInt
    val title = document.select("#question-header .question-hyperlink").text()
    val body = document.select("#question .post-text").first().text()
    val codeSectionNum = document.select("#question .post-text code").size()
    val linkNum = document.select("#question .post-text a").size()
    val answerElements = document.select("#answers div.answer").iterator().asScala
    val questionId = document.select("#question").attr("data-questionid").toInt
    val answers = answerElements.map(answerElement => {
      val id = answerElement.attr("data-answerid").toInt
      val votes = answerElement.select("div.vote span.vote-count-post").first().text().toInt
      val accepted = answerElement.hasClass("accepted-answer")
      val codeSections = answerElement.select("div.post-text code").size()
      val codeList = answerElement.select(".post-text pre code").iterator().asScala.map(_.text()).toSeq
      val inlineCodeList = answerElement.select(".post-text p code").iterator().asScala.map(_.text()).toSeq
      val linkNum = answerElement.select(".answercell div.post-text a").size()
      val links = answerElement.select(".answercell div.post-text a").iterator().asScala.map(link => {
        link.attr("href")
      }).toSeq
      println(answerElement.select(".answercell .user-info .reputation-score"))
      val authorReputation = Option(answerElement.select(".answercell .user-info .reputation-score").last()).map(reputationElement => {
        val reputationText = reputationElement.attr("title").replaceAll("\\D", "")
        println("TEXT:" + reputationText)
        if (reputationText.length > 0) {
          reputationText.toInt
        } else {
          reputationElement.text().replaceAll("\\D", "").toInt
        }
      }).getOrElse(0)
      println("author:" + authorReputation)
      val answer = Answer(id, questionId, accepted, votes, authorReputation, codeSections, linkNum, links, codeList, inlineCodeList)
      println(title)
      println(questionId)
      println(votes)
      println(answer.authorReputation)
      answer
    }).toSeq
    Question(id, title, body, voteNum, viewNum, answers, codeSectionNum, linkNum)
  }
}
