package alteredu.stanford.nlp.util;


/**
 * Scored Object: Wrapper class for holding a scored object
 *
 * @author Dan Klein
 * @version 2/7/01
 */
public class ScoredObject implements Scored {

  private double score;

  public double score() {
    return score;
  }

  public void setScore(double score) {
    this.score = score;
  }


  private Object object;

  public Object object() {
    return object;
  }

  public void setObject(Object object) {
    this.object = object;
  }

  public ScoredObject() {
    object = null;
    score = 0;
  }

  public ScoredObject(Object object, double score) {
    this.object = object;
    this.score = score;
  }

  public String toString() {
    return object + " @ " + score;
  }

}
