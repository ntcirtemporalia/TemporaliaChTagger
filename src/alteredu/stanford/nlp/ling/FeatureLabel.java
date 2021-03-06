package alteredu.stanford.nlp.ling;

import alteredu.stanford.nlp.util.MapFactory;
import alteredu.stanford.nlp.util.Interner;
import alteredu.stanford.nlp.util.CollectionUtils;

import java.util.*;

/**
 * An <code>AbstractMapLabel</code> implementation which defines equality
 * as equality of the internal map.  Thus it is NOT SAFE for use in cyclic
 * data structures, in which the map may point to something which in turn
 * points to this map again, as calling equals() will lead to an infinite
 * recursion.
 * <p/>
 * As standardly used, both a key and the corresponding value of a
 * FeatureLabel are of type String.
 *
 * @author Jenny Finkel
 * @author Teg Grenager
 */

public class FeatureLabel extends AbstractMapLabel {

  public static String TOSTRING_FORMAT = null;
  public Collection features = null;

  // CONSTRUCTORS

  public FeatureLabel() {
    super();
  }

  public FeatureLabel(MapFactory mapFactory) {
    super(mapFactory);
  }

  public Set keySet() {
    return map.keySet();
  }
  
  /**
   *
   * @param keys
   * @param values
   */
  public FeatureLabel(String[] keys, String[] values) {
    for (int i = 0; i < keys.length && i < values.length; i++) {
      if (keys[i] == null) {
        continue;
      }
      put(keys[i], values[i]);
    }
  }

  /**
   * Uses String representation of a Map to populate Map with String keys and String values.
   */
  public static FeatureLabel valueOf(String s, MapFactory mf) throws Exception {
    return new FeatureLabel(CollectionUtils.getMapFromString(s, Class.forName("java.lang.String"),
                                                             Class.forName("java.lang.String"), mf));
  }

  /**
   * Copy constructor.  Makes a new FeatureLabel using the MapFactory and 
   * Map contents of the other AbstractMapLabel.
   */
  public FeatureLabel(AbstractMapLabel other) {
    super(other.mapFactory);
    map = mapFactory.newMap(initCapacity);
    map.putAll(other.map);
  }

  /**
   * Copy constructor.  Makes a new FeatureLabel using the MapFactory and
   * Map contents of the other AbstractMapLabel.
   */
  public FeatureLabel(Map map) {
    super(); // default map factory
    this.map = map;
  }

  public static String[] mapStringToArray(String map) {
    String[] m = map.split("[,;]");
    int maxIndex = 0;
    String[] keys = new String[m.length];
    int[] indices = new int[m.length];
    for (int i = 0; i < m.length; i++) {
      int index = m[i].lastIndexOf("=");
      keys[i] = m[i].substring(0, index);
      indices[i] = Integer.parseInt(m[i].substring(index + 1));
      if (indices[i] > maxIndex) {
        maxIndex = indices[i];
      }
    }
    String[] mapArr = new String[maxIndex + 1];
    Arrays.fill(mapArr, null);
    for (int i = 0; i < m.length; i++) {
      mapArr[indices[i]] = keys[i];
    }
    return mapArr;
  }

  /**
   * convenience method for getting word *
   */
  public String word() {
    return getString(WORD_KEY);
  }

  /**
   * convenience method for getting answer *
   */
  public String answer() {
    return getString(ANSWER_KEY);
  }

  /**
   * convenience method for getting gold answer *
   */
  public String goldAnswer() {
    return getString(GOLDANSWER_KEY);
  }

  /**
   * convenience method for setting word *
   */
  public void setWord(String word) {
    put(WORD_KEY, word);
  }

  /**
   * convenience method for setting answer *
   */
  public void setAnswer(String answer) {
    put(ANSWER_KEY, answer);
  }

  /**
   * convenience method for setting gold answer *
   */
  public void setGoldAnswer(String goldAnswer) {
    put(GOLDANSWER_KEY, goldAnswer);
  }

  /**
   * Return the String before the word,
   * which is stored in the map under the key {@link
   * AbstractMapLabel#BEFORE_KEY <code>BEFORE_KEY</code>}.
   *
   * @return the String before the word
   */
  public String before() {
    return getString(BEFORE_KEY);
  }

  /**
   * Set the String before the word by storing it in the map under
   * the key {@link AbstractMapLabel#BEFORE_KEY
   * <code>BEFORE_KEY</code>}.
   *
   * @param before the String before the word
   */
  public void setBefore(String before) {
    map.put(BEFORE_KEY, before);
  }

  /**
   * Prepend this String to the current before String
   *
   * @param before the String to be prepended
   */
  public void prependBefore(String before) {
    String oldBefore = before();
    setBefore(before + oldBefore);
  }

  /**
   * Return the String which is the unmangled word,
   * which is stored in the map under the key {@link
   * AbstractMapLabel#CURRENT_KEY <code>CURRENT_KEY</code>}.
   *
   * @return the unmangled word
   */
  public String current() {
    return getString(CURRENT_KEY);
  }

  /**
   * Set the String which is the unmangled word,
   * which is stored in the map under the key {@link
   * AbstractMapLabel#CURRENT_KEY <code>CURRENT_KEY</code>}.
   *
   * @param current the unmangled word
   */
  public void setCurrent(String current) {
    map.put(CURRENT_KEY, current);
  }

  /**
   * Return the String after the word,
   * which is stored in the map under the key {@link
   * AbstractMapLabel#AFTER_KEY <code>AFTER_KEY</code>}.
   *
   * @return the String after the word
   */
  public String after() {
    return getString(AFTER_KEY);
  }

  /**
   * Set the String after the word by storing it in the map under
   * the key {@link AbstractMapLabel#AFTER_KEY
   * <code>AFTER_KEY</code>}.
   *
   * @param after The String after the word
   */
  public void setAfter(String after) {
    map.put(AFTER_KEY, after);
  }

  /**
   * Append this String to the current after String
   *
   * @param after The String to be prepended
   */
  public void appendAfter(String after) {
    String oldAfter = after();
    setAfter(oldAfter + after);
  }

  /**
   * Return the NER type of the word,
   * which is stored in the map under the key {@link
   * AbstractMapLabel#NER_KEY <code>NER_KEY</code>}.
   *
   * @return the String after the word
   */
  public String ner() {
    return getString(NER_KEY);
  }

  /**
   * Set the NER label for the word, using
   * the key {@link AbstractMapLabel#NER_KEY
   * <code>NER_KEY</code>}.
   *
   * @param ner The String ner the word
   */
  public void setNER(String ner) {
    map.put(NER_KEY, ner);
  }

  /**
   * Pieces a List of MapLabels back together using before, after
   * and current.
   */
  public static String toOriginalString(List<FeatureLabel> sentence) {
    StringBuilder text = new StringBuilder();
    for (int i = 0, sz = sentence.size(); i < sz; i++) {
      FeatureLabel iw = sentence.get(i);
      text.append(iw.before());
      text.append(iw.current());
      if (i == sz - 1) {
        text.append(iw.after());
      }
    }
    return text.toString();
  }

  /**
   * Pieces a List of MapLabels back together using
   * word and setting a white space between each word
   */
  public static String toSentence(List<FeatureLabel> sentence) {
    StringBuilder text = new StringBuilder();
    for (int i = 0, sz = sentence.size(); i < sz; i++) {
      FeatureLabel iw = sentence.get(i);
      text.append(iw.word());
      if (i < sz - 1) {
        text.append(" ");
      }
    }
    return text.toString();
  }



  public String value() {
    return getString(VALUE_KEY);
  }

  public void setValue(String value) {
    put(VALUE_KEY, value);
  }

  public String toString() {
    return toString(TOSTRING_FORMAT);
  }

  public String toString(String format) {
    if (format == null || format.equals("")) {
      return map.toString(); 
    } else if (format.equals("word")) {
      return word();
    } else if (format.equals("wordtag")) {
      if (tag() != null) {
        return word() + tag();
      } else {
        return word();
      }
    } else {
      return map.toString(); 
    }        
  }

  public void setFromString(String labelStr) {
    put(VALUE_KEY, labelStr);
  }

  public LabelFactory labelFactory() {
    return new FeatureLabelFactory();
  }

  public static LabelFactory factory() {
    return new FeatureLabelFactory();
  }

  /**
   * Interns all of the keys and values in the underlying map of this FeatureLabel.
   * @param interner
   */
  public void internValues(Interner interner) {
    Map newMap = mapFactory.newMap(initCapacity);
    for (Object o : map.entrySet()) {
      Map.Entry entry = (Map.Entry) o;
      Object key = entry.getKey();
      Object value = entry.getValue();
      newMap.put(key, interner.intern(value));
    }
    map = newMap;
  }

  private static class FeatureLabelFactory implements LabelFactory {

    private FeatureLabelFactory() {}

    public Label newLabel(String labelStr) {
      FeatureLabel result = new FeatureLabel();
      result.setValue(labelStr);
      return result;
    }

    public Label newLabel(String labelStr, int options) {
      FeatureLabel result = new FeatureLabel();
      result.setValue(labelStr);
      return result;
    }

    public Label newLabelFromString(String encodedLabelStr) {
      FeatureLabel result = new FeatureLabel();
      result.setValue(encodedLabelStr);
      return result;
    }

    public Label newLabel(Label oldLabel) {
      FeatureLabel result = new FeatureLabel();
      result.setValue(oldLabel.value());
      return result;
    }

  } // end class FeatureLabelFactory


  public String lemma() {
    return getString(LEMMA_KEY);
  }


  public String tag() {
    return getString(TAG_KEY);
  }

  /** Return the String value of the FeatureLabel for an arbitrary key.
   *  See AbstractMapLabel for standard attribute key names, though anything
   *  can be used.  The return value of this method is always a non-null
   *  String.  If the attribute does not exist in the Map, then the empty 
   *  String is returned.
   * <p/>
   * <i>Note: This behavior is central to how our IE systems
   *  work and should not be changed!!</i>
   */
  public String getString(Object attribute) {
    String v = (String) map.get(attribute);
    if (v == null) {
      return "";
    }
    return v;
  }

  public void set(Object attribute, Object value) {
    put(attribute, value);
  }

  public Map map() {
    return map;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof FeatureLabel)) return false;
    final FeatureLabel featureLabel = (FeatureLabel) o;

    return map == null ? featureLabel.map == null : map.equals(featureLabel.map);
  }

  public int hashCode() {
    return (map != null ? map.hashCode() : 7);
  }

  private static final long serialVersionUID = 19L;
  
  public void remove(String key) {
    map.remove(key);
  }

}

