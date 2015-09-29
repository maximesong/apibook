package controllers

import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

import com.cppdo.apibook.forum.QuestionSummary
import com.cppdo.apibook.forum.StackOverflowMongoDb

/**
 * Created by song on 9/24/15.
 */
object StackOverflow extends Controller {
  def summaries = Action {
    val client = new StackOverflowMongoDb("localhost", "apibook")
    val summaries = client.getQuestionSummaries()
    Ok(Json.toJson(summaries))
  }
}
