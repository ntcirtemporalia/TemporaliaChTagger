package alteredu.stanford.nlp.ie.crf;

import alteredu.stanford.nlp.util.Index;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Jenny Finkel
 */

public class CRFLabel implements Serializable {

  private int[] label;
  int hashCode = -1;

  public static int maxNumClasses = 10;

  public CRFLabel(int[] label) {
    this.label = label;
  }

  public boolean equals(Object o) {
    if (!(o instanceof CRFLabel)) {
      return false;
    }
    CRFLabel other = (CRFLabel) o;

    if (other.label.length != label.length) {
      return false;
    }
    for (int i = 0; i < label.length; i++) {
      if (label[i] != other.label[i]) {
        return false;
      }
    }

    return true;
  }

  public CRFLabel getSmallerLabel(int size) {
    int[] newLabel = new int[size];
    System.arraycopy(label, label.length - size, newLabel, 0, size);
    return new CRFLabel(newLabel);
  }

  public CRFLabel getOneSmallerLabel() {
    return getSmallerLabel(label.length - 1);
  }

  public int[] getLabel() {
    return label;
  }

  public String toString(Index classIndex) {
    List l = new ArrayList();
    for (int i = 0; i < label.length; i++) {
      l.add(classIndex.get(label[i]));
    }
    return l.toString();
  }

  public String toString() {
    List l = new ArrayList();
    for (int i = 0; i < label.length; i++) {
      l.add(new Integer(label[i]));
    }
    return l.toString();
  }

  public int hashCode() {
    if (hashCode < 0) {
      hashCode = 0;
      for (int i = 0; i < label.length; i++) {
        hashCode *= maxNumClasses;
        hashCode += label[i];
      }
    }
    return hashCode;
  }

}
