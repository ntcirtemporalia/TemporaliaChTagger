package alteredu.stanford.nlp.stats;

import alteredu.stanford.nlp.util.MapFactory;

import java.util.Set;
import java.util.Comparator;

/**
 * Interface to a generic (type-independent) Counter.
 * We go through this interface whenever possible to keep generic the
 * higher-level routines in Distribution and Counters.
 */
public interface GenericCounter<E> {

  /**
   * @return true iff key is a key in this GenericCounter.
   */
  public boolean containsKey(E key);


  /**
   * Returns the count for this key as a double.
   */
  public double getCount(E key);

  /**
   * Returns the count for this key as a String.
   */
  public String getCountAsString(E key);

  /**
   * Sets the count for this key to be the number encoded in the given
   * String.
   */
  public void setCount(E key, String s);

  /**
   * Computes the total of all counts in this counter, and returns it
   * as a double.
   */
  public double totalDoubleCount();

  /**
   * Returns the Set of keys in this counter.
   */
  public Set<E> keySet();

  /**
   * Returns the number of entries in this counter.
   */
  public int size();

  /**
   * Returns the value of the maximum entry in this counter, as a double.
   */
  public double doubleMax();

  /**
   * Returns the MapFactory used by this counter.
   */
  public MapFactory getMapFactory();
  
  /**
   * Returns a comparator
   */
  public Comparator<E> comparator();
}
