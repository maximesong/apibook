package com.cppdo.apibook.db

/**
 * Created by song on 10/8/15.
 */

case class CodeClass(fullName: String, superFullName: Option[String], access: String, methods: Seq[CodeMethod])


case class CodeMethod(canonicalName: String, fullName: String, typeFullName: String,
                      parameterTypes: Seq[String], returnType: String, access: String, isStatic: Boolean)

object CodeMethod {
  def buildCanonicalName(methodFullName: String, paramterTypes: Seq[String], returnType: String) = {
    val canonicalName = s"${methodFullName}(${paramterTypes.mkString(", ")}): ${returnType}"
    if (canonicalName.length > 1000) { // e.g. org.codehaus.groovy.runtime.ArrayUtil.createArray()
      s"${methodFullName}(): ${returnType}"
    } else {
      canonicalName
    }
  }
}

case class MethodInvocations(methodFullName: String, invokedBy: Map[String, Boolean])

case class MethodInfo(canonicalName: String, fullName: String,
                        parameters: Seq[Parameter], returnType: String, commentText: String,
                        parameterTags: Seq[Tag], returnTag: Option[Tag])

case class Parameter(name: String, typeName: String)
case class Tag(name: String, text: String)

case class Field(name: String, typeName: String)

case class ClassInfo(fullName: String, fields:Seq[Field], commentText: String)

object Imports {
  implicit class RichCodeClass(codeClass: CodeClass) {
    def name: String = codeClass.fullName.split("[.]").last
  }

  implicit class RichCodeMethod(codeMethod: CodeMethod) {
    def name: String = codeMethod.fullName.split("[.]").last

    def typeName: String = codeMethod.typeFullName.split("[.]").last
  }
}