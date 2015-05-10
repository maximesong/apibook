package com.cppdo.apibook.ast

import org.eclipse.jdt.core.dom.{ASTVisitor, ImportDeclaration, TypeDeclaration}

import scala.collection.immutable.List

/**
 * Created by song on 5/1/15.
 */
class ImportVisitor extends ASTVisitor {
  var imports = List[ImportDeclaration]()

  override def visit(node: ImportDeclaration): Boolean = {
    imports = node :: imports
    false
  }
}
