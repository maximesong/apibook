package com.cppdo.apibook.db

/**
 * Created by song on 10/8/15.
 */

case class CodeClass(fullName: String, superFullName: Option[String], access: String, methods: Seq[CodeMethod])

case class CodeMethod(name: String, access: String, isStatic: Boolean, parameterTypes: Seq[String], returnType: String)

case class ClassInvocations()