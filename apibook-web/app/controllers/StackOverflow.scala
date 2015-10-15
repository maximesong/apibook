package controllers

import play.api.http.ContentTypes
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

import com.cppdo.apibook.forum._

/**
 * Created by song on 9/24/15.
 */
object StackOverflow extends Controller {
  def overviews = Action {
    val client = new StackOverflowMongoDb("localhost", "apibook")
    val overviews = client.getQuestionOverviews()
    client.close()
    Ok(Json.toJson(overviews))
  }

  def questions = Action {
    val client = new StackOverflowMongoDb("localhost", "apibook")
    val questions = client.getQuestions().toList
    client.close()
    Ok(Json.prettyPrint(Json.toJson(questions))).as(ContentTypes.JSON)
  }

  def questionMethodReviews = Action {
    var client = new StackOverflowMongoDb("localhost", "apibook")
    val reviewsJson = client.getQuestionMethodReviewsInJson()
    client.close()
    Ok(reviewsJson).as(ContentTypes.JSON)
  }

  def questionReviews = Action {
    var client = new StackOverflowMongoDb("localhost", "apibook")
    val questionReviews = client.getQuestionReviews().toList
    client.close()
    Ok(Json.prettyPrint(Json.toJson(questionReviews))).as(ContentTypes.JSON)
  }

  def upsertQuestionReview(id: Int) = Action(parse.json) { request =>
    Ok(request.body)
  }

  def upsertQuestionReviewField(id: Int) = Action(parse.json) { request =>
    val field = (request.body \ "field").as[String]
    val value = (request.body \ "value")
    val parsed = value.asOpt[Boolean].getOrElse(
      value.asOpt[Int].getOrElse(value.as[String])
    )
    val reviewer = (request.body \ "reviewer").as[String]
    val client = new StackOverflowMongoDb("localhost", "apibook")
    client.upsertQuestionReviewField(id, reviewer, field, parsed)
    client.close()
    Ok(Json.obj(
      "result" -> 200
    ))
  }

  def upsertQuestionMethodReview(id: Int) = Action(parse.json) { request =>
    val canonicalName = (request.body \ "canonicalName").as[String]
    val methodFullName = (request.body \ "methodFullName").as[String]
    val relevance = (request.body \ "relevance").as[String]
    val reviewer = (request.body \ "reviewer").as[String]
    val review = QuestionMethodReview(id, canonicalName, methodFullName, Some(relevance), reviewer)
    val client = new StackOverflowMongoDb("localhost", "apibook")
    client.upsertQuestionMethodReview(review)
    client.close()
    Ok(Json.obj(
      "result" -> 200
    ))
  }
}
