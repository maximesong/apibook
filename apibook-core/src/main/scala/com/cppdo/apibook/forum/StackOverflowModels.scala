package com.cppdo.apibook.forum
import play.api.libs.json._

/**
 * Created by song on 9/24/15.
 */
case class QuestionOverview(id: Int, title: String, questionUrl: String, voteNum: Int, viewNum: Int, answerNum: Int)

object QuestionOverview {
  implicit val overviewWrites = new Writes[QuestionOverview] {
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

object Question {
  implicit val answerWrites = new Writes[Answer] {
    def writes(answer: Answer) = Json.obj(
      "id" -> answer.id,
      "questionId" -> answer.questionId,
      "accepted" -> answer.accepted,
      "voteNum" -> answer.voteNum,
      "authorReputation" -> answer.authorReputation,
      "codeSectionNum" -> answer.codeSectionNum,
      "linkNum" -> answer.linkNum,
      "links" -> answer.links,
      "codeList" -> answer.codeList,
      "inlineCodeList" -> answer.inlineCodeList
    )
  }
  implicit val questionsWrites = new Writes[Question] {
    def writes(question: Question) = Json.obj(
      "id" -> question.id,
      "title" -> question.title,
      "body" -> question.body,
      "codeSectionNum" -> question.codeSectionNum,
      "linkNum" -> question.linkNum,
      "answers" -> question.answers,
      "viewNum" -> question.viewNum,
      "voteNum" -> question.voteNum
    )
  }

}

case class Question(id: Int, title: String, body: String, voteNum: Int, viewNum: Int, answers: Seq[Answer],
                    codeSectionNum: Int, linkNum: Int)


object Answer {
  implicit val answerWrites = new Writes[Answer] {
    def writes(answer: Answer) = Json.obj(
      "id" -> answer.id,
      "questionId" -> answer.questionId,
      "accepted" -> answer.accepted,
      "voteNum" -> answer.voteNum,
      "authorReputation" -> answer.authorReputation,
      "codeSectionNum" -> answer.codeSectionNum,
      "linkNum" -> answer.linkNum,
      "links" -> answer.links,
      "codeList"  -> answer.codeList,
      "inlineCodeList" -> answer.inlineCodeList
    )
  }
}
case class Answer(id: Int, questionId:Int, accepted: Boolean, voteNum: Int, authorReputation: Int,
                  codeSectionNum: Int, linkNum: Int, links: Seq[String], codeList: Seq[String], inlineCodeList: Seq[String])

case class QuestionReview(id: Int, isProgramTask: Boolean,
                          answerIdUsingApi: Int = 0, reviewer: String = "author")

object QuestionReview {
  implicit val questionReviewWrites = new Writes[QuestionReview] {
    def writes(questionReview: QuestionReview) = Json.obj(
      "id" -> questionReview.id,
      "reviewer" -> questionReview.reviewer,
      "isProgramTask" -> questionReview.isProgramTask,
      "answerIdUsingApi" -> questionReview.answerIdUsingApi
    )
  }
}