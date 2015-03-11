package controllers

import com.cppdo.apibook.index.IndexManager
import play.api.libs.json.{JsString, JsArray}
import play.api.mvc._
import play.api.libs.json.Json

/**
 * Created by song on 3/11/15.
 */
object Search extends Controller {
  def post = Action(parse.json) { implicit request =>
    val searchText = (request.body \ "searchText").as[String]
    val resultEntries = IndexManager.search(searchText).map(document => {
      document.get("name")
    })
    val result = Json.toJson(resultEntries)
    Ok(Json.obj(
      "request" -> request.body,
      "result" -> result
    ))
  }
}
