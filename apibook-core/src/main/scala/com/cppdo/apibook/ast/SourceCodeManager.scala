package com.cppdo.apibook.ast

import org.apache.commons.io.FileUtils
import org.eclipse.jdt.core.dom.{AST, ASTParser}

/**
 * Created by song on 3/19/15.
 */
object SourceCodeManager {
  def parseJavaSourceFile(file: String) = {
    val parser = ASTParser.newParser(AST.JLS8)
  }
}
