package com.cppdo.apibook.ast


import com.cppdo.apibook.APIBook._
import com.cppdo.apibook.db._
import com.typesafe.scalalogging.LazyLogging
import org.eclipse.jdt.core.dom._
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree._
import org.objectweb.asm.util.Textifier
import scala.collection.JavaConverters._
import org.objectweb.asm.{Type=>AsmType}

/**
 * Created by song on 3/11/15.
 */
object AstTreeManager extends LazyLogging {
  def typeDeclarationsOf(cu:  CompilationUnit): Seq[TypeDeclaration] = {
    val classVisitor = new ClassVisitor
    cu.accept(classVisitor)
    classVisitor.types
  }

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

  def calculateMethodUsage(classNode: ClassNode): Set[String] = {
    val classType = AsmType.getObjectType(classNode.name)
    var invocationMethodSet = Set[String]()
    val superClassType = Option(classNode.superName).map(name => AsmType.getObjectType(name))
    methodNodesOf(classNode).foreach(methodNode => {
      methodNode.instructions.iterator().asScala.filter({
        case _: MethodInsnNode => true
        case _ => false
      }).foreach({case methodInsNode: MethodInsnNode => {
        methodInsNode.name
        val ownerType = AsmType.getObjectType(methodInsNode.owner)
        val methodFullName = s"${ownerType.getClassName}.${methodInsNode.name}"
        invocationMethodSet += methodFullName
      }})
    })
    invocationMethodSet
  }

  def calculateConstantParameter(classNode: ClassNode) = {
    val classType = AsmType.getObjectType(classNode.name)
    var invocationMethodSet = Set[String]()
    methodNodesOf(classNode).foreach(methodNode => {
      methodNode.instructions.iterator().asScala.foreach {
        case methodInsNode: MethodInsnNode => {
          methodInsNode.name
          val ownerType = AsmType.getObjectType(methodInsNode.owner)
          val methodFullName = s"${ownerType.getClassName}.${methodInsNode.name}"
          invocationMethodSet += methodFullName
        }
        case ldcInsNode: LdcInsnNode => {
          ldcInsNode.cst match  {
            case str: String => {
              println(str)
            }
            case _ => {} // nothing

          }
        }
        case _ => {}// nothing
      }
    })
  }

  def buildCodeClass(classNode: ClassNode, onlyPublic: Boolean = true) = {
    val classType = AsmType.getObjectType(classNode.name)
    val superClassType = Option(classNode.superName).map(name => AsmType.getObjectType(name))
    val methods = methodNodesOf(classNode).map(methodNode => {
      val methodName = if (methodNode.name == "<init>") classType.getClassName.split(".").last else methodNode.name
      val methodType = AsmType.getMethodType(methodNode.desc)
      val parameterTypes = methodType.getArgumentTypes.map(parameterType => {
        parameterType.getClassName
      })
      val returnType = methodType.getReturnType.getClassName
      CodeMethod(methodName, methodNode.getMethodAccess, methodNode.isStatic, parameterTypes, returnType)
    }).filter(method => {
      !onlyPublic || method.access == "public"
    })
    CodeClass(classType.getClassName, superClassType.map(t => t.getClassName), classNode.getClassAccess, methods)
  }

  def buildFrom(typeDeclaration: TypeDeclaration, artifact: Artifact): Class = {
    val fieldNames = typeDeclaration.getFields.map(field => {
      val fieldType = field.getType.toString
      field.fragments().toArray.map({
        case variableDeclaration: VariableDeclaration => {
          variableDeclaration.getName.toString
        }
      }).mkString(", ")
    })
    Class(typeDeclaration.getName.toString, fieldNames.mkString(", "), artifact.id.get)
  }

  def buildFrom(methodDeclaration: MethodDeclaration, klass: Class): Method = {
    val parameterNames = methodDeclaration.parameters().toArray.map({
      case variableDeclaration: SingleVariableDeclaration => {
        variableDeclaration.getName.toString
      }
    })
    val parameterTypes = methodDeclaration.parameters().toArray.map({
      case variableDeclaration: SingleVariableDeclaration => {
        variableDeclaration.getType.toString
      }
    })
    val returnType = Option(methodDeclaration.getReturnType2).map(_.toString).getOrElse("void")
    val signature = parameterTypes.mkString(", ") + ", " + returnType
    Method(methodDeclaration.getName.toString, signature, parameterNames.mkString(", "), klass.id.get)
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

    def getClassAccess: String = {
      if ((classNode.access & Opcodes.ACC_PUBLIC) != 0) {
        "public"
      }
      else if ((classNode.access & Opcodes.ACC_PRIVATE) != 0) {
        "private"
      } else if ((classNode.access & Opcodes.ACC_PROTECTED) != 0) {
        "protected"
      } else {
        "unknown"
      }
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

    def getMethodAccess: String = {
      if ((methodNode.access & Opcodes.ACC_PUBLIC) != 0) {
        "public"
      }
      else if ((methodNode.access & Opcodes.ACC_PRIVATE) != 0) {
        "private"
      } else if ((methodNode.access & Opcodes.ACC_PROTECTED) != 0) {
        "protected"
      } else {
        "unknown"
      }
    }
    def isPublic: Boolean = {
      (methodNode.access & Opcodes.ACC_PUBLIC) != 0
    }

    def isStatic: Boolean = {
      (methodNode.access & Opcodes.ACC_STATIC) != 0
    }

    def isRegular: Boolean = {
      !(methodNode.name.contains("$") || methodNode.name.contains("<"))
    }
  }

}
