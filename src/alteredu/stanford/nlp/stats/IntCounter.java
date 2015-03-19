package alteredu.stanford.nlp.stats;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.*;

import alteredu.stanford.nlp.util.*;

/**
 * A specialized kind of hash table (or map) for storing numeric counts for
 * objects. It works like a Map,
 * but with different methods for easily getting/setting/incrementing counts
 * for objects and computing various functions with the counts.
 * The Counter constructor
 * and <tt>addAll</tt> method can be used to copy another Counter's contents
 * over. This class also provides access
 * to Comparators that can be used to sort the keys or entries of this Counter
 * by the counts, in either ascending or descending order.
 *
 * @author Dan Klein (klein@cs.stanford.edu)
 * @author Joseph Smarr (jsmarr@stanford.edu)
 * @author Teg Grenager (grenager@stanford.edu)
 * @author Galen Andrew
 * @author Christopher Manning
 */
public class IntCounter<E> implements Serializable, GenericCounter<E> {

  private Map<E, MutableInteger>  map;
  private MapFactory mapFactory;

  /**
   * Default comparator for breaking ties in argmin and argmax.
   */
  private static final Comparator naturalComparator = new NaturalComparator();
  private static final long serialVersionUID = 4;

  // CONSTRUCTORS

  /**
   * Constructs a new (empty) Counter.
   */
  public IntCounter() {
    this(MapFactory.HASH_MAP_FACTORY);
  }

  /**
   * Pass in a MapFactory and the map it vends will back your counter.
   */
  public IntCounter(MapFactory mapFactory) {
    this.mapFactory = mapFactory;
    map = mapFactory.newMap();
  }

  /**
   * Constructs a new Counter with the contents of the given Counter.
   */
  public IntCounter(IntCounter<E> c) {
    this();
    addAll(c);
  }


  // STANDARD ACCESS MODIFICATION METHODS

  public MapFactory getMapFactory() {
    return mapFactory;
  }

  /**
   * Returns the current total count for all objects in this Counter.
   * All counts are summed each time, so cache it if you need it repeatedly.
   */
  public int totalCount() {
    int total = 0;
    for (Iterator iter = map.keySet().iterator(); iter.hasNext();) {
      Object key = iter.next();
      total += getIntCount(key);
    }
    return total;
  }

  public double totalDoubleCount() {
    return totalCount();
  }

  /**
   * Returns the total count for all objects in this Counter that pass the
   * given Filter. Passing in a filter that always returns true is equivalent
   * to calling {@link #totalCount()}.
   */
  public int totalCount(Filter filter) {
    int total = 0;
    for (Iterator iter = map.keySet().iterator(); iter.hasNext();) {
      Object key = iter.next();
      if (filter.accept(key)) {
        total += getIntCount(key);
      }
    }
    return (total);
  }

  public double totalDoubleCount(Filter filter) {
    return totalCount(filter);
  }

  /**
   * Returns the mean of all the counts (totalCount/size).
   */
  public double averageCount() {
    return ((double) totalCount() / (double) map.size());
  }

  /**
   * Returns the current count for the given key, which is 0 if it hasn't
   * been
   * seen before. This is a convenient version of <code>get</code> that casts
   * and extracts the primitive value.
   */
  public double getCount(E key) {
    return getIntCount(key);
  }

  public String getCountAsString(E key) {
    return Integer.toString(getIntCount(key));
  }

  /**
   * Returns the current count for the given key, which is 0 if it hasn't
   * been
   * seen before. This is a convenient version of <code>get</code> that casts
   * and extracts the primitive value.
   */
  public int getIntCount(Object key) {
    MutableInteger count =  map.get(key);
    if (count == null) {
      return 0; // haven't seen this object before -> 0 count
    }
    return (count.intValue());
  }

  /**
   * This has been de-deprecated in order to reduce compilation warnings, but
   * really you should create a {@link edu.stanford.nlp.stats.Distribution} instead of using this method.
   */
  public double getNormalizedCount(E key) {
    return ((double) getCount(key) / (totalCount()));
  }

  /**
   * Sets the current count for the given key. This will wipe out any existing
   * count for that key.
   * <p/>
   * To add to a count instead of replacing it, use
   * {@link #incrementCount(Object,int)}.
   */
  public void setCount(E key, int count) {
    if (tempMInteger == null) {
      tempMInteger = new MutableInteger();
    }
    tempMInteger.set(count);
    tempMInteger = map.put(key, tempMInteger);
  }

  public void setCount(E key, String s) {
    setCount(key, Integer.parseInt(s));
  }

  // for more efficient memory usage
  private transient MutableInteger tempMInteger = null;

  /**
   * Sets the current count for each of the given keys. This will wipe out
   * any existing counts for these keys.
   * <p/>
   * To add to the counts of a collection of objects instead of replacing them,
   * use {@link #incrementCounts(Collection,int)}.
   */
  public void setCounts(Collection<E> keys, int count) {
    for (Iterator<E> iter = keys.iterator(); iter.hasNext();) {
      setCount(iter.next(), count);
    }
  }

  /**
   * Adds the given count to the current count for the given key. If the key
   * hasn't been seen before, it is assumed to have count 0, and thus this
   * method will set its count to the given amount. Negative increments are
   * equivalent to calling <tt>decrementCount</tt>.
   * <p/>
   * To more conviently increment the count by 1, use
   * {@link #incrementCount(Object)}.
   * <p/>
   * To set a count to a specifc value instead of incrementing it, use
   * {@link #setCount(Object,int)}.
   */
  public void incrementCount(E key, int count) {
    if (tempMInteger == null) {
      tempMInteger = new MutableInteger();
    }
    tempMInteger.set(count);
    MutableInteger oldMInteger = map.put(key, tempMInteger);
    if (oldMInteger != null) {
      tempMInteger.set(count + oldMInteger.intValue());
    }
    tempMInteger = oldMInteger;
    //      total += count;
    //put(key,new Integer(count+getCount(key)));
  }

  /**
   * Adds 1 to the count for the given key. If the key hasn't been seen
   * before, it is assumed to have count 0, and thus this method will set
   * its count to 1.
   * <p/>
   * To increment the count by a value other than 1, use
   * {@link #incrementCount(Object,int)}.
   * <p/>
   * To set a count to a specifc value instead of incrementing it, use
   * {@link #setCount(Object,int)}.
   */
  public void incrementCount(E key) {
    incrementCount(key, 1);
  }

  /**
   * Adds the given count to the current counts for each of the given keys.
   * If any of the keys haven't been seen before, they are assumed to have
   * count 0, and thus this method will set their counts to the given
   * amount. Negative increments are equivalent to calling <tt>decrementCounts</tt>.
   * <p/>
   * To more conviniently increment the counts of a collection of objects by
   * 1, use {@link #incrementCounts(Collection)}.
   * <p/>
   * To set the counts of a collection of objects to a specific value instead
   * of incrementing them, use {@link #setCounts(Collection,int)}.
   */
  public void incrementCounts(Collection<E> keys, int count) {
    for (Iterator<E> iter = keys.iterator(); iter.hasNext();) {
      incrementCount(iter.next(), count);
    }
  }

  /**
   * Adds 1 to the counts for each of the given keys. If any of the keys
   * haven't been seen before, they are assumed to have count 0, and thus
   * this method will set their counts to 1.
   * <p/>
   * To increment the counts of a collection of object by a value other
   * than 1, use {@link #incrementCounts(Collection,int)}.
   * <p/>
   * To set the counts of a collection of objects  to a specific value instead
   * of incrementing them, use  {@link #setCounts(Collection,int)}.
   */
  public void incrementCounts(Collection<E> keys) {
    incrementCounts(keys, 1);
  }

  /**
   * Subtracts the given count from the current count for the given key.
   * If the key hasn't been seen before, it is assumed to have count 0, and
   * thus this  method will set its count to the negative of the given amount.
   * Negative increments are equivalent to calling <tt>incrementCount</tt>.
   * <p/>
   * To more conviently decrement the count by 1, use
   * {@link #decrementCount(Object)}.
   * <p/>
   * To set a count to a specifc value instead of decrementing it, use
   * {@link #setCount(Object,int)}.
   */
  public void decrementCount(E key, int count) {
    incrementCount(key, -count);
  }

  /**
   * Subtracts 1 from the count for the given key. If the key hasn't been
   * seen  before, it is assumed to have count 0, and thus this method will
   * set its count to -1.
   * <p/>
   * To decrement the count by a value other than 1, use
   * {@link #decrementCount(Object,int)}.
   * <p/>
   * To set a count to a specifc value instead of decrementing it, use
   * {@link #setCount(Object,int)}.
   */
  public void decrementCount(E key) {
    decrementCount(key, 1);
  }

  /**
   * Subtracts the given count from the current counts for each of the given keys.
   * If any of the keys haven't been seen before, they are assumed to have
   * count 0, and thus this method will set their counts to the negative of the given
   * amount. Negative increments are equivalent to calling <tt>incrementCount</tt>.
   * <p/>
   * To more conviniently decrement the counts of a collection of objects by
   * 1, use {@link #decrementCounts(Collection)}.
   * <p/>
   * To set the counts of a collection of objects to a specific value instead
   * of decrementing them, use {@link #setCounts(Collection,int)}.
   */
  public void decrementCounts(Collection<E> keys, int count) {
    incrementCounts(keys, -count);
  }

  /**
   * Subtracts 1 from the counts of each of the given keys. If any of the keys
   * haven't been seen before, they are assumed to have count 0, and thus
   * this method will set their counts to -1.
   * <p/>
   * To decrement the counts of a collection of object by a value other
   * than 1, use {@link #decrementCounts(Collection,int)}.
   * <p/>
   * To set the counts of a collection of objects  to a specifc value instead
   * of decrementing them, use  {@link #setCounts(Collection,int)}.
   */
  public void decrementCounts(Collection<E> keys) {
    decrementCounts(keys, 1);
  }

  /**
   * Adds the counts in the given Counter to the counts in this Counter.
   * <p/>
   * To copy the values from another Counter rather than adding them, use
   */
  public void addAll(IntCounter<E> counter) {
    for (Iterator<E> iter = counter.keySet().iterator(); iter.hasNext();) {
      E key = iter.next();
      int count = counter.getIntCount(key);
      incrementCount(key, count);
    }
  }

  /**
   * Subtracts the counts in the given Counter from the counts in this Counter.
   * <p/>
   * To copy the values from another Counter rather than subtracting them, use
   */
  public void subtractAll(IntCounter<E> counter) {
    for (Iterator<E> iter = map.keySet().iterator(); iter.hasNext();) {
      E key = iter.next();
      decrementCount(key, counter.getIntCount(key));
    }
  }

  // MAP LIKE OPERATIONS

  public boolean containsKey(E key) {
    return map.containsKey(key);
  }

  /**
   * Removes the given key from this Counter. Its count will now be 0 and it
   * will no longer be considered previously seen.
   */
  public Object remove(E key) {
    //      total-=getCount(key); // subtract removed count from total (may be 0)
    return (map.remove(key));
  }

  /**
   * Removes all the given keys from this Counter.
   */
  public void removeAll(Collection<E> c) {
    for (Iterator<E> iter = c.iterator(); iter.hasNext();) {
      remove(iter.next());
    }
  }

  /**
   * Removes all counts from this Counter.
   */
  public void clear() {
    map.clear();
    //        total=0;
  }

  public int size() {
    return map.size();
  }

  public boolean isEmpty() {
    return (size() == 0);
  }

  public Set<E> keySet() {
    return map.keySet();
  }

  // OBJECT STUFF

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof IntCounter)) {
      return false;
    }

    final IntCounter counter = (IntCounter) o;

    return map.equals(counter.map);
  }

  public int hashCode() {
    return map.hashCode();
  }

  public String toString() {
    return map.toString();
  }


  public String toString(NumberFormat nf, String preAppend, String postAppend, String keyValSeparator, String itemSeparator) {
    StringBuffer sb = new StringBuffer();
    sb.append(preAppend);
    List list = new ArrayList(map.keySet());
    try {
      Collections.sort(list); // see if it can be sorted
    } catch (Exception e) {
    }
    for (Iterator iter = list.iterator(); iter.hasNext();) {
      Object key = iter.next();
      MutableInteger d = map.get(key);
      sb.append(key + keyValSeparator);
      sb.append(nf.format(d));
      if (iter.hasNext()) {
        sb.append(itemSeparator);
      }
    }
    sb.append(postAppend);
    return sb.toString();
  }


  public String toString(NumberFormat nf) {
    StringBuffer sb = new StringBuffer();
    sb.append("{");
    List list = new ArrayList(map.keySet());
    try {
      Collections.sort(list); // see if it can be sorted
    } catch (Exception e) {
    }
    for (Iterator iter = list.iterator(); iter.hasNext();) {
      Object key = iter.next();
      MutableInteger d = map.get(key);
      sb.append(key + "=");
      sb.append(nf.format(d));
      if (iter.hasNext()) {
        sb.append(", ");
      }
    }
    sb.append("}");
    return sb.toString();
  }

  public Object clone() {
    return new IntCounter(this);
  }

  // EXTRA CALCULATION METHODS

  /**
   * Removes all keys whose count is 0. After incrementing and decrementing
   * counts or adding and subtracting Counters, there may be keys left whose
   * count is 0, though normally this is undesirable. This method cleans up
   * the map.
   * <p/>
   * Maybe in the future we should try to do this more on-the-fly, though it's
   * not clear whether a distinction should be made between "never seen" (i.e.
   * null count) and "seen with 0 count". Certainly there's no distinction in
   * getCount() but there is in containsKey().
   */
  public void removeZeroCounts() {
    for (Iterator<E> iter = map.keySet().iterator(); iter.hasNext();) {
      if (getCount(iter.next()) == 0) {
        iter.remove();
      }
    }
  }

  /**
   * Finds and returns the largest count in this Counter.
   */
  public int max() {
    int max = Integer.MIN_VALUE;
    for (Iterator<E> iter = map.keySet().iterator(); iter.hasNext();) {
      max = Math.max(max, getIntCount(iter.next()));
    }
    return (max);
  }

  public double doubleMax() {
    return max();
  }

  /**
   * Finds and returns the smallest count in this Counter.
   */
  public int min() {
    int min = Integer.MAX_VALUE;
    for (Iterator iter = map.keySet().iterator(); iter.hasNext();) {
      min = Math.min(min, getIntCount(iter.next()));
    }
    return (min);
  }

  /**
   * Finds and returns the key in this Counter with the largest count.
   * Ties are broken by comparing the objects using the given tie breaking
   * Comparator, favoring Objects that are sorted to the front. This is useful
   * if the keys are numeric and there is a bias to prefer smaller or larger
   * values, and can be useful in other circumstances where random tie-breaking
   * is not desirable. Returns null if this Counter is empty.
   */
  public E argmax(Comparator tieBreaker) {
    int max = Integer.MIN_VALUE;
    E argmax = null;
    for (Iterator<E> iter = keySet().iterator(); iter.hasNext();) {
      E key = iter.next();
      int count = getIntCount(key);
      if (argmax == null || count > max || (count == max && tieBreaker.compare(key, argmax) < 0)) {
        max = count;
        argmax = key;
      }
    }
    return (argmax);
  }


  /**
   * Finds and returns the key in this Counter with the largest count.
   * Ties are broken according to the natural ordering of the objects.
   * This will prefer smaller numeric keys and lexicographically earlier
   * String keys. To use a different tie-breaking Comparator, use
   * {@link #argmax(Comparator)}. Returns null if this Counter is empty.
   */
  public E argmax() {
    return (argmax(naturalComparator));
  }

  /**
   * Finds and returns the key in this Counter with the smallest count.
   * Ties are broken by comparing the objects using the given tie breaking
   * Comparator, favoring Objects that are sorted to the front. This is useful
   * if the keys are numeric and there is a bias to prefer smaller or larger
   * values, and can be useful in other circumstances where random tie-breaking
   * is not desirable. Returns null if this Counter is empty.
   */
  public E argmin(Comparator tieBreaker) {
    int min = Integer.MAX_VALUE;
    E argmin = null;
    for (Iterator<E> iter = map.keySet().iterator(); iter.hasNext();) {
      E key = iter.next();
      int count = getIntCount(key);
      if (argmin == null || count < min || (count == min && tieBreaker.compare(key, argmin) < 0)) {
        min = count;
        argmin = key;
      }
    }
    return (argmin);
  }

  /**
   * Finds and returns the key in this Counter with the smallest count.
   * Ties are broken according to the natural ordering of the objects.
   * This will prefer smaller numeric keys and lexicographically earlier
   * String keys. To use a different tie-breaking Comparator, use
   * {@link #argmin(Comparator)}. Returns null if this Counter is empty.
   */
  public E argmin() {
    return (argmin(naturalComparator));
  }

  /**
   * Returns the set of keys whose counts are at or above the given threshold.
   * This set may have 0 elements but will not be null.
   */
  public Set keysAbove(int countThreshold) {
    Set keys = new HashSet();
    for (Iterator iter = map.keySet().iterator(); iter.hasNext();) {
      Object key = iter.next();
      if (getIntCount(key) >= countThreshold) {
        keys.add(key);
      }
    }
    return (keys);
  }

  /**
   * Returns the set of keys whose counts are at or below the given threshold.
   * This set may have 0 elements but will not be null.
   */
  public Set keysBelow(int countThreshold) {
    Set keys = new HashSet();
    for (Iterator iter = map.keySet().iterator(); iter.hasNext();) {
      Object key = iter.next();
      if (getIntCount(key) <= countThreshold) {
        keys.add(key);
      }
    }
    return (keys);
  }

  /**
   * Returns the set of keys that have exactly the given count.
   * This set may have 0 elements but will not be null.
   */
  public Set keysAt(int count) {
    Set keys = new HashSet();
    for (Iterator iter = map.keySet().iterator(); iter.hasNext();) {
      Object key = iter.next();
      if (getIntCount(key) == count) {
        keys.add(key);
      }
    }
    return (keys);
  }

  /**
   * Returns a comparator suitable for sorting this Counter's keys or entries
   * by their respective counts. If <tt>ascending</tt> is true, lower counts
   * will be returned first, otherwise higher counts will be returned first.
   * <p/>
   * Sample usage:
   * <pre>
   * Counter c = new Counter();
   * // add to the counter...
   * List biggestKeys = new ArrayList(c.keySet());
   * Collections.sort(biggestKeys, c.comparator(false));
   * List smallestEntries = new ArrayList(c.entrySet());
   * Collections.sort(smallestEntries, c.comparator(true))
   * </pre>
   */
  public Comparator comparator(boolean ascending) {
    return (new EntryValueComparator(map, ascending));
  }

  /**
   * Returns a comparator suitable for sorting this Counter's keys or entries
   * by their respective value or magnitude (unsigned value).
   * If <tt>ascending</tt> is true, smaller magnitudes will
   * be returned first, otherwise higher magnitudes will be returned first.
   * <p/>
   * Sample usage:
   * <pre>
   * Counter c = new Counter();
   * // add to the counter...
   * List biggestKeys = new ArrayList(c.keySet());
   * Collections.sort(biggestKeys, c.comparator(false, true));
   * List smallestEntries = new ArrayList(c.entrySet());
   * Collections.sort(smallestEntries, c.comparator(true))
   * </pre>
   */
  public Comparator comparator(boolean ascending, boolean useMagnitude) {
    return (new EntryValueComparator(map, ascending, useMagnitude));
  }

  /**
   * Comparator that sorts objects by (increasing) count. Shortcut for calling
   * {@link #comparator(boolean) comparator(true)}.
   */
  public Comparator comparator() {
    return (comparator(true));
  }

  /**
   * Comparator that uses natural ordering.
   * Returns 0 if o1 is not Comparable.
   */
  private static class NaturalComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      if (o1 instanceof Comparable) {
        return (((Comparable) o1).compareTo(o2));
      }
      return (0); // soft-fail
    }
  }
}
