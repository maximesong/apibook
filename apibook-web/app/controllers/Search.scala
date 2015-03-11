package controllers

import com.cppdo.apibook.index.IndexManager
import play.api.mvc._
/**
 * Created by song on 3/11/15.
 */
object Search extends Controller {
  def post = Action(parse.json) { implicit request =>
    IndexManager
    Ok(request.body)
  }
}
