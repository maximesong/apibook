package com.cppdo.apibook.ast


import com.cppdo.apibook.db.{Artifact, Class, Method}
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.{FieldNode, ParameterNode, MethodNode, ClassNode}
import org.objectweb.asm.util.Textifier
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
    Class(classNode.name, classNode.textifiedFieldNames, artifact.id.get)
  }

  def buildFrom(methodNode: MethodNode, klass: Class): Method = {
    Method(methodNode.name, methodNode.textifiedSignature, methodNode.textifiedParameters, klass.id.get)
  }

  def getFieldNames(classNode: ClassNode): String = {
    Option(classNode.fields).map(_.asScala.map({ case field: FieldNode =>
        field.name
      }).mkString(",")).getOrElse("")
  }

  def getTextifiedSignature(methodNode: MethodNode): String = {
    Option(methodNode.signature).map(signature => signature).getOrElse("")
  }

  def getTextifiedParameters(methodNode: MethodNode): String = {
    Option(methodNode.parameters).map(_.asScala.map({ case parameterNode: ParameterNode =>
      parameterNode.name
    }).mkString(",")).getOrElse("")
  }

  implicit class RichClassNode(classNode: ClassNode) {
    def textifiedFieldNames: String = {
      getFieldNames(classNode)
    }

    def isPublic: Boolean = {
      (classNode.access & Opcodes.ACC_PUBLIC) != 0
    }

    def isRegular: Boolean = {
      !classNode.name.contains("$")
    }
  }

  implicit class RichMethodNode(methodNode: MethodNode) {
    def textifiedSignature: String = {
      getTextifiedSignature(methodNode)
    }

    def textifiedParameters: String = {
      getTextifiedParameters(methodNode)
    }

    def isPublic: Boolean = {
      (methodNode.access & Opcodes.ACC_PUBLIC) != 0
    }
  }

}
