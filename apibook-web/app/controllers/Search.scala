package controllers

import java.io.File
import java.util.jar.JarFile
import java.util.zip.ZipException

import com.cppdo.apibook.ast.JarManager
import com.cppdo.apibook.db.DatabaseManager
import com.cppdo.apibook.index.IndexManager
import com.cppdo.apibook.index.IndexManager.FieldName
import com.cppdo.apibook.nlp.CoreNLP
import com.cppdo.apibook.search.{MethodScore, SearchManager}
import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.io.FileUtils
import play.api.Play
import play.api.libs.json.{JsString, JsArray}
import play.api.mvc._
import play.api.libs.json.Json
import com.cppdo.apibook.repository.ArtifactsManager.{RichArtifact, RichDocEntry}
import com.cppdo.apibook.repository.MavenRepository.MavenArtifact

import play.api.Play.current




/**
 * Created by song on 3/11/15.
 */
object Search extends Controller with LazyLogging {

  val dbHost = "localhost"
  val dbName = "apibook"

  def searchMethod = Action(parse.json) { implicit request =>
    val searchText = (request.body \ "searchText").as[String]
    val searchEngine = (request.body \ "searchEngine").asOpt[String].getOrElse("V2")
    val godModeTypes = (request.body \ "godModeTypes").asOpt[Seq[String]].getOrElse(Seq[String]())
    val searchManager = new SearchManager(dbHost, dbName, classLoader=Some(Play.classloader))
    val methodScoreDetails = if (searchEngine == "GodMode") {
      searchManager.searchGodModeAndReturnJson(searchText, godModeTypes, 100, searchEngine).toList
    } else {
      searchManager.searchAndReturnJson(searchText, 100, searchEngine).toList
    }
    searchManager.close()
    Ok(Json.obj(
      "result" -> methodScoreDetails
    ))
  }

  def searchSnippets = Action(parse.json) { implicit request =>
    val canonicalName = (request.body \ "canonicalName").as[String]
    val searchManager = new SearchManager(dbHost, dbName, classLoader=Some(Play.classloader))
    logger.info(canonicalName)
    val codeSnippets = searchManager.findUsageSnippetsOfCanonicalName(canonicalName, 2).toList
    searchManager.close()
    Ok(Json.obj(
      "result" -> codeSnippets
    ))
  }
}
