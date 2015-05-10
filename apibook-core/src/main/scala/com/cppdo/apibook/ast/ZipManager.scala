package com.cppdo.apibook.ast

import java.util.jar.JarFile
import java.util.zip.ZipFile

import com.cppdo.apibook.ast.JarManager._
import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.io.IOUtils
import org.eclipse.jdt.core.dom.{AST, ASTParser, CompilationUnit}
import scala.collection.JavaConverters._

/**
 * Created by song on 5/10/15.
 */
object ZipManager extends LazyLogging {
  def getCompilationUnits(zipPath: String): Seq[CompilationUnit] = {
    val zipFile = new ZipFile(zipPath)
    val sourceEntries = zipFile.entries().asScala.filter(_.getName endsWith ".java")
    val parser = ASTParser.newParser(AST.JLS8)
    val compilationUnits = sourceEntries.map(entry => {
      val inputStream = zipFile.getInputStream(entry)
      val source = IOUtils.toCharArray(inputStream)
      parser.setSource(source)
      val cu = parser.createAST(null).asInstanceOf[CompilationUnit]
      if (cu == null) {
        logger.info(entry.getName)
      }
      cu
    })
    compilationUnits.toSeq
  }
}
