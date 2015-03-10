package com.cppdo.apibook.repository


import java.io.File
import java.net.URL

import com.cppdo.apibook.db.{Artifact, Project}
import org.apache.commons.io.FileUtils
import org.joda.time.DateTime

import scala.io.Source

/**
 * Created by song on 1/19/15.
 */
object MavenRepository {
  val projectListBaseUrl = "http://mvnrepository.com/popular"
  val baseUrl = "http://mvnrepository.com"
  val baseDownloadUrl = "http://central.maven.org/maven2"
  val VersionRegex = """(\d+)(?:\.(\d+)(?:\.(\d+))?)?(?:-([\w-.]+))?""".r
  val projectsPerPage = 20
  val reasonableMaxMajorVersion = 100

  case class LibraryDetail(artifact: Artifact, downloadLink: String, releaseDateTime: DateTime)

  case class Version(major: Int, minor: Option[Int] = None, incremental: Option[Int] = None, qualifier: Option[String] = None)
    extends Ordered[Version] {

    val QualifierRegex = """(?i)(m|a|alpha|b|beta|r|rc|final)(?:-?(\d+))?""".r

    def matchQualifierToVersion(qualifier: String): Int = {
     qualifier match {
       case "" => 0
       case QualifierRegex(releaseType, version) => {
         val major = releaseType match {
           case s if s matches "(?i)m" => -900
           case s if s matches "(?i)a|alpha" => -800
           case s if s matches "(?i)b|beta" => -700
           case s if s matches "(?i)c|rc" => -600
           case s if s matches "(?i)final" => 0
           case _ => -1000
         }
         val incremental = Option(version).map(_.toInt).getOrElse(0)
         major + incremental
       }
       case _ => -1000
     }
    }

    override def compare(that: Version): Int = {
      val q1 = qualifier.getOrElse("")
      val q2 = that.qualifier.getOrElse("")
      val c1 = major compare that.major
      val c2 = minor.getOrElse(-1) compare that.minor.getOrElse(-1)
      val c3 = incremental.getOrElse(-1) compare that.incremental.getOrElse(-1)
      val c4 = matchQualifierToVersion(q1) compare matchQualifierToVersion(q2)
      if (c1 != 0) c1 else if (c2 != 0) c2 else if (c3 != 0) c3 else if (c4 != 0) c4 else q1 compare q2
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
          Some(Version(major.toInt, Option(minor).map(_.toInt), Option(incremental).map(_.toInt), Option(qualifier)))
        }
        case _ => None
      }
    }
  }


  object VersionOrdering extends Ordering[String] {
    override def compare(x: String, y: String): Int = {
      val vx = Version.tryParse(x).getOrElse(Version(-1))
      val vy = Version.tryParse(y).getOrElse(Version(-1))
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

  private def detailUrlOf(project: Project) : String = s"${baseUrl}/artifact/${project.group}/${project.name}"

  private def detailUrlOf(artifact: Artifact): String =
    s"${baseUrl}/artifact/${artifact.group}/${artifact.name}/${artifact.version}"

  private def libraryPackageUrlOf(artifact: Artifact): String = {
    val groupPath = artifact.group.replaceAllLiterally(".", "/")
    s"${baseDownloadUrl}/${groupPath}/${artifact.name}/${artifact.version}/${artifact.name}-${artifact.version}.jar"
  }


  private def sourcePackageUrlOf(artifact: Artifact): String = {
    val groupPath = artifact.group.replaceAllLiterally(".", "/")
    s"${baseDownloadUrl}/${groupPath}/${artifact.name}/${artifact.version}/${artifact.name}-${artifact.version}-sources.jar"
  }


  def fetchArtifactsOf(project: Project) : Seq[Artifact] = {
    val projectUrl = detailUrlOf(project)
    val artifacts = fetchFrom(projectUrl, MavenWebPageParser.parseProjectDetailPage)
    artifacts
  }

  def takeLatestVersion(artifacts: Seq[Artifact]): Option[Artifact] = {
    val cleanArtifacts = artifacts.filter(artifact => {
      val version = Version.tryParse(artifact.version)
      version.exists(v => v.major < reasonableMaxMajorVersion)
    })
    if (cleanArtifacts.nonEmpty)
      Some(cleanArtifacts.maxBy(artifact => artifact.version)(VersionOrdering))
    else
      None
  }

  implicit class MavenProject(project: Project) {
    def fetchArtifacts(): Seq[Artifact] = MavenRepository.fetchArtifactsOf(project)
  }

  implicit class MavenArtifact(artifact: Artifact) {
    def libraryPackageUrl: String = MavenRepository.libraryPackageUrlOf(artifact)

    def sourcePackageUrl: String = MavenRepository.sourcePackageUrlOf(artifact)

    def artifactPath: String = s"${artifact.group}/${artifact.name}/${artifact.version}"

    def libraryPackagePath = s"${artifact.artifactPath}/${artifact.name}-${artifact.version}.jar"

    def sourcePackagePath = s"${artifact.artifactPath}/${artifact.name}-${artifact.version}-sources.jar"
  }

  implicit class MavenArtifactSeq(artifacts: Seq[Artifact]) {
    def takeLatestVersion: Option[Artifact] = MavenRepository.takeLatestVersion(artifacts)
  }
}
