package com.cppdo.apibook.db

import org.objectweb.asm.{Type=>AsmType}

/**
 * Created by song on 10/8/15.
 */

case class CodeClass(fullName: String, superFullName: Option[String], access: String, methods: Seq[CodeMethod])


case class CodeMethod(canonicalName: String, fullName: String, typeFullName: String,
                      parameterTypes: Seq[String], returnType: String, access: String, isStatic: Boolean)

object CodeMethod {
  def buildCanonicalName(methodFullName: String, paramterTypes: Seq[String], returnType: String): String = {
    val canonicalName = s"${returnType} ${methodFullName}(${paramterTypes.mkString(", ")})"
    if (canonicalName.length > 1000) { // e.g. org.codehaus.groovy.runtime.ArrayUtil.createArray()
      s"${returnType} ${methodFullName}()"
    } else {
      canonicalName
    }
  }

  def buildFullName(classType: AsmType, methodName: String) = {
    if (methodName == "<init>") {
      val typeName = classType.getClassName.split("[.]").last
      s"${classType.getClassName}.${typeName}"
    } else {
      s"${classType.getClassName}.${methodName}"
    }
  }

  def buildCanonicalName(methodFullName: String, methodType: AsmType): String = {
    val returnType = methodType.getReturnType.getClassName
    val parameterTypes = methodType.getArgumentTypes.map(_.getClassName)
    buildCanonicalName(methodFullName, parameterTypes, returnType)
  }
}

case class MethodInvocation(canonicalName: String, methodFullName: String, typeFullName: String,
                            invokedByCanonicalName: String, invokedByMethod: String, invokedByType: String)

case class MethodInfo(canonicalName: String, fullName: String,
                        parameters: Seq[Parameter], returnType: String, commentText: String,
                        parameterTags: Seq[Tag], returnTag: Option[Tag])

case class Parameter(name: String, typeName: String)
case class Tag(name: String, text: String)

case class Field(name: String, typeName: String)

case class ClassInfo(fullName: String, fields:Seq[Field], commentText: String)

case class ClassArtifacts(fullName: String, sourceCodeFilePath: Option[String],
                         byteCodeJarPath: Option[String], sourceCodeJarPath: Option[String], docJarPath: Option[String])

object ClassArtifacts {
  def byteCodeJarPath = "byteCodeJarPath"
  def sourceCodeFilePath = "sourceCodeFilePath"
}
object Imports {
  implicit class RichCodeClass(codeClass: CodeClass) {
    def name: String = codeClass.fullName.split("[.]").last
  }

  implicit class RichCodeMethod(codeMethod: CodeMethod) {
    def name: String = codeMethod.fullName.split("[.]").last

    def typeName: String = codeMethod.typeFullName.split("[.]").last
  }
}