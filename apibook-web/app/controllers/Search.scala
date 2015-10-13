package controllers

import java.io.File
import java.util.jar.JarFile
import java.util.zip.ZipException

import com.cppdo.apibook.ast.JarManager
import com.cppdo.apibook.db.DatabaseManager
import com.cppdo.apibook.index.IndexManager
import com.cppdo.apibook.index.IndexManager.FieldName
import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.io.FileUtils
import play.api.libs.json.{JsString, JsArray}
import play.api.mvc._
import play.api.libs.json.Json
import com.cppdo.apibook.repository.ArtifactsManager.{RichArtifact, RichDocEntry}
import com.cppdo.apibook.repository.MavenRepository.MavenArtifact
/**
 * Created by song on 3/11/15.
 */
object Search extends Controller with LazyLogging {
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
          logger.info(artifact.fullDocPackagePath)
          val optionDocPath = if (new File(artifact.fullDocPackagePath).exists()) {
            val jarFile = new JarFile(artifact.fullDocPackagePath)
            val optionEntry = JarManager.getDocEntry(jarFile, klass)
            optionEntry.map(e => {
              val inputStream = jarFile.getInputStream(e)
              FileUtils.copyInputStreamToFile(inputStream, new File(e.docSavePath))
              e.docPath
            })
          } else {
            None
          }
          Json.obj(
            FieldName.Name.toString -> document.get(FieldName.Name.toString),
            FieldName.Type.toString -> typeClass,
            "Artifact" -> artifact.name,
            "DownloadUrl" -> artifact.libraryPackageUrl,
            "DocPath" -> optionDocPath.getOrElse("").toString
          )
        }
        case `typeMethod` => {
          val id = document.get(FieldName.DbId.toString).toInt
          val method = DatabaseManager.getMethod(id)
          val klass = DatabaseManager.getKlass(method.enclosingClassId)
          val artifact = DatabaseManager.getArtifact(klass.artifactId)
          logger.info(artifact.fullDocPackagePath)
          val optionDocPath = if (new File(artifact.fullDocPackagePath).exists()) {
            val jarFile = new JarFile(artifact.fullDocPackagePath)
            val optionEntry = JarManager.getDocEntry(jarFile, klass)
            optionEntry.map(e => {
              val inputStream = jarFile.getInputStream(e)
              FileUtils.copyInputStreamToFile(inputStream, new File(e.docSavePath))
              e.docPath
            })
          } else {
            None
          }

          Json.obj(
            FieldName.Name.toString -> document.get(FieldName.Name.toString),
            FieldName.Type.toString -> typeMethod,
            "Class" -> klass.fullName,
            "Artifact" -> artifact.name,
            "Id" -> document.get(FieldName.DbId.toString),
            "DownloadUrl" -> artifact.libraryPackageUrl,
            "DocPath" -> optionDocPath.getOrElse("").toString
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

  def searchMethod = Action(parse.json) { implicit request =>
    val searchText = (request.body \ "searchText").as[String]
    val resultEntries = IndexManager.searchMethod(searchText).map(document => {
      document.get(IndexManager.FieldName.FullName.toString)
    })
    Ok(Json.obj(
      "result" -> resultEntries
    ))
  }
}
