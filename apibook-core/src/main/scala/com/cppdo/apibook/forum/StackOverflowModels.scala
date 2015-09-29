package com.cppdo.apibook.forum
import play.api.libs.json._

/**
 * Created by song on 9/24/15.
 */
case class QuestionSummary(id: Int, title: String, link: String, votes: Int)

object QuestionSummary {
  implicit val locationWrites = new Writes[QuestionSummary] {
    def writes(summary: QuestionSummary) = Json.obj(
      "id" -> summary.id,
      "title" -> summary.title,
      "link" -> summary.link,
      "votes" -> summary.votes
    )
  }
}
