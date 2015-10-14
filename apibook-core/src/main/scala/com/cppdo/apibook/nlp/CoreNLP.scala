package com.cppdo.apibook.nlp

import java.util.Properties
import scala.collection.JavaConverters._

import edu.stanford.nlp.dcoref.CoNLL2011DocumentReader.NamedEntityAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.{PartOfSpeechAnnotation, TextAnnotation, TokensAnnotation, SentencesAnnotation}
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation

/**
 * Created by song on 10/13/15.
 */
object CoreNLP {
  val properties = new Properties()
  //properties.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref")
  properties.setProperty("annotators", "tokenize, ssplit, pos, lemma")
  val pipline = new StanfordCoreNLP(properties)

  def getPOSMap(text: String): Map[String, String] = {
    val document = new Annotation(text)
    var posMap = Map[String, String]()
    pipline.annotate(document)
    val sentences = document.get(classOf[SentencesAnnotation])
    sentences.asScala.foreach(sentence => {
      val tokens = sentence.get(classOf[TokensAnnotation])
      tokens.asScala.foreach(token => {
        val word = token.get(classOf[TextAnnotation])
        val pos = token.get(classOf[PartOfSpeechAnnotation])
        posMap += word -> pos

        //val ne = token.get(classOf[NamedEntityAnnotation])
        //println(word)
        //println(pos)
        //println(ne)
      })
      //val tree = sentence.get(classOf[TreeAnnotation])
      //tree.indentedListPrint()
      //val dependencies = sentence.get(classOf[CollapsedCCProcessedDependenciesAnnotation])
      //dependencies.prettyPrint()
    })
    posMap
  }
}
