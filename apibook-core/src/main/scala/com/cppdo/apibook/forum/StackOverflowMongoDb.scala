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
  val questionCollection = db("questions")
  val answerCollection = db("answers")

  def upsertQuestionSummary(summary: QuestionSummary) = {
    val query = MongoDBObject("id" -> summary.id)
    val update = MongoDBObject("id" -> summary.id, "votes" -> summary.votes, "title" -> summary.title, "link" -> summary.link,
      "views" -> summary.views, "answers" -> summary.answers)
    questionCollection.update(query, update, upsert=true)
  }

  def getQuestionSummaries() = {
    val summaries = questionCollection.find().map(obj => {
      QuestionSummary(
        obj.as[Int]("id"),
        obj.as[String]("title"),
        obj.as[String]("link"),
        obj.as[Int]("votes"),
        obj.as[Int]("views"),
        obj.as[Int]("answers")
      )
    }).toSeq
    summaries
  }

}
