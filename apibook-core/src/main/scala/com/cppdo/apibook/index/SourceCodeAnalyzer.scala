package com.cppdo.apibook.index

import java.io.Reader

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents
import org.apache.lucene.analysis.{standard, TokenStream}
import org.apache.lucene.analysis.core.{StopFilter, LowerCaseFilter, StopAnalyzer}
import org.apache.lucene.analysis.standard.{StandardFilter, StandardTokenizer, StandardAnalyzer}
import org.apache.lucene.analysis.util.StopwordAnalyzerBase

/**
 * Created by song on 5/10/15.
 */
class SourceCodeAnalyzer extends StopwordAnalyzerBase {
  val maxTokenLength = 255

  val STOP_WORDS_SET = StopAnalyzer.ENGLISH_STOP_WORDS_SET

  override def createComponents(fieldName: String, reader: Reader): TokenStreamComponents = {
    val src = new StandardTokenizer(reader)
    src.setMaxTokenLength(maxTokenLength)
    var tok: TokenStream = new StandardFilter(src)
    tok = new LowerCaseFilter(tok)
    tok = new StopFilter(tok, stopwords)
    new TokenStreamComponents(src, tok) {
      override def setReader(reader: Reader) {
        src.setMaxTokenLength(maxTokenLength)
        super.setReader(reader)
      }
    }
  }
}
