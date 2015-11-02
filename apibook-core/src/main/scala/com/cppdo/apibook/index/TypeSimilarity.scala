package com.cppdo.apibook.index

import org.apache.lucene.search.similarities.DefaultSimilarity

/**
 * Created by song on 10/31/15.
 */
class MethodTypesSimilarity extends DefaultSimilarity {
  override def coord(overlap: Int, maxOverlap: Int): Float = {
    overlap.toFloat
  }

  /*
  @Override
  public float lengthNorm(FieldInvertState state) {
    final int numTerms;
    if (discountOverlaps)
      numTerms = state.getLength() - state.getNumOverlap();
    else
      numTerms = state.getLength();
    return state.getBoost() * ((float) (1.0 / Math.sqrt(numTerms)));
  }
  */

  override def queryNorm(sumOfSquaredWeights: Float): Float = {
    return 1
  }

  override def tf(freq: Float): Float = {
    1
  }



  override def idf(docFreq: Long, numDocs: Long): Float = {
    1
  }

  override def toString(): String = {
    "MethodTypesSimilarity"
  }
}
