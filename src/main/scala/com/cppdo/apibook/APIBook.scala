package com.cppdo.apibook

import com.cppdo.apibook.repository.MavenRepository

/**
 * Created by song on 1/17/15.
 */
object APIBook extends App {
  println("Hi")
  val artifacts =  MavenRepository.getTopArtifacts()
  artifacts.foreach(println)
}
