package alteredu.stanford.nlp.stats;

import alteredu.stanford.nlp.util.EntryValueComparator;
import alteredu.stanford.nlp.util.Filter;
import alteredu.stanford.nlp.util.PriorityQueue;
import alteredu.stanford.nlp.util.BinaryHeapPriorityQueue;
import alteredu.stanford.nlp.util.MapFactory;
import alteredu.stanford.nlp.util.MutableDouble;
import alteredu.stanford.nlp.math.ArrayMath;
import alteredu.stanford.nlp.math.SloppyMath;

import java.io.*;
import java.text.NumberFormat;
import java.util.*;

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
 * <p/>
 * <i>Implementation note:</i> Note that this class stores a 
 * <code>totalCount</code> field as well as the map.  This makes certain
 * operations much more efficient, but means that any methods that change the
 * map must also update <code>totalCount</code> appropriately. If you use the
 * <code>setCount</code> method, then you cannot go wrong.
 *
 * @author Dan Klein (klein@cs.stanford.edu)
 * @author Joseph Smarr (jsmarr@stanford.edu)
 * @author Teg Grenager
 * @author Galen Andrew
 * @author Christopher Manning
 * @author Kayur Patel (kdpatel@cs)
 */
public class Counter <E> implements Serializable, GenericCounter<E>,Iterable<E> {

  Map<E, MutableDouble> map;  // accessed by DeltaCounter
  MapFactory mapFactory;      // accessed by DeltaCounter
  private double totalCount;

  /**
   * Default comparator for breaking ties in argmin and argmax.
   */
  private static final Comparator naturalComparator = new NaturalComparator();
  private static final long serialVersionUID = 4;

  // for more efficient memory usage
  private transient MutableDouble tempMDouble = null;

  // CONSTRUCTORS

  /**
   * Constructs a new (empty) Counter.
   */
  public Counter() {
    this(MapFactory.HASH_MAP_FACTORY);
  }

  /**
   * Pass in a MapFactory and the map it vends will back your counter.
   */
  public Counter(MapFactory mapFactory) {
    this.mapFactory = mapFactory;
    //map = mapFactory.newMap();
    map = mapFactory.setMap(map);
    totalCount = 0.0;
  }

  /**
   * Constructs a new Counter with the contents of the given Counter.
   */
  public Counter(GenericCounter<E> c) {
    this();
    addAll(c);
  }

  /**
   * Constructs a new Counter by counting the elements in the given Collection.
   */
  public Counter(Collection<E> collection) {
    this();
    addAll(collection);
  }


  // STANDARD ACCESS MODIFICATION METHODS

  public MapFactory getMapFactory() {
    return mapFactory;
  }

  /**
   * Returns the current total count for all objects in this Counter.
   */
  public double totalCount() {
    return totalCount;
  }

  /* All counts are summed each time, so cache it if you need it repeatedly.
   * public double totalCount() {
   * double total = 0.0;
   * for (E key : map.keySet()) {
   * total += getCount(key);
   * }
   * return total;
   * }
   */

  public Iterator<E> iterator() { return keySet().iterator(); }
  
  /** Returns the current total count for all objects in this Counter.
   */
  public double totalDoubleCount() {
    return totalCount();
  }

  /**
   * Returns the total count for all objects in this Counter that pass the
   * given Filter. Passing in a filter that always returns true is equivalent
   * to calling {@link #totalCount()}.
   */
  public double totalCount(Filter<E> filter) {
    double total = 0.0;
    for (E key : map.keySet()) {
      if (filter.accept(key)) {
        total += getCount(key);
      }
    }
    return (total);
  }

  public double logSum() {
    double[] toSum = new double[map.size()];
    int i = 0;
    for (E key : map.keySet()) {
      toSum[i++] = getCount(key);
    }
    return ArrayMath.logSum(toSum);
  }

  public void logNormalize() {
    incrementAll(-logSum());
  }
  
  /**
   * Returns the mean of all the counts (totalCount/size).
   */
  public double averageCount() {
    return (totalCount() / map.size());
  }

  /**
   * Returns the current count for the given key, which is 0 if it hasn't
   * been
   * seen before. This is a convenient version of <code>get</code> that casts
   * and extracts the primitive value.
   */
  public double getCount(E key) {
    Number count = map.get(key);
    if (count == null) {
      return 0.0; // haven't seen this object before -> 0 count
    }
    return count.doubleValue();
  }

  public String getCountAsString(E key) {
    return Double.toString(getCount(key));
  }

  /** Return the proportion of the Counter mass under this key.
   *  This has been de-deprecated in order to reduce compilation warnings, but
   *  really you should create a {@link Distribution} instead of using this
   *  method.
   */
  public double getNormalizedCount(E key) {
    return getCount(key) / totalCount();
  }

  /**
   * Sets the current count for the given key. This will wipe out any existing
   * count for that key.
   * <p/>
   * To add to a count instead of replacing it, use
   * {@link #incrementCount(Object,double)}.
   */
  public void setCount(E key, double count) {
    if (tempMDouble == null) {
      //System.out.println("creating mdouble");
      tempMDouble = new MutableDouble();
    }
    //System.out.println("setting mdouble");
    tempMDouble.set(count);
    //System.out.println("putting mdouble in map");        
    tempMDouble = map.put(key, tempMDouble);
    //System.out.println("placed mDouble in map");

    totalCount += count;
    if (tempMDouble != null) {
      totalCount -= tempMDouble.doubleValue();
    }
  }

  public void setCount(E key, String s) {
    setCount(key, Double.parseDouble(s));
  }

  /**
   * Sets the current count for each of the given keys. This will wipe out
   * any existing counts for these keys.
   * <p/>
   * To add to the counts of a collection of objects instead of replacing them,
   * use {@link #incrementCounts(Collection, double)}.
   */
  public void setCounts(Collection<E> keys, double count) {
    for (E key : keys) {
      setCount(key, count);
    }
  }

  /**
   * Adds the given count to the current count for the given key. If the key
   * hasn't been seen before, it is assumed to have count 0, and thus this
   * method will set its count to the given amount. Negative increments are
   * equivalent to calling <tt>decrementCount</tt>.
   * <p/>
   * To more conveniently increment the count by 1.0, use
   * {@link #incrementCount(Object)}.
   * <p/>
   * To set a count to a specifc value instead of incrementing it, use
   * {@link #setCount(Object,double)}.
   */
  public void incrementCount(E key, double count) {
    if (tempMDouble == null) {
      tempMDouble = new MutableDouble();
    }
    tempMDouble.set(count);
    MutableDouble oldMDouble = map.put(key, tempMDouble);
    if (oldMDouble != null) {
      tempMDouble.set(count + oldMDouble.doubleValue());
    }
    tempMDouble = oldMDouble;

    totalCount += count;
    //      total += count;
    //put(key,new Double(count+getCount(key)));
  }

  /**
   * If the current count for the object is c1, and you call
   * logIncrementCount with a value of c2, then the new value will
   * be log(e^c1 + e^c2). If the key
   * hasn't been seen before, it is assumed to have count Double.NEGATIVE_INFINITY,
   * and thus this
   * method will set its count to the given amount. 
   * To set a count to a specifc value instead of incrementing it, use
   * {@link #setCount(Object,double)}.
   */
  public void logIncrementCount(E key, double count) {
    if (tempMDouble == null) {
      tempMDouble = new MutableDouble();
    }
    tempMDouble.set(count);
    MutableDouble oldMDouble = map.put(key, tempMDouble);
    if (oldMDouble != null) {
      count = SloppyMath.logAdd(count, oldMDouble.doubleValue());
      tempMDouble.set(count);
      totalCount += count - oldMDouble.doubleValue();
    } else {
      totalCount += count;
    }
    tempMDouble = oldMDouble;
    //      total += count;
    //put(key,new Double(count+getCount(key)));
  }  

  
  /**
   * Adds 1.0 to the count for the given key. If the key hasn't been seen
   * before, it is assumed to have count 0, and thus this method will set
   * its count to 1.0.
   * <p/>
   * To increment the count by a value other than 1.0, use
   * {@link #incrementCount(Object,double)}.
   * <p/>
   * To set a count to a specifc value instead of incrementing it, use
   * {@link #setCount(Object,double)}.
   */
  public void incrementCount(E key) {
    incrementCount(key, 1.0);
  }

  /**
   * Adds the given count to the current counts for each of the given keys.
   * If any of the keys haven't been seen before, they are assumed to have
   * count 0, and thus this method will set their counts to the given
   * amount. Negative increments are equivalent to calling 
   * <tt>decrementCounts</tt>.
   * <p/>
   * To more conviniently increment the counts of a collection of objects by
   * 1.0, use {@link #incrementCounts(Collection)}.
   * <p/>
   * To set the counts of a collection of objects to a specific value instead
   * of incrementing them, use {@link #setCounts(Collection,double)}.
   */
  public void incrementCounts(Collection<E> keys, double count) {
    for (E key : keys) {
      incrementCount(key, count);
    }
  }

  /**
   * Adds 1.0 to the counts for each of the given keys. If any of the keys
   * haven't been seen before, they are assumed to have count 0, and thus
   * this method will set their counts to 1.0.
   * <p/>
   * To increment the counts of a collection of object by a value other
   * than 1.0, use {@link #incrementCounts(Collection,double)}.
   * <p/>
   * To set the counts of a collection of objects  to a specifc value instead
   * of incrementing them, use  {@link #setCounts(Collection,double)}.
   */
  public void incrementCounts(Collection<E> keys) {
    incrementCounts(keys, 1.0);
  }

  /**
   * Adds the same amount to every count, that is to every key currently
   * stored in the counter (with no lookups).
   *
   * @param count The amount to be added
   */
  public void incrementAll(double count) {
    for (MutableDouble md : map.values()) {
      md.set(md.doubleValue() + count);
      totalCount += count;
    }
  }

  /**
   * Subtracts the given count from the current count for the given key.
   * If the key hasn't been seen before, it is assumed to have count 0, and
   * thus this  method will set its count to the negative of the given amount.
   * Negative increments are equivalent to calling <tt>incrementCount</tt>.
   * <p/>
   * To more conviently decrement the count by 1.0, use
   * {@link #decrementCount(Object)}.
   * <p/>
   * To set a count to a specifc value instead of decrementing it, use
   * {@link #setCount(Object,double)}.
   */
  public void decrementCount(E key, double count) {
    incrementCount(key, -count);
  }

  /**
   * Subtracts 1.0 from the count for the given key. If the key hasn't been
   * seen  before, it is assumed to have count 0, and thus this method will
   * set its count to -1.0.
   * <p/>
   * To decrement the count by a value other than 1.0, use
   * {@link #decrementCount(Object,double)}.
   * <p/>
   * To set a count to a specifc value instead of decrementing it, use
   * {@link #setCount(Object,double)}.
   */
  public void decrementCount(E key) {
    decrementCount(key, 1.0);
  }

  /**
   * Subtracts the given count from the current counts for each of the given keys.
   * If any of the keys haven't been seen before, they are assumed to have
   * count 0, and thus this method will set their counts to the negative of the given
   * amount. Negative increments are equivalent to calling <tt>incrementCount</tt>.
   * <p/>
   * To more conviniently decrement the counts of a collection of objects by
   * 1.0, use {@link #decrementCounts(Collection)}.
   * <p/>
   * To set the counts of a collection of objects to a specific value instead
   * of decrementing them, use {@link #setCounts(Collection,double)}.
   */
  public void decrementCounts(Collection<E> keys, double count) {
    incrementCounts(keys, -count);
  }

  /**
   * Subtracts 1.0 from the counts of each of the given keys. If any of the
   * keys haven't been seen before, they are assumed to have count 0, and thus
   * this method will set their counts to -1.0.
   * <p/>
   * To decrement the counts of a collection of object by a value other
   * than 1.0, use {@link #decrementCounts(Collection,double)}.
   * <p/>
   * To set the counts of a collection of objects  to a specifc value instead
   * of decrementing them, use  {@link #setCounts(Collection,double)}.
   */
  public void decrementCounts(Collection<E> keys) {
    decrementCounts(keys, 1.0);
  }

  /**
   * Adds the counts in the given Counter to the counts in this Counter.
   * <p/>
   * To copy the values from another Counter rather than adding them, use
   */
  public void addAll(GenericCounter<E> counter) {
    for (E key : counter.keySet()) {
      incrementCount(key, counter.getCount(key));
    }
  }

  /**
   * Calls incrementCount(key) on each key in the given collection.
   */
  public void addAll(Collection<E> collection) {
    for (E key : collection) {
      incrementCount(key);
    }
  }

  /**
   * Multiplies every count by the given multiplier.
   */
  public void multiplyBy(double multiplier) {
    for (E key : map.keySet()) {
      setCount(key, getCount(key) * multiplier);
    }
  }

  /**
   * Divides every count by the given divisor.
   */
  public void divideBy(double divisor) {
    for (E key : map.keySet()) {
      setCount(key, getCount(key) / divisor);
    }
  }

  /**
   * Divides every non-zero count by the count for the corresponding key in
   * the argument Counter.
   * Beware that this can give NaN values for zero counts in the argument 
   * counter!
   *
   * @param counter Entries in argument scale individual keys in this counter
   */
  public void divideBy(Counter<E> counter) {
    for (E key : map.keySet()) {
      setCount(key, getCount(key) / counter.getCount(key));
    }
  }

  /**
   * Subtracts the counts in the given Counter from the counts in this Counter.
   * <p/>
   * To copy the values from another Counter rather than subtracting them, use
   */
  public void subtractAll(GenericCounter<E> counter, boolean removeZeroKeys) {
    for (E key : counter.keySet()) {
      decrementCount(key, counter.getCount(key));
      if (removeZeroKeys && getCount(key)==0.0) remove(key);
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
  public MutableDouble remove(E key) {
    MutableDouble md = map.remove(key);
    if (md != null) {
      totalCount -= md.doubleValue();
    }
    return md;
  }

  /**
   * Removes all the given keys from this Counter.
   */
  public void removeAll(Collection<E> keys) {
    for (E key : keys) {
      remove(key);
    }
  }

  /**
   * Removes all counts from this Counter.
   */
  public void clear() {
    map.clear();
    totalCount = 0.0;
  }

  /**
   * Returns the number of keys stored in the counter.
   *
   * @return The number of keys
   */
  public int size() {
    return map.size();
  }

  public boolean isEmpty() {
    return (size() == 0);
  }

  public Set<E> keySet() {
    return map.keySet();
  }

  public Set<Map.Entry<E,MutableDouble>> entrySet() {
    return map.entrySet();
  }

  // OBJECT STUFF

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Counter)) {
      return false;
    }

    final Counter counter = (Counter) o;
    if (totalCount != counter.totalCount) {
      return false;
    }
    return map.equals(counter.map);
  }

  public int hashCode() {
    return map.hashCode();
  }

  public String toString() {
    return map.toString();
  }

  /**
   * Returns a string representation which includes no more than the
   * maxKeysToPrint elements with largest counts.
   *
   * @param maxKeysToPrint
   * @return partial string representation
   */
  public String toString(int maxKeysToPrint) {
    return asBinaryHeapPriorityQueue().toString(maxKeysToPrint);
  }

  /** Pretty print a Counter. This one has more flexibility in formatting,
   *  and doesn't sort the keys.
   */
  public String toString(NumberFormat nf, String preAppend, String postAppend,
                         String keyValSeparator, String itemSeparator) {
    StringBuilder sb = new StringBuilder();
    sb.append(preAppend);
    // List<E> list = new ArrayList<E>(map.keySet());
    //     try {
    //       Collections.sort(list); // see if it can be sorted
    //     } catch (Exception e) {
    //     }
    for (Iterator<E> iter = map.keySet().iterator(); iter.hasNext(); ) {
      E key = iter.next();
      MutableDouble d = map.get(key);
      sb.append(key);
      sb.append(keyValSeparator);
      sb.append(nf.format(d));
      if (iter.hasNext()) {
        sb.append(itemSeparator);
      }
    }
    sb.append(postAppend);
    return sb.toString();
  }


  /** Pretty print a Counter.  This version tries to sort the keys.
   */
  public String toString(NumberFormat nf) {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    List<E> list = new ArrayList<E>(map.keySet());
    try {
      Collections.sort((List) list); // see if it can be sorted
    } catch (Exception e) {
    }
    for (Iterator<E> iter = list.iterator(); iter.hasNext();) {
      E key = iter.next();
      MutableDouble d = map.get(key);
      sb.append(key);
      sb.append("=");
      sb.append(nf.format(d));
      if (iter.hasNext()) {
        sb.append(", ");
      }
    }
    sb.append("}");
    return sb.toString();
  }

  public Object clone() {
    return new Counter<E>(this);
  }

  // EXTRA CALCULATION METHODS

  /**
   * This has been de-deprecated in order to reduce compilation warnings, but
   * really you should create a {@link Distribution}
   * instead. 
   */
  public void normalize() {
    double total = totalCount(); // cache value since it will change as we normalize
    if (total==0.0 || Double.isNaN(total) || total==Double.NEGATIVE_INFINITY || total==Double.POSITIVE_INFINITY) throw new RuntimeException("Can't normalize with bad total: " + total);
    for (E key : map.keySet()) {
      setCount(key, getCount(key) / total);
    }
  }

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
      if (getCount(iter.next()) == 0.0) {
        iter.remove();
      }
    }
  }

  /**
   * Builds a priority queue whose elements are the counter's elements, and
   * whose priorities are those elements' counts in the counter.
   */
  public PriorityQueue<E> asPriorityQueue() {
    return asBinaryHeapPriorityQueue();
  }

  /**
   * Builds a priority queue whose elements are the counter's elements, and
   * whose priorities are those elements' counts in the counter.
   */
  public BinaryHeapPriorityQueue<E> asBinaryHeapPriorityQueue() {
    BinaryHeapPriorityQueue<E> pq = new BinaryHeapPriorityQueue<E>();
    for (Map.Entry<E, MutableDouble> entry : map.entrySet()) {
      pq.add(entry.getKey(), entry.getValue().doubleValue());
    }
    return pq;
  }

  /**
   * Finds and returns the largest count in this Counter.
   */
  public double max() {
    double max = Double.NEGATIVE_INFINITY;
    for (E key : map.keySet()) {
      max = Math.max(max, getCount(key));
    }
    return (max);
  }

  public double doubleMax() {
    return max();
  }

  /**
   * Finds and returns the smallest count in this Counter.
   */
  public double min() {
    double min = Double.POSITIVE_INFINITY;
    for (E key : map.keySet()) {
      min = Math.min(min, getCount(key));
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
  public E argmax(Comparator<E> tieBreaker) {
    double max = Double.NEGATIVE_INFINITY;
    E argmax = null;
    for (E key : map.keySet()) {
      double count = getCount(key);
      if (argmax == null || count > max || (count == max && tieBreaker.compare(key, argmax) < 0)) {
        max = count;
        argmax = key;
      }
    }
    return (argmax);
  }

  public void retainTop(int num) {
    int numToPurge = size()-num;
    if (numToPurge<=0) return;
//    System.err.println("purging...old size=" + size());
    int size = size();
    List<E> l = Counters.toSortedList(this);
    Collections.reverse(l);
    for (int i=0; i<numToPurge; i++) {
      remove(l.get(i));
    }
//    System.err.println("new size=" + size());
    
  }


  /**
   * Finds and returns the key in this Counter with the largest count.
   * Ties are broken according to the natural ordering of the objects.
   * This will prefer smaller numeric keys and lexicographically earlier
   * String keys. To use a different tie-breaking Comparator, use
   * {@link #argmax(Comparator)}. Returns null if this Counter is empty.
   */
  public E argmax() {
    return argmax(naturalComparator);
  }

  /**
   * Finds and returns the key in this Counter with the smallest count.
   * Ties are broken by comparing the objects using the given tie breaking
   * Comparator, favoring Objects that are sorted to the front. This is useful
   * if the keys are numeric and there is a bias to prefer smaller or larger
   * values, and can be useful in other circumstances where random tie-breaking
   * is not desirable. Returns null if this Counter is empty.
   */
  public E argmin(Comparator<E> tieBreaker) {
    double min = Double.POSITIVE_INFINITY;
    E argmin = null;

    for (E key : map.keySet()) {
      double count = getCount(key);
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
  public Set<E> keysAbove(double countThreshold) {
    Set<E> keys = new HashSet<E>();
    for (E key : map.keySet()) {
      if (getCount(key) >= countThreshold) {
        keys.add(key);
      }
    }
    return (keys);
  }

  /**
   * Returns the set of keys whose counts are at or below the given threshold.
   * This set may have 0 elements but will not be null.
   */
  public Set<E> keysBelow(double countThreshold) {
    Set<E> keys = new HashSet<E>();

    for (E key : map.keySet()) {
      if (getCount(key) <= countThreshold) {
        keys.add(key);
      }
    }
    return (keys);
  }

  /**
   * Returns the set of keys that have exactly the given count.
   * This set may have 0 elements but will not be null.
   */
  public Set<E> keysAt(double count) {
    Set<E> keys = new HashSet<E>();

    for (E key : map.keySet()) {
      if (getCount(key) == count) {
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
    public NaturalComparator() {}
    public String toString() { return "NaturalComparator"; }
    public int compare(Object o1, Object o2) {
      if (o1 instanceof Comparable) {
        return (((Comparable) o1).compareTo(o2));
      }
      return 0; // soft-fail
    }
  }

  /**
   * Returns the Counter over Strings specified by this String.
   * The String is normally the whole contents of a file.
   * Format is one entry per line, which is "String key\tdouble value".
   * @param s A String representation of a Counter, where the entries are
   *     one-per-line ('\n') and each line is key \t value
   * @return The Counter with String keys
   */
  public static Counter<String> valueOf(String s) {
    Counter<String> result = new Counter<String>();
    String[] lines = s.split("\n");
    for (String line : lines) {
      String[] fields = line.split("\t");
      result.setCount(fields[0], Double.parseDouble(fields[1]));
    }
    return result;
  }

  /**
   * For internal debugging purposes only.
   */
  public static void main(String[] args) throws Exception {
    Counter<String> c = new Counter<String>();
    c.setCount("p", 0);
    c.setCount("q", 2);
    System.out.println(c + " -> " + c.totalCount() + " should be {p=0.0, q=2.0} -> 2.0");
    c.incrementCount("p");
    System.out.println(c + " -> " + c.totalCount() + " should be {p=1.0, q=2.0} -> 3.0");
    c.incrementCount("p", 2.0);
    System.out.println(c.min() + " " + c.argmin() + " should be 2.0 q");
    c.setCount("w", -5);
    c.setCount("x", -2.5);
    List<String> biggestKeys = new ArrayList<String>(c.keySet());
    Collections.sort(biggestKeys, c.comparator(false, true));
    System.out.println(biggestKeys + " should be [w, p, x, q]");
    System.out.println(c + " should be {p=3.0, q=2.0, w=-5.0, x=-2.5}");
    if (args.length > 0) {
      // serialize to  file
      ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(args[0])));
      out.writeObject(c);
      out.close();

      // reconstitute
      ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(args[0])));
      c = (Counter<String>) in.readObject();
      in.close();
      System.out.println(c + " -> " + c.totalCount() + 
                         " should be same -> -2.5");
      //c.put("p",new Integer(3));
      System.out.println(c.min() + " " + c.argmin() + " should be -5 w");
      c.clear();
      System.out.println(c+" -> "+c.totalCount() + " should be {} -> 0");
    }
  }

}
