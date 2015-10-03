package com.cppdo.apibook.forum
import play.api.libs.json._

/**
 * Created by song on 9/24/15.
 */
case class QuestionOverview(id: Int, title: String, questionUrl: String, voteNum: Int, viewNum: Int, answerNum: Int)

object QuestionSummary {
  implicit val locationWrites = new Writes[QuestionOverview] {
    def writes(overview: QuestionOverview) = Json.obj(
      "id" -> overview.id,
      "title" -> overview.title,
      "questionUrl" -> overview.questionUrl,
      "voteNum" -> overview.voteNum,
      "viewNum" -> overview.viewNum,
      "answerNum" -> overview.answerNum
    )
  }
}

case class Question(id: Int, title: String, body: String, voteNum: Int, viewNum: Int, answers: Seq[Answer],
                    codeSectionNum: Int, linkNum: Int)

case class Answer(id: Int, questionId:Int, accepted: Boolean, voteNum: Int, authorReputation: Int,
                  codeSectionNum: Int, linkNum: Int, links: Seq[String])
