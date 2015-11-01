package com.cppdo.apibook.index

import com.cppdo.apibook.db.{MethodInfo, CodeMethod, CodeClass}
import com.cppdo.apibook.index.IndexManager.{DocumentType, FieldName}
import com.cppdo.apibook.db.{Field => ClassField, Class, Method, CodeMethod, CodeClass, MethodInfo}
import com.cppdo.apibook.db.Imports._
import org.apache.lucene.document.{TextField, Field, StringField, Document}
import org.apache.lucene.index.{DirectoryReader, IndexWriter}
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.{BooleanQuery, IndexSearcher}
import org.objectweb.asm.tree.ClassNode

/**
 * Created by song on 10/31/15.
 */
class MethodIndexManager(indexDirectory: String) extends IndexManager(indexDirectory) {

  override def buildDocument(codeClass: CodeClass, codeMethod: CodeMethod, methodInfo: Option[MethodInfo]): Document = {
    val document = new Document
    val canonicalNameField = new StringField(FieldName.CanonicalName.toString, codeMethod.canonicalName, Field.Store.YES)
    val methodFullNameField = new StringField(FieldName.MethodFullName.toString, codeMethod.fullName, Field.Store.YES)
    val classFullNameField = new StringField(FieldName.ClassFullName.toString, codeMethod.typeFullName, Field.Store.YES)
    val parameterTypesField = new TextField(FieldName.ParameterTypes.toString, codeMethod.parameterTypes.mkString(" "),
      Field.Store.YES)
    val returnTypeField = new StringField(FieldName.ReturnType.toString, codeMethod.returnType, Field.Store.YES)

    val typeField = new StringField(FieldName.Type.toString, DocumentType.Method.toString, Field.Store.YES)
    document.add(canonicalNameField)
    document.add(methodFullNameField)
    document.add(classFullNameField)
    document.add(parameterTypesField)
    document.add(returnTypeField)
    document.add(typeField)


    methodInfo.foreach(info => {
      val parameterNames = info.parameters.map(_.name)
      val parameterField = new TextField(FieldName.ParameterNames.toString, parameterNames.mkString(" "), Field.Store.YES)
      val commentField = new TextField(FieldName.CommentText.toString, info.commentText, Field.Store.YES)
      val parameterTagsField = new TextField(FieldName.ParameterTags.toString,
        info.parameterTags.map(_.text).mkString(" "), Field.Store.YES)
      document.add(parameterField)
      document.add(commentField)
      document.add(parameterTagsField)
      info.returnTag.foreach(returnTag => {
        val returnTagField = new TextField(FieldName.ReturnTags.toString,
          returnTag.text, Field.Store.YES)
        document.add(returnTagField)
      })
    })
    //document.add(parameterField)
    document
  }
}
