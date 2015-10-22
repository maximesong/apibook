package com.cppdo.apibook.db

import com.cppdo.apibook.search.MethodScore
import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import com.typesafe.scalalogging.LazyLogging
import com.novus.salat._
import com.novus.salat.global._
import play.api.libs.json.{JsObject, JsValue}
import play.libs.Json

/**
 * Created by song on 10/8/15.
 */
class CodeMongoDb(host: String, dbName: String, classLoader: Option[ClassLoader] = None) extends LazyLogging {
  classLoader.foreach(classLoader => {
    ctx.clearAllGraters()
    ctx.registerClassLoader(classOf[CodeClass].getClassLoader)
  })
  val mongoClient = MongoClient(host)
  val db = mongoClient(dbName)
  val classCollection = db("classes")
  val methodCollection = db("methods")
  val methodUsageCollection = db("method_usage")
  val classUsageCollection = db("class_usage")
  val methodInfoCollection = db("method_info")
  val classInfoCollection = db("class_info")

  val classArtifactsCollection = db("class_artifacts")

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
  methodCollection.createIndex(MongoDBObject(
    "typeFullName" -> 1
  ))

  methodInfoCollection.createIndex(MongoDBObject(
    "canonicalName" -> 1
  ))

  classInfoCollection.createIndex(MongoDBObject(
    "fullName" -> 1
  ))

  classArtifactsCollection.createIndex(MongoDBObject(
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

  def toJson(methodScore: MethodScore): String = {
    grater[MethodScore].toPrettyJSON(methodScore)
  }

  def findClassOfName(name: String): Seq[CodeClass] = {
    var regex = if (name.contains(".")) s"${name}$$" else s"[.]${name}$$"
    classCollection.find(MongoDBObject(
      "fullName" -> MongoDBObject(
        "$regex" -> regex
        //"$options" -> "i"
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

  def getCodeMethods(canonicalNames: Seq[String]): Seq[CodeMethod] = {
    methodCollection.find("canonicalName" $in canonicalNames).toSeq.map(obj => {
      grater[CodeMethod].asObject(obj)
    })
  }

  def getMethodInfo(fullName: String): Option[MethodInfo] = {
    val query = MongoDBObject(
      "canonicalName" -> fullName
    )
    methodInfoCollection.find(query).toSeq.headOption.map(obj => {
      grater[MethodInfo].asObject(obj)
    })
  }

  def getClassArtifacts(fullName: String): Option[ClassArtifacts] = {
    val query = MongoDBObject(
      "fullName" -> fullName
    )
    classArtifactsCollection.find(query).toSeq.headOption.map(obj => {
      grater[ClassArtifacts].asObject(obj)
    })
  }

  def upsertClassArtifact(fullName: String, artifactType: String, path: String) = {
    val query = MongoDBObject(
      "fullName" -> fullName
    )
    val update = $set(artifactType -> path)
     /*
      MongoDBObject(
      "fullName" -> fullName,
      artifactType -> path
    )*/
    classArtifactsCollection.update(query, update, upsert = true)
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
        ),
        MongoDBObject(
          "typeFullName" -> fullTypeName
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
      "canonicalName" -> methodInfo.canonicalName
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