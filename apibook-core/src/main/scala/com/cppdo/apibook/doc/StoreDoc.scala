package com.cppdo.apibook.doc

import com.sun.javadoc.{RootDoc, Doclet}

/**
 * Created by song on 10/10/15.
 */
object StoreDoc extends Doclet {
  def start(rootDoc: RootDoc): Boolean = {
    println("true")
    true
  }
}
