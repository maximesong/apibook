package com.cppdo.apibook.ast

import java.util.jar.JarFile

import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode

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
}
