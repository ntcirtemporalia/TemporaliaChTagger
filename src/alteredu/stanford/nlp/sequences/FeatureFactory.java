package alteredu.stanford.nlp.sequences;

import alteredu.stanford.nlp.util.PaddedList;
import alteredu.stanford.nlp.util.Index;
import alteredu.stanford.nlp.ling.FeatureLabel;

//import edu.stanford.nlp.ie.SeqClassifierFlags;

import java.util.*;
import java.io.Serializable;

/**
 * This is the abstract class that all feature factories must
 * subclass.  It also defines most of the basic {@link Clique}s
 * that you would want to make features over.  It contains a
 * convenient method, getCliques(maxLeft, maxRight) which will give
 * you all the cliques within the specified limits.
 *
 * @author Jenny Finkel
 */
public abstract class FeatureFactory implements Serializable {

  private static final long serialVersionUID = 7249250071983091694L;

  protected SeqClassifierFlags flags;

  public FeatureFactory() {}

  public void init (SeqClassifierFlags flags) {
    this.flags = flags;
  }

  public static final Clique cliqueC = Clique.valueOf(new int[] {0});
  public static final Clique cliqueCpC = Clique.valueOf(new int[] {-1, 0});
  public static final Clique cliqueCp2C = Clique.valueOf(new int[] {-2, 0});
  public static final Clique cliqueCp3C = Clique.valueOf(new int[] {-3, 0});
  public static final Clique cliqueCp4C = Clique.valueOf(new int[] {-4, 0});
  public static final Clique cliqueCp5C = Clique.valueOf(new int[] {-5, 0});
  public static final Clique cliqueCpCp2C = Clique.valueOf(new int[] {-2, -1, 0});
  public static final Clique cliqueCpCp2Cp3C = Clique.valueOf(new int[] {-3, -2, -1, 0});
  public static final Clique cliqueCpCp2Cp3Cp4C = Clique.valueOf(new int[] {-4, -3, -2, -1, 0});
  public static final Clique cliqueCpCp2Cp3Cp4Cp5C = Clique.valueOf(new int[] {-5, -4, -3, -2, -1, 0});
  public static final Clique cliqueCnC = Clique.valueOf(new int[] {0, 1});
  public static final Clique cliqueCpCnC = Clique.valueOf(new int[] {-1, 0, 1});

  public static final List<Clique> knownCliques = Arrays.asList(new Clique[]{cliqueC, cliqueCpC, cliqueCp2C, cliqueCp3C, cliqueCp4C, cliqueCp5C, cliqueCpCp2C, cliqueCpCp2Cp3C, cliqueCpCp2Cp3Cp4C, cliqueCpCp2Cp3Cp4Cp5C, cliqueCnC, cliqueCpCnC});

  public Index<Clique> getCliques() {
    return getCliques(flags.maxLeft, flags.maxRight);
  }

  public static Index<Clique> getCliques(int maxLeft, int maxRight) {
    Index<Clique> cliques = new Index<Clique>();
    for (Clique c : knownCliques) {
      if (-c.maxLeft() <= maxLeft && c.maxRight() <= maxRight) {
        cliques.add(c);
      }
    }
    return cliques;
  }

  /**
   * This method returns a {@link Collection} of the features
   * calculated for the word at the specified position in info (the list of
   * words) for the specified {@link Clique}.
   * It should return the actual features, <b>NOT</B> wrapped in a
   * {@link Features} object, as the wrapping
   * will be done automatically.
   * Because it takes a {@link PaddedList} you don't
   * need to worry about indices which are outside of the list.
   */
  public abstract Collection getCliqueFeatures(PaddedList<FeatureLabel> info, int position, Clique clique) ;

}
