package alteredu.stanford.nlp.ling;

import java.io.Serializable;

/**
 * A <code>StringLabel</code> object acts as a Label by containing a
 * single String, which it sets or returns in response to requests.
 * The hashCode() and compareTo() methods for this class assume that this
 * string value is non-null.  equals() is correctly implemented
 *
 * @author Christopher Manning
 * @version 2000/12/20
 */
public class StringLabel extends ValueLabel {

  private String str;


  /**
   * Create a new <code>StringLabel</code> with a null content (i.e., str).
   */
  public StringLabel() {
  }


  /**
   * Create a new <code>StringLabel</code> with the given content.
   *
   * @param str The new label's content
   */
  public StringLabel(String str) {
    this.str = str;
  }


  /**
   * Create a new <code>StringLabel</code> with the
   * <code>value()</code> of another label as its label.
   *
   * @param label The other label
   */
  public StringLabel(Label label) {
    this.str = label.value();
  }


  /**
   * Return the word value of the label (or null if none).
   *
   * @return String the word value for the label
   */
  public String value() {
    return str;
  }


  /**
   * Set the value for the label.
   *
   * @param value The value for the label
   */
  public void setValue(final String value) {
    str = value;
  }


  /**
   * Set the label from a String.
   *
   * @param str The str for the label
   */
  public void setFromString(final String str) {
    this.str = str;
  }


  // extra class guarantees correct lazy loading (Bloch p.194)
  private static class StringLabelFactoryHolder {
    static final LabelFactory lf = new StringLabelFactory();
  }

  /**
   * Return a factory for this kind of label
   * (i.e., <code>StringLabel</code>).
   * The factory returned is always the same one (a singleton).
   *
   * @return The label factory
   */
  public LabelFactory labelFactory() {
    return StringLabelFactoryHolder.lf;
  }


  /**
   * Return a factory for this kind of label.
   *
   * @return The label factory
   */
  public static LabelFactory factory() {
    return StringLabelFactoryHolder.lf;
  }

}
