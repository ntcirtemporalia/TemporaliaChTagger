package alteredu.stanford.nlp.ie.crf;

import alteredu.stanford.nlp.ling.Datum;

import java.io.Serializable;
import java.util.List;


/**
 * @author Jenny Finkel
 */

public class CRFDatum implements Serializable {
  /**
   * features for this Datum
   */
  private final List features;
  private Object label = null;

  /*
   * Constructs a new BasicDatum with the given features and label.
   */
  public CRFDatum(List features, Object label) {
    this(features);
    setLabel(label);
  }

  /**
   * Constructs a new BasicDatum with the given features and no labels.
   */
  public CRFDatum(List features) {
    this.features = features;
  }

  /**
   * Returns the collection that this BasicDatum was constructed with.
   */
  public List asFeatures() {
    return (features);
  }

  /**
   * Returns the label for this Datum, or null if none have been set.
   */

  public Object label() {
    return label;
  }

  /**
   * Removes all currently assigned Labels for this Datum then adds the
   * given Label.
   * Calling <tt>setLabel(null)</tt> effectively clears all labels.
   */
  public void setLabel(Object label) {
    this.label = label;
  }

  /**
   * Returns a String representation of this BasicDatum (lists features and labels).
   */
  public String toString() {
    StringBuffer sb = new StringBuffer("CRFDatum[\n");
    sb.append("     label=").append(label).append("\n");
    for (int i = 0; i < features.size(); i++) {
      sb.append("     features("+i+"):"+features.get(i)+"\n");
    }
    sb.append("]");
    return sb.toString();
  }


  /**
   * Returns whether the given Datum contains the same features as this Datum.
   * Doesn't check the labels, should we change this?
   */
  public boolean equals(Object o) {
    if (!(o instanceof Datum)) {
      return (false);
    }

    Datum d = (Datum) o;
    return (features.equals(d.asFeatures()));
  }
}

