package alteredu.stanford.nlp.sequences;

import alteredu.stanford.nlp.ling.FeatureLabel;
import alteredu.stanford.nlp.objectbank.IteratorFromReaderFactory;

import java.util.List;
import java.io.PrintWriter;
import java.io.Serializable;

/**
 * This interface is used for reading data and writing
 * output into and out of {@link SequenceClassifier}s.
 * If you subclass this interface, all of the other
 * mechanisms necessary for getting your data into a
 * {@link SequenceClassifier} will be taken care of
 * for you.  Subclasses <B>MUST</B> have an empty constructor so
 * that they can be instantiated by reflection, and
 * there is a promise that the init method will
 * be called immediately after construction.
 * 
 * @author Jenny Finkel
 */

public interface  DocumentReaderAndWriter extends IteratorFromReaderFactory<List<FeatureLabel>>, Serializable {
  /**
   * Will be called immediately after construction.  Needed
   * because of reflection.
   */
  public void init(SeqClassifierFlags flags) ;
  
  /**
   * This method prints the output of the classifier to a
   * {@link PrintWriter}.
   */
  public void printAnswers(List<FeatureLabel> doc, PrintWriter out) ;
  
}
