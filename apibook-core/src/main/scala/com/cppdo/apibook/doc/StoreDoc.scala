package com.cppdo.apibook.doc

import com.cppdo.apibook.db._
import com.sun.javadoc.{RootDoc, Doclet}

/**
 * Created by song on 10/10/15.
 */
object StoreDoc extends Doclet {
  def start(rootDoc: RootDoc): Boolean = {
    val db = new CodeMongoDb("localhost", "apibook")
    rootDoc.classes().foreach(classDoc => {
      val fields = classDoc.fields().map(field => {
        Field(field.name(), field.`type`().qualifiedTypeName())
      })
      val classInfo = ClassInfo(classDoc.qualifiedName(), fields, classDoc.commentText())
      db.upsertClassInfo(classInfo)
      val methodDetails = classDoc.methods().map(methodDoc => {
        println(s"${classDoc.name}.${methodDoc.name()}")
        val parameters = methodDoc.parameters().map(parameter => {
          Parameter(parameter.name(), parameter.`type`().qualifiedTypeName())
        })
        MethodInfo(methodDoc.qualifiedName(), parameters, methodDoc.returnType().qualifiedTypeName(),
          methodDoc.commentText())
      })
      methodDetails.foreach(methodDetail => {
        db.upsertMethodDetail(methodDetail)
      })
    })
    true
  }
}
