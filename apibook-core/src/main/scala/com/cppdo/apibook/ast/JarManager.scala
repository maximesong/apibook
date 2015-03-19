package com.cppdo.apibook.ast

import java.util.jar.JarFile

import com.cppdo.apibook.db.Method
import org.apache.commons.io.{IOUtils, FileUtils}
import org.eclipse.jdt.core.dom.{AST, ASTParser, CompilationUnit}
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.{MethodNode, ClassNode}

import scala.collection.JavaConverters._


/**
 * Created by song on 3/4/15.
 */
object JarManager {
  def getClassNodes(jarPath: String): Seq[ClassNode] = {
    val jarFile = new JarFile(jarPath)
    val classEntries = jarFile.entries().asScala.filter(_.getName endsWith ".class")
    val classNodes = classEntries.map(entry => {
      val inputStream = jarFile.getInputStream(entry)
      val reader = new ClassReader(inputStream)
      val classNode = new ClassNode()
      reader.accept(classNode, ClassReader.SKIP_CODE)
      classNode
    }).toSeq
    classNodes
  }

  def getCompilationUnits(jarPath: String): Seq[CompilationUnit] = {
    val jarFile = new JarFile(jarPath)
    val sourceEntries = jarFile.entries().asScala.filter(_.getName endsWith ".java")
    val parser = ASTParser.newParser(AST.JLS8)
    val compilationUnits = sourceEntries.map(entry => {
      val inputStream = jarFile.getInputStream(entry)
      val source = IOUtils.toCharArray(inputStream)
      parser.setSource(source)
      parser.createAST(null).asInstanceOf[CompilationUnit]
    })
    compilationUnits.toSeq
  }
}
