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
  val methodCollection = db("methods")
  val methodUsageCollection = db("method_usage")
  val classUsageCollection = db("class_usage")
  val methodInfoCollection = db("method_info")
  val classInfoCollection = db("class_info")

  classUsageCollection.createIndex(MongoDBObject(
    "typeFullName" -> 1,
    "usedByTypeFullName" -> 1
  ))
  methodUsageCollection.createIndex(MongoDBObject(
    "methodFullName" -> 1,
    "usedByTypeFullName" -> 1
  ))
  classCollection.createIndex(MongoDBObject(
    "fullName" -> 1
  ))
  methodCollection.createIndex(MongoDBObject(
    "canonicalName" -> 1
  ))
  methodCollection.createIndex(MongoDBObject(
    "parameterTypes" -> 1
  ))
  methodCollection.createIndex(MongoDBObject(
    "returnType" -> 1
  ))

  methodInfoCollection.createIndex(MongoDBObject(
    "canonicalName" -> 1
  ))

  classInfoCollection.createIndex(MongoDBObject(
    "fullName" -> 1
  ))

  def upsertClass(codeClass: CodeClass): Unit = {
    val query = MongoDBObject(
      "fullName" -> codeClass.fullName
    )
    val update = grater[CodeClass].asDBObject(codeClass)
    classCollection.update(query, update, upsert=true)
  }

  def upsertMethod(codeMethod: CodeMethod): Unit = {
    val query = MongoDBObject(
      "canonicalName" -> codeMethod.canonicalName
    )
    val update = grater[CodeMethod].asDBObject(codeMethod)
    methodCollection.update(query, update, upsert=true)
  }

  def getCodeClass(fullName: String): Option[CodeClass] = {
    classCollection.find(MongoDBObject(
      "fullName" -> fullName
    )).toSeq.headOption.map(obj => {
      grater[CodeClass].asObject(obj)
    })
  }

  def findClassOfName(name: String): Seq[CodeClass] = {
    classCollection.find(MongoDBObject(
      "fullName" -> MongoDBObject(
        "$regex" -> s"[.]${name}$$",
        "$options" -> "i"
      )
    )).toSeq.map(obj => {
      grater[CodeClass].asObject(obj)
    })
  }

  def getCodeClasses(): Seq[CodeClass] = {
    val codeClasses = classCollection.find().map(obj => {
      grater[CodeClass].asObject(obj)
    })
    codeClasses.toSeq
  }

  def getMethodInfo(fullName: String): Option[MethodInfo] = {
    val query = MongoDBObject(
      "fullName" -> fullName
    )
    methodInfoCollection.find(query).toSeq.headOption.map(obj => {
      grater[MethodInfo].asObject(obj)
    })
  }

  def findMethodsAccept(fullTypeName: String): Seq[CodeMethod] = {
    methodCollection.find(MongoDBObject(
      "parameterTypes" -> fullTypeName
    )).toSeq.map(obj => {
      grater[CodeMethod].asObject(obj)
    })
  }

  def findMethodsReturn(fullTypeName: String): Seq[CodeMethod] = {
    methodCollection.find(MongoDBObject(
      "returnType" -> fullTypeName
    )).toSeq.map(obj => {
      grater[CodeMethod].asObject(obj)
    })
  }

  def findMethodsRelated(fullTypeName: String): Seq[CodeMethod] = {
    methodCollection.find(MongoDBObject(
      "$or" -> MongoDBList(
        MongoDBObject(
          "parameterTypes" -> fullTypeName
        ),
        MongoDBObject(
          "returnType" -> fullTypeName
        )
      )
    )).toSeq.map(obj => {
      grater[CodeMethod].asObject(obj)
    })
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

  def findClassRelated(typeFullName: String): Seq[CodeClass] = {
    val classesReturn = findClassReturn(typeFullName)
    val classesAccept = findClassAccept(typeFullName)
    classesReturn ++ classesAccept
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

  def upsertMethodUsage(methodFullName: String, usedByTypeFullName: String) = {
    val query = MongoDBObject(
      "methodFullName" -> methodFullName,
      "usedByTypeFullName" -> usedByTypeFullName
    )
    val update = MongoDBObject(
      "methodFullName" -> methodFullName,
      "usedByTypeFullName" -> usedByTypeFullName
    )
    methodUsageCollection.update(query, update, upsert=true)
  }

  def getMethodUsage(methodFullName: String): Seq[String] = {
    methodUsageCollection.find(MongoDBObject(
      "methodFullName" -> methodFullName
    )).toSeq.map(obj => {
      obj.as[String]("usedByTypeFullName")
    })
  }

  def getClassUsage(typeFullName: String): Seq[String] = {
    classUsageCollection.find(MongoDBObject(
      "typeFullName" -> typeFullName
    )).toSeq.map(obj => {
      obj.as[String]("usedByTypeFullName")
    })
  }

  def upsertClassUsage(typeFullName: String, usedByTypeFullName: String) = {
    val query = MongoDBObject(
      "typeFullName" -> typeFullName,
      "usedByTypeFullName" -> usedByTypeFullName
    )
    val update = MongoDBObject(
      "typeFullName" -> typeFullName,
      "usedByTypeFullName" -> usedByTypeFullName
    )
    classUsageCollection.update(query, update, upsert=true)
  }


  def upsertMethodInfo(methodInfo: MethodInfo) = {
    val query = MongoDBObject(
      "fullName" -> methodInfo.fullName
    )
    val update = grater[MethodInfo].asDBObject(methodInfo)
    methodInfoCollection.update(query, update, upsert=true)
  }

  def upsertClassInfo(classInfo: ClassInfo) = {
    val query = MongoDBObject(
      "fullName" -> classInfo.fullName
    )
    val update = grater[ClassInfo].asDBObject(classInfo)
    classInfoCollection.update(query, update, upsert=true)
  }

  def close() = {
    mongoClient.close()
  }
}