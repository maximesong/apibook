package controllers

import com.cppdo.apibook.index.IndexManager
import com.cppdo.apibook.index.IndexManager.FieldName
import play.api.libs.json.{JsString, JsArray}
import play.api.mvc._
import play.api.libs.json.Json

/**
 * Created by song on 3/11/15.
 */
object Search extends Controller {
  def post = Action(parse.json) { implicit request =>
    val searchText = (request.body \ "searchText").as[String]
    val resultEntries = IndexManager.trivial_search(searchText).map(document => {
      val documentType = document.get(IndexManager.FieldName.Type.toString)
      val typeClass = IndexManager.DocumentType.Class.toString
      val typeMethod = IndexManager.DocumentType.Method.toString
      documentType match {
        case `typeClass` => Json.obj(
          FieldName.Name.toString -> document.get(FieldName.Name.toString),
          FieldName.Type.toString -> typeClass,
          "Id" -> document.get(FieldName.DbId.toString)
        )
        case `typeMethod` => Json.obj(
          FieldName.Name.toString -> document.get(FieldName.Name.toString),
          FieldName.Type.toString -> typeMethod,
          "Id" -> document.get(FieldName.DbId.toString)
        )
      }
    })
    val result = Json.toJson(resultEntries)
    Ok(Json.obj(
      "request" -> request.body,
      "result" -> result
    ))
  }
}
