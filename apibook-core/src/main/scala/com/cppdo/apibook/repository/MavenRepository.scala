package com.cppdo.apibook.repository


import com.cppdo.apibook.db.{Artifact, Project}
import org.joda.time.DateTime

import scala.io.Source

/**
 * Created by song on 1/19/15.
 */
object MavenRepository {
  val projectListBaseUrl = "http://mvnrepository.com/popular"
  val baseUrl = "http://mvnrepository.com"
  val VersionRegex = """(\d+)(?:\.(\d+)(?:\.(\d+))?)?(?:-(\w+))?""".r
  val projectsPerPage = 20

  case class LibraryDetail(artifact: Artifact, downloadLink: String, releaseDateTime: DateTime)

  case class Version(major: Int, minor: Option[Int], incremental: Option[Int], qualifier: Option[String])
    extends Ordered[Version] {
    override def compare(that: Version): Int = {
      val c1 = major compare that.major
      val c2 = minor.getOrElse(-1) compare that.minor.getOrElse(-1)
      val c3 = incremental.getOrElse(-1) compare that.incremental.getOrElse(-1)
      val c4 = qualifier.getOrElse("") compare that.qualifier.getOrElse("")
      if (c1 != 0) c1 else if (c2 != 0) c2 else if (c3 != 0) c3 else c4
    }
  }

  object Version {
    def parse(version: String): Version = {
      version match {
        case VersionRegex(major, minor, incremental, qualifier) => {
          Version(major.toInt, Option(minor).map(_.toInt), Option(incremental).map(_.toInt), Option(qualifier))
        }
      }
    }

    def tryParse(version: String): Option[Version] = {
      version match {
        case VersionRegex(major, minor, incremental, qualifier) => {
          Some(Version(major.toInt, Some(minor).map(_.toInt), Some(incremental).map(_.toInt), Some(qualifier)))
        }
        case _ => None
      }
    }
  }

  object VersionOrdering extends Ordering[String] {
    override def compare(x: String, y: String): Int = {
      val vx = Version.parse(x)
      val vy = Version.parse(y)
      vx compare vy
    }
  }

  def getTopProjects(n: Int = projectsPerPage) : Seq[Project] = {
    val pages = Math.ceil(n.toDouble / projectsPerPage).toInt
    val projects = (1 to pages).flatMap(page => {
      fetchFromProjectListPage(page, MavenWebPageParser.parseProjects)
    })
    projects.take(n)
  }

  private def fetchFrom[A](url: String, f: String => A) = {
    val content = Source.fromURL(url).mkString
    f(content)
  }

  private def fetchFromProjectListPage[A](page: Int, f: String => A) = {
    val pageUrl = s"${projectListBaseUrl}?page=${page-1}"
    fetchFrom(pageUrl, f)
  }

  private def detailLinkOf(project: Project) : String = "${baseUrl}/artifact/${project.group}/${project.name}"

  private def detailLinkOf(artifact: Artifact): String =
    "${baseUrl}/artifact/${artifact.group}/${artifact.name}/${artifact.version}"

  def getArtifactsForProject(project: Project) : Seq[Artifact] = {
    val projectUrl = detailLinkOf(project)
    val artifacts = fetchFrom(projectUrl, MavenWebPageParser.parseProjectDetailPage)
    artifacts
  }

  def testingExtractVersions() = {
    val url = "http://mvnrepository.com/artifact/com.typesafe.slick/slick_2.11"
    val content = Source.fromURL(url).mkString
    val versions = MavenWebPageParser.parseVersions(content)
    println(versions.toList)
  }

  def testingExtractProjectLinks() = {
    val url = "http://mvnrepository.com/popular"
    val content = Source.fromURL(url).mkString
    val links = MavenWebPageParser.parseProjectLinks(content)
    println(links.toList)
  }

  def testingExtractLibraryDetail() = {
    val url = "http://mvnrepository.com/artifact/joda-time/joda-time/2.7"
    val content = Source.fromURL(url).mkString
    val detail = MavenWebPageParser.parseLibraryDetail(content)
    println(detail)
  }

  def mostRecentVersion(versions: Seq[String]) = {

  }
}
