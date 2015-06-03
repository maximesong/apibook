package controllers

import com.cppdo.apibook.db.DatabaseManager
import com.cppdo.apibook.index.IndexManager
import com.cppdo.apibook.index.IndexManager.FieldName
import play.api.libs.json.{JsString, JsArray}
import play.api.mvc._
import play.api.libs.json.Json
import com.cppdo.apibook.repository.ArtifactsManager.RichArtifact
import com.cppdo.apibook.repository.MavenRepository.MavenArtifact
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
        case `typeClass` => {
          val id = document.get(FieldName.DbId.toString).toInt
          val klass = DatabaseManager.getKlass(id)
          val artifact = DatabaseManager.getArtifact(klass.artifactId)
          Json.obj(
            FieldName.Name.toString -> document.get(FieldName.Name.toString),
            FieldName.Type.toString -> typeClass,
            "Artifact" -> artifact.name,
            "DownloadUrl" -> artifact.libraryPackageUrl
          )
        }
        case `typeMethod` => {
          val id = document.get(FieldName.DbId.toString).toInt
          val method = DatabaseManager.getMethod(id)
          val klass = DatabaseManager.getKlass(method.enclosingClassId)
          val artifact = DatabaseManager.getArtifact(klass.artifactId)
          Json.obj(
            FieldName.Name.toString -> document.get(FieldName.Name.toString),
            FieldName.Type.toString -> typeMethod,
            "Class" -> klass.fullName,
            "Artifact" -> artifact.name,
            "Id" -> document.get(FieldName.DbId.toString),
            "DownloadUrl" -> artifact.libraryPackageUrl
          )
        }
      }
    })
    val result = Json.toJson(resultEntries)
    Ok(Json.obj(
      "request" -> request.body,
      "result" -> result
    ))
  }
}
