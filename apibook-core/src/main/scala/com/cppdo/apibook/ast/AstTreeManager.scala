package com.cppdo.apibook.ast

import com.cppdo.apibook.db.{Artifact, Class, Method}
import org.objectweb.asm.tree.{MethodNode, ClassNode}
import scala.collection.JavaConverters._

/**
 * Created by song on 3/11/15.
 */
object AstTreeManager {
  def methodNodesOf(classNode: ClassNode): Seq[MethodNode] = {
    classNode.methods.asScala.map { case methodNode: MethodNode =>
      methodNode
    }.toSeq
  }

  def buildFrom(classNode: ClassNode, artifact: Artifact): Class = {
    Class(classNode.name, artifact.id.get)
  }

  def buildFrom(methodNode: MethodNode, klass: Class): Method = {
    Method(methodNode.name, methodNode.signature, klass.id.get)
  }

}
