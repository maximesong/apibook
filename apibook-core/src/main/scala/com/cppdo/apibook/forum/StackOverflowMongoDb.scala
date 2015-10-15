package com.cppdo.apibook.forum

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoClient
import com.typesafe.scalalogging.LazyLogging
import scala.collection.JavaConverters._

import com.novus.salat._
import com.novus.salat.global._

/**
 * Created by song on 9/24/15.
 */
class StackOverflowMongoDb(host: String, dbName: String) extends LazyLogging {
  ctx.clearAllGraters()
  ctx.registerClassLoader(classOf[QuestionMethodReview].getClassLoader)

  val mongoClient = MongoClient(host)
  val db = mongoClient(dbName)
  val questionOverviewCollection = db("question_overviews")
  val questionCollection = db("questions")
  val questionReviewCollection = db("question_reviews")
  val questionMethodReviewCollection = db("question_method_reviews")

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

  def getQuestionOverviews(): Seq[QuestionOverview] = {
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
          "links" -> answer.links,
          "codeList" -> answer.codeList,
          "inlineCodeList" -> answer.inlineCodeList
        )
      })
    )
    questionCollection.update(query, update, upsert=true)
  }

  def getQuestions(): Seq[Question] = {
    val questions = questionCollection.find().sort(MongoDBObject("voteNum" -> -1)).limit(100).map(obj => {
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
              answer.as[Seq[String]]("links"),
              answer.as[Seq[String]]("codeList"),
              answer.as[Seq[String]]("inlineCodeList")
            )
          }
          case a: Any => {
            // should not go here!
            println(a.getClass)
            Answer(0, 0, true, 0, 0, 0, 0, Seq[String](),Seq[String](),Seq[String]())
          }
        },
        //Seq[Answer](),
        obj.as[Int]("codeSectionNum"),
        obj.as[Int]("linkNum")
      )
    }).toSeq
    questions
  }

  def upsertQuestionReview(questionReview: QuestionReview) = {
    val query = MongoDBObject("id" -> questionReview.id, "reviewer" -> questionReview.reviewer)
    val update = MongoDBObject(
      "id" -> questionReview.id,
      "reviewer" -> questionReview.reviewer,
      "isProgramTask" -> questionReview.isProgramTask,
      "answerIdUsingApi" -> questionReview.answerIdUsingApi
    )
    questionReviewCollection.update(query, update, upsert=true)
  }

  def getQuestionReviews(): Seq[QuestionReview] = {
    val questionReviews = questionReviewCollection.find().map(obj => {
      QuestionReview(
        obj.as[Int]("id"),
        obj.getAs[Boolean]("isProgramTask"),
        obj.getAs[Int]("answerIdUsingApi"),
        obj.getAs[Boolean]("singleKeyApi"),
        obj.as[String]("reviewer")
      )
    }).toSeq
    questionReviews
  }

  def getQuestionMethodReviews(): Seq[QuestionMethodReview] = {
    questionMethodReviewCollection.find().toSeq.map(obj => {
      grater[QuestionMethodReview].asObject(obj)
    })
  }
  def getQuestionMethodReviewsInJson(): String = {
    val reviews = getQuestionMethodReviews()
    grater[QuestionMethodReview].toPrettyJSONArray(reviews)
  }

  def upsertQuestionReviewField(id: Int, reviewer: String, field: String, value: Any): Unit = {
    val mongoClient = MongoClient(host)
    val db = mongoClient(dbName)
    val questionReviewCollection = db("question_reviews")
    val query = MongoDBObject("id" -> id, "reviewer" -> reviewer)
    val update = $set(field -> value)
    questionReviewCollection.update(query, update, upsert = true)
    mongoClient.close()
  }

  def upsertQuestionMethodReview(questionMethodReview: QuestionMethodReview): Unit = {
    val query = MongoDBObject(
      "questionId" -> questionMethodReview.questionId,
      "canonicalName" -> questionMethodReview.canonicalName,
      "reviewer" -> questionMethodReview.reviewer
    )
    val update = grater[QuestionMethodReview].asDBObject(questionMethodReview)
    questionMethodReviewCollection.update(query, update, upsert=true)
  }

  def close() = {
    mongoClient.close()
  }

}
