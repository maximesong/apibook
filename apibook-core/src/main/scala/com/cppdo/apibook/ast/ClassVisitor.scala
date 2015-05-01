package com.cppdo.apibook.ast

import org.eclipse.jdt.core.dom.{TypeDeclaration, ASTVisitor}

import scala.collection.immutable.List

/**
 * Created by song on 5/1/15.
 */
class ClassVisitor extends ASTVisitor {
  var types = List[TypeDeclaration]()

  override def visit(node: TypeDeclaration): Boolean = {
    types = node :: types
    false
  }
}
