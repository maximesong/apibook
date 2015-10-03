package com.cppdo.apibook.forum

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoClient
import com.typesafe.scalalogging.LazyLogging

/**
 * Created by song on 9/24/15.
 */
class StackOverflowMongoDb(host: String, dbName: String) extends LazyLogging {
  val mongoClient = MongoClient(host)
  val db = mongoClient(dbName)
  val questionOverviewCollection = db("question_overviews")
  val questionCollection = db("questions")

  def upsertQuestionOverview(overview: QuestionOverview) = {
    val query = MongoDBObject("id" -> overview.id)
    val update = MongoDBObject(
      "id" -> overview.id,
      "voteNum" -> overview.voteNum,
      "title" -> overview.title,
      "questionUrl" -> overview.questionUrl,
      "viewNum" -> overview.viewNum,
      "answerNum" -> overview.answerNum)
    questionOverviewCollection.update(query, update, upsert=true)
  }

  def getQuestionOverviews() = {
    val overviews = questionOverviewCollection.find().map(obj => {
      QuestionOverview(
        obj.as[Int]("id"),
        obj.as[String]("title"),
        obj.as[String]("link"),
        obj.as[Int]("voteNum"),
        obj.as[Int]("viewNum"),
        obj.as[Int]("answerNum")
      )
    }).toSeq
    overviews
  }

  def upsertQuestion(question: Question) = {
    val query = MongoDBObject("id" -> question.id)
    val update = MongoDBObject(
      "id" -> question.id,
      "linkNum" -> question.linkNum,
      "voteNum" -> question.voteNum,
      "codeSectionNum" -> question.codeSectionNum,
      "title" -> question.title,
      "body" -> question.body,
      "answers" -> MongoDBList(question.answers.map( answer => {
        MongoDBObject(
          "id" -> answer.id,
          "accepted" -> answer.accepted,
          "questionId" -> answer.questionId,
          "voteNum" -> answer.voteNum,
          "linkNum" -> answer.linkNum,
          "codeSectionNum" -> answer.codeSectionNum,
          "authorReputation" -> answer.authorReputation,
          "links" -> answer.links
        )
      }))
    )
    questionCollection.update(query, update, upsert=true)
  }

  def getQuestions() = {

  }
}
