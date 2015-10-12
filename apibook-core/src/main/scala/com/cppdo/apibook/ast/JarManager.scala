package com.cppdo.apibook.ast

import java.io.InputStream
import java.util.jar.{JarEntry, JarFile}

import com.cppdo.apibook.db.{Class, Method}
import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.io.{IOUtils, FileUtils}
import org.eclipse.jdt.core.dom.{AST, ASTParser, CompilationUnit}
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.{MethodNode, ClassNode}

import scala.collection.JavaConverters._


/**
 * Created by song on 3/4/15.
 */
object JarManager extends LazyLogging {
  def getClassNodes(jarPath: String, flag: Int = 0): Seq[ClassNode] = {
    val jarFile = new JarFile(jarPath)
    val classEntries = jarFile.entries().asScala.filter(_.getName endsWith ".class")
    val classNodes = classEntries.map(entry => {
      val inputStream = jarFile.getInputStream(entry)
      val reader = new ClassReader(inputStream)
      val classNode = new ClassNode()
      reader.accept(classNode, flag)
      //reader.accept(classNode, ClassReader.SKIP_CODE)
      classNode
    }).toSeq
    classNodes
  }

  def getSource(jarPath: String): String = {
    val jarFile = new JarFile(jarPath)
    val sourceEntries = jarFile.entries().asScala.filter(_.getName endsWith ".java")
    sourceEntries.foreach(entry => {
      val inputStream = jarFile.getInputStream(entry)
    })
    ""
  }

  def getDocEntry(jarPath: String, klass: Class): Option[JarEntry] = {
    val entries = getDocEntries(jarPath, klass)
    selectDocEntry(entries, klass)
  }

  def getDocEntry(jarFile: JarFile, klass: Class): Option[JarEntry] = {
    val entries = getDocEntries(jarFile, klass)
    selectDocEntry(entries, klass)
  }

  def getDocInputStream(jarPath: String, klass: Class): Option[InputStream] = {
    val jarFile = new JarFile(jarPath)
    val optionDocEntry = getDocEntry(jarFile, klass)
    optionDocEntry.map(e => {
      jarFile.getInputStream(e)
    })
  }

  def getDocEntries(jarFile: JarFile, klass: Class): Seq[JarEntry] = {
    val docEntries = jarFile.entries().asScala.filter(entry => {
      val name = entry.getName
      name.endsWith(".html") && name.contains(klass.fullName)
    })
    docEntries.toSeq
  }

  def getDocEntries(jarPath: String, klass: Class): Seq[JarEntry] = {
    val jarFile = new JarFile(jarPath)
    getDocEntries(jarFile, klass)
  }

  def selectDocEntry(entries: Seq[JarEntry], klass: Class): Option[JarEntry] = {
    val sorted = entries.sortBy(jarEntry => jarEntry.getName.length)
    sorted.headOption
  }

  def getCompilationUnits(jarPath: String): Seq[CompilationUnit] = {
    val jarFile = new JarFile(jarPath)
    val sourceEntries = jarFile.entries().asScala.filter(_.getName endsWith ".java")
    val parser = ASTParser.newParser(AST.JLS8)
    val compilationUnits = sourceEntries.map(entry => {
      val inputStream = jarFile.getInputStream(entry)
      val source = IOUtils.toCharArray(inputStream)
      parser.setSource(source)
      val cu = parser.createAST(null).asInstanceOf[CompilationUnit]
      if (cu == null) {
        logger.info(entry.getName)
      }
      cu
    })
    compilationUnits.toSeq
  }
}
