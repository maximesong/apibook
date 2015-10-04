package com.cppdo.apibook.forum

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoClient
import com.typesafe.scalalogging.LazyLogging
import scala.collection.JavaConverters._

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
      "viewNum" -> question.viewNum,
      "codeSectionNum" -> question.codeSectionNum,
      "title" -> question.title,
      "body" -> question.body,
      "answers" -> question.answers.map( answer => {
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
      })
    )
    questionCollection.update(query, update, upsert=true)
  }

  def getQuestions() = {
    val questions = questionCollection.find().map(obj => {
      val answers = obj.as[MongoDBList]("answers")
      Question(
        obj.as[Int]("id"),
        obj.as[String]("title"),
        obj.as[String]("body"),
        obj.as[Int]("voteNum"),
        obj.as[Int]("viewNum"),
        obj.as[BasicDBList]("answers").map{
          case answer: BasicDBObject => {
            Answer(
              answer.as[Int]("id"),
              answer.as[Int]("questionId"),
              answer.as[Boolean]("accepted"),
              answer.as[Int]("voteNum"),
              answer.as[Int]("authorReputation"),
              answer.as[Int]("codeSectionNum"),
              answer.as[Int]("linkNum"),
              answer.as[Seq[String]]("links")
            )
          }
          case a: Any => {
            println(a.getClass)
            Answer(0, 0, true, 0, 0, 0, 0, Seq[String]())
          }
        },
        //Seq[Answer](),
        obj.as[Int]("codeSectionNum"),
        obj.as[Int]("linkNum")
      )
    }).toSeq
    questions
  }
}
