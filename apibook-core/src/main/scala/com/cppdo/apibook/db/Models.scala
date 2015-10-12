package com.cppdo.apibook.db

/**
 * Created by song on 10/8/15.
 */

case class CodeClass(fullName: String, superFullName: Option[String], access: String, methods: Seq[CodeMethod])

case class CodeMethod(name: String, access: String, isStatic: Boolean, parameterTypes: Seq[String], returnType: String)

case class MethodInvocations(methodFullName: String, invokedBy: Map[String, Boolean])

case class MethodInfo(fullName: String, parameters: Seq[Parameter], returnType: String, commentText: String)

case class Parameter(name: String, typeName: String)
case class Field(name: String, typeName: String)

case class ClassInfo(fullName: String, fields:Seq[Field], commentText: String)