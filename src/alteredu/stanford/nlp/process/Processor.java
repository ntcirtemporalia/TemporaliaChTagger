package alteredu.stanford.nlp.process;

import alteredu.stanford.nlp.ling.Document;

/**
 * Top-level interface for transforming Documents.
 *
 * @author Sepandar Kamvar (sdkamvar@stanford.edu)
 * @see #processDocument
 */
public interface Processor {

  /**
   * Converts a Document to a different Document, by transforming
   * or filtering the original Document. The general contract of this method
   * is to not modify the <code>in</code> Document in any way, and to
   * preserve the metadata of the <code>in</code> Document in the
   * returned Document.
   *
   * @see FunctionProcessor
   */
  public Document processDocument(Document in);

}
