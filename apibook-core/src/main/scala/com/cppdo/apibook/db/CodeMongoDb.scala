package com.cppdo.apibook.db

import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import com.typesafe.scalalogging.LazyLogging
import com.novus.salat._
import com.novus.salat.global._
/**
 * Created by song on 10/8/15.
 */
class CodeMongoDb(host: String, dbName: String) extends LazyLogging {
  val mongoClient = MongoClient(host)
  val db = mongoClient(dbName)
  val classCollection = db("classes")

  def upsertClass(codeClass: CodeClass): Unit = {
    val query = MongoDBObject(
      "fullName" -> codeClass.fullName
    )
    val update = grater[CodeClass].asDBObject(codeClass)
    classCollection.update(query, update, upsert=true)
  }

  def findClassAccept(typeName: String): Seq[CodeClass] = {
    val query = "methods" $elemMatch MongoDBObject(
      "parameterTypes" -> typeName
    )
    classCollection.find(query).toSeq.map(obj => {
      grater[CodeClass].asObject(obj)
    })
  }

  def findClassReturn(typeName: String): Seq[CodeClass] = {
    val query = "methods" $elemMatch MongoDBObject(
      "returnType" -> typeName
    )
    classCollection.find(query).toSeq.map(obj => {
      grater[CodeClass].asObject(obj)
    })
  }

  def findMethodConvert(fromTypeName: String, toTypeName: String): Seq[CodeClass] = {
    val query = "methods" $elemMatch MongoDBObject(
      "parameterTypes" -> fromTypeName,
      "returnType" -> toTypeName
    )
    val results = classCollection.find(query).toSeq.map(obj => {
      grater[CodeClass].asObject(obj)
    })
    results
  }
}