package alteredu.stanford.nlp.process;

import java.io.Serializable;

/**
 * An interface for classes that act as a function transforming one object
 * to another.  This interface is widely used in the process package for
 * various classes that do linguistic transformations.
 *
 * @author Dan Klein
 */
public interface Function <T1,T2> extends Serializable {

  /**
   * Converts a T1 to a different T2.  For example, a Parser
   * will convert a Sentence to a Tree.  A Tagger will convert a Sentence
   * to a TaggedSentence.
   */
  public T2 apply(T1 in);

}
