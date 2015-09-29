package com.cppdo.apibook.forum
import play.api.libs.json._

/**
 * Created by song on 9/24/15.
 */
case class QuestionSummary(id: Int, title: String, link: String, votes: Int, views: Int, answers: Int)

object QuestionSummary {
  implicit val locationWrites = new Writes[QuestionSummary] {
    def writes(summary: QuestionSummary) = Json.obj(
      "id" -> summary.id,
      "title" -> summary.title,
      "link" -> summary.link,
      "votes" -> summary.votes,
      "views" -> summary.views,
      "answers" -> summary.answers
    )
  }
}

case class Question(id: Int)

case class Answer(id: Int, votes: Int, accepted: Boolean, codeSections: Int, links: Int, authorReputation: Int)
