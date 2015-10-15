package controllers

import com.cppdo.apibook.index.IndexManager
import play.api.Play._
import play.api._
import play.api.mvc._

object Application extends Controller {
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def stackoverflow = Action {
    Ok(views.html.stackoverflow())
  }

}