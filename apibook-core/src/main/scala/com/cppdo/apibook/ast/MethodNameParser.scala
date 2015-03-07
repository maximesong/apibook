package com.cppdo.apibook.ast


import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes

/**
 * Created by song on 2015/3/4.
 */
class MethodNameParser extends ClassVisitor(Opcodes.ASM5) {
  override def visitMethod(access: Int, name: String,
    desc: String, signature: String, exceptions: Array[String]) = {
    println(name)
    null
  }
}
