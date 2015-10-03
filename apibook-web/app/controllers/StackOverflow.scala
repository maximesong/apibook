package controllers

import play.api.http.ContentTypes
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

import com.cppdo.apibook.forum.QuestionOverview
import com.cppdo.apibook.forum.Answer
import com.cppdo.apibook.forum.StackOverflowMongoDb

/**
 * Created by song on 9/24/15.
 */
object StackOverflow extends Controller {
  def overviews = Action {
    val client = new StackOverflowMongoDb("localhost", "apibook")
    val overviews = client.getQuestionOverviews()
    Ok(Json.toJson(overviews))
  }

  def questions = Action {
    val client = new StackOverflowMongoDb("localhost", "apibook")
    val questions = client.getQuestions()
    Ok(Json.prettyPrint(Json.toJson(questions))).as(ContentTypes.JSON)
  }
}
