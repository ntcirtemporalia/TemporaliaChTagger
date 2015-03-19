package alteredu.stanford.nlp.stats;

import alteredu.stanford.nlp.util.PriorityQueue;
import alteredu.stanford.nlp.util.BinaryHeapPriorityQueue;
import alteredu.stanford.nlp.util.Index;
import alteredu.stanford.nlp.util.Sets;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.*;


/**
 * Static methods for operating on {@link Counter}s.
 *
 * @author Galen Andrew (galand@cs.stanford.edu)
 * @author Jeff Michels (jmichels@stanford.edu)
 */
public class Counters {

  private Counters() {} // only static methods

  /**
   * Returns a Counter that is the union of the two Counters passed in (counts are added).
   *
   * @param c1
   * @param c2
   * @return A Counter that is the union of the two Counters passed in (counts are added).
   */
  public static <E> Counter<E> union(GenericCounter<E> c1, GenericCounter<E> c2) {
    Counter<E> result = new Counter<E>();
    result.addAll(c1);
    result.addAll(c2);
    return result;
  }

  /**
   * Returns a counter that is the intersection of c1 and c2.  If both c1 and c2 contain a
   * key, the min of the two counts is used.
   *
   * @param c1
   * @param c2
   * @return A counter that is the intersection of c1 and c2
   */
  public static <E> Counter<E> intersection(GenericCounter<E> c1, GenericCounter<E> c2) {
    Counter<E> result = new Counter<E>();
    double count1, count2, minCount;
    for (E key : (Set<E>) Sets.union(c1.keySet(), c2.keySet())) {
      count1 = c1.getCount(key);
      count2 = c2.getCount(key);
      minCount = (count1 < count2 ? count1 : count2);
      if (minCount > 0) {
        result.setCount(key, minCount);
      }
    }
    return result;
  }

  /**
   * Returns the Jaccard Coefficient of the two counters. Calculated as
   * |c1 intersect c2| / ( |c1| + |c2| - |c1 intersect c2|
   *
   * @param c1
   * @param c2
   * @return The Jaccard Coefficient of the two counters
   */
  public static <E> double jaccardCoefficient(GenericCounter<E> c1, GenericCounter<E> c2) {
    double count1, count2, minCount = 0, maxCount = 0;
    for (E key : (Set<E>) Sets.union(c1.keySet(), c2.keySet())) {
      count1 = c1.getCount(key);
      count2 = c2.getCount(key);
      minCount += (count1 < count2 ? count1 : count2);
      maxCount += (count1 > count2 ? count1 : count2);
    }
    return minCount / maxCount;
  }

  /**
   * Returns the product of c1 and c2.
   *
   * @param c1
   * @param c2
   * @return The product of c1 and c2.
   */
  public static <E> Counter<E> product(GenericCounter<E> c1, GenericCounter<E> c2) {
    Counter<E> result = new Counter<E>();
    for (E key : (Set<E>) Sets.intersection(c1.keySet(), c2.keySet())) {
      result.setCount(key, c1.getCount(key) * c2.getCount(key));
    }
    return result;
  }

  /**
   * Returns the product of c1 and c2.
   *
   * @param c1
   * @param c2
   * @return The product of c1 and c2.
   */
  public static <E> double dotProduct(GenericCounter<E> c1, GenericCounter<E> c2) {
    double dotProd = 0.0;
    for (E key : c1.keySet()) {
      double count1 = c1.getCount(key);
      if (Double.isNaN(count1) || Double.isInfinite(count1)) throw new RuntimeException();
      if (count1 != 0.0) {
        double count2 = c2.getCount(key);
        if (Double.isNaN(count2) || Double.isInfinite(count2)) throw new RuntimeException();
        if (count2 != 0.0) {
          // this is the inner product
          dotProd += (count1 * count2);
        }
      }
    }
    return dotProd;
  }


  /**
   * Returns |c1 - c2|.
   *
   * @param c1
   * @param c2
   * @return The difference between sets c1 and c2.
   */
  public static <E> Counter<E> absoluteDifference(GenericCounter<E> c1, GenericCounter<E> c2) {
    Counter<E> result = new Counter<E>();
    double newCount;
    for (E key : (Set<E>) Sets.union(c1.keySet(), c2.keySet())) {
      newCount = Math.abs(c1.getCount(key) - c2.getCount(key));
      if (newCount > 0) {
        result.setCount(key, newCount);
      }
    }
    return result;
  }

  /**
   * Returns c1 divided by c2.  Note that this can create NaN if c1 has non-zero counts for keys that
   * c2 has zero counts.
   *
   * @param c1
   * @param c2
   * @return c1 divided by c2.
   */
  public static <E> Counter<E> division(GenericCounter<E> c1, GenericCounter<E> c2) {
    Counter<E> result = new Counter<E>();
    for (E key : (Set<E>) Sets.union(c1.keySet(), c2.keySet())) {
      result.setCount(key, c1.getCount(key) / c2.getCount(key));
    }
    return result;
  }

  /**
   * Calculates the entropy of the given counter (in bits). This method internally
   * uses normalized counts (so they sum to one), but the value returned is
   * meaningless if some of the counts are negative.
   *
   * @return The entropy of the given counter (in bits)
   */
  public static <E> double entropy(GenericCounter<E> c) {
    double entropy = 0.0;
    double total = c.totalDoubleCount();
    for (E key : c.keySet()) {
      double count = c.getCount(key);
      if (count == 0) {
        continue; // 0.0 doesn't add entropy but may cause -Inf
      }
      count /= total; // use normalized count
      entropy -= count * (Math.log(count) / Math.log(2.0));
    }
    return (entropy);
  }

  /**
   * Note that this implementation doesn't normalize the "from" Counter.
   * It does, however, normalize the "to" Counter.
   * Result is meaningless if any of the counts are negative.
   *
   * @return The cross entropy of H(from, to)
   */
  public static <E> double crossEntropy(GenericCounter<E> from, GenericCounter<E> to) {
    double tot2 = to.totalDoubleCount();
    double result = 0.0;
    double log2 = Math.log(2.0);
    for (E key : from.keySet()) {
      double count1 = from.getCount(key);
      if (count1 == 0.0) {
        continue;
      }
      double count2 = to.getCount(key);
      double logFract = Math.log(count2 / tot2);
      if (logFract == Double.NEGATIVE_INFINITY) {
        return Double.NEGATIVE_INFINITY; // can't recover
      }
      result += count1 * (logFract / log2); // express it in log base 2
    }
    return result;
  }

  /**
   * Note that this implementation doesn't normalize the "from" Counter.
   * Result is meaningless if any of the counts are negative.
   *
   * @return The cross entropy of H(from, to)
   */
  public static <E> double crossEntropy(GenericCounter<E> from, Counter<E> to) {
    double result = 0.0;
    double log2 = Math.log(2.0);
    for (E key : from.keySet()) {
      double count1 = from.getCount(key);
      if (count1 == 0.0) {
        continue;
      }
      double prob = to.getCount(key);
      double logFract = Math.log(prob);
      if (logFract == Double.NEGATIVE_INFINITY) {
        return Double.NEGATIVE_INFINITY; // can't recover
      }
      result += count1 * (logFract / log2); // express it in log base 2
    }
    return result;
  }

  /**
   * Calculates the KL divergence between the two counters.
   * That is, it calculates KL(from || to). This method internally
   * uses normalized counts (so they sum to one), but the value returned is
   * meaningless if any of the counts are negative.
   * In other words, how well can c1 be represented by c2.
   * if there is some value in c1 that gets zero prob in c2, then return positive infinity.
   *
   * @param from
   * @param to
   * @return The KL divergence between the distributions
   */
  public static <E> double klDivergence(GenericCounter<E> from, GenericCounter<E> to) {
    double result = 0.0;
    double tot = (from.totalDoubleCount());
    double tot2 = (to.totalDoubleCount());
    // System.out.println("tot is " + tot + " tot2 is " + tot2);
    double log2 = Math.log(2.0);
    for (E key : from.keySet()) {
      double num = (from.getCount(key));
      if (num == 0) {
        continue;
      }
      num /= tot;
      double num2 = (to.getCount(key));
      num2 /= tot2;
      // System.out.println("num is " + num + " num2 is " + num2);
      double logFract = Math.log(num / num2);
      if (logFract == Double.NEGATIVE_INFINITY) {
        return Double.NEGATIVE_INFINITY; // can't recover
      }
      result += num * (logFract / log2); // express it in log base 2
    }
    return result;
  }

  /**
   * Calculates the Jensen-Shannon divergence between the two counters.
   * That is, it calculates 1/2 [KL(c1 || avg(c1,c2)) + KL(c2 || avg(c1,c2))] .
   *
   * @param c1
   * @param c2
   * @return The Jensen-Shannon divergence between the distributions
   */
  public static <E> double jensenShannonDivergence(GenericCounter<E> c1, GenericCounter<E> c2) {
    Counter<E> average = average(c1, c2);
    double kl1 = klDivergence(c1, average);
    double kl2 = klDivergence(c2, average);
    return (kl1 + kl2) / 2.0;
  }

  /**
   * Calculates the skew divergence between the two counters.
   * That is, it calculates KL(c1 || (c2*skew + c1*(1-skew))) .
   * In other words, how well can c1 be represented by a "smoothed" c2.
   *
   * @param c1
   * @param c2
   * @param skew
   * @return The skew divergence between the distributions
   */
  public static <E> double skewDivergence(GenericCounter<E> c1, GenericCounter<E> c2, double skew) {
    Counter<E> average = linearCombination(c2, skew, c1, (1.0 - skew));
    return klDivergence(c1, average);
  }


  /** L2 normalize a counter.
   *
   * @param c the {@link GenericCounter} to be L2 normalized.
   */
  public static <E> Counter<E> L2Normalize(GenericCounter<E> c) {
    double total = 0.0;
    for (E key : c.keySet()) {
      double count2 = c.getCount(key);
      if (count2 != 0.0) {
        total += (count2 * count2);
      }
    }
    return scale(c,1.0/Math.sqrt(total));
  }

  public static <E> double cosine(GenericCounter<E> c1, GenericCounter<E> c2) {
    double dotProd = 0.0;
    double lsq1 = 0.0;
    double lsq2 = 0.0;
    for (E key : c1.keySet()) {
      double count1 = c1.getCount(key);
      if (count1 != 0.0) {
        lsq1 += (count1 * count1);
        double count2 = c2.getCount(key);
        if (count2 != 0.0) {
          // this is the inner product
          dotProd += (count1 * count2);
        }
      }
    }
    for (E key : c2.keySet()) {
      double count2 = c2.getCount(key);
      if (count2 != 0.0) {
        lsq2 += (count2 * count2);
      }
    }
    if (lsq1 != 0.0 && lsq2 != 0.0) {
      double denom = (Math.sqrt(lsq1) * Math.sqrt(lsq2));
      return dotProd / denom;
    } else {
      return 0.0;
    }
  }

  /**
   * Returns a new Counter with counts averaged from the two given Counters.
   * The average Counter will contain the union of keys in both
   * source Counters, and each count will be the average of the two source
   * counts for that key, where as usual a missing count in one Counter
   * is treated as count 0.
   *
   * @return A new counter with counts that are the mean of the resp. counts
   *         in the given counters.
   */
  public static <E> Counter<E> average(GenericCounter<E> c1, GenericCounter<E> c2) {
    Counter<E> average = new Counter<E>();
    Set<E> allKeys = new HashSet<E>(c1.keySet());
    allKeys.addAll(c2.keySet());
    for (E key : allKeys) {
      average.setCount(key, (c1.getCount(key) + c2.getCount(key)) * 0.5);
    }
    return (average);
  }


  /**
   * Returns a Counter which is a weighted average of c1 and c2. Counts from c1
   * are weighted with weight w1 and counts from c2 are weighted with w2.
   */
  public static <E> Counter<E> linearCombination(GenericCounter<E> c1, double w1, GenericCounter<E> c2, double w2) {
    Counter<E> result = new Counter<E>();
    for (E o : c1.keySet()) {
      result.incrementCount(o, c1.getCount(o) * w1);
    }
    for (E o : c2.keySet()) {
      result.incrementCount(o, c2.getCount(o) * w2);
    }
    return result;
  }

  public static <E> Counter<E> perturbCounts(GenericCounter<E> c, Random random, double p) {
    Counter<E> result = new Counter<E>(c.getMapFactory());
    for (E key : c.keySet()) {
      double count = c.getCount(key);
      double noise = -Math.log(1.0 - random.nextDouble()); // inverse of CDF for exponential distribution
      //      System.err.println("noise=" + noise);
      double perturbedCount = count + noise * p;
      result.setCount(key, perturbedCount);
    }
    return result;
  }

  public static <E> Counter<E> createCounterFromList(List<E> l) {
    return createCounterFromCollection(l);
  }

  public static <E> Counter<E> createCounterFromCollection(Collection<E> l) {
    Counter<E> result = new Counter<E>();
    for (E o : l) {
      result.incrementCount(o);
    }
    return result;
  }

  /**
   *  A List of the keys in c, sorted from highest count to lowest.
   *
   * @param c
   * @return A List of the keys in c, sorted from highest count to lowest.
   */
  public static <E> List<E> toSortedList(GenericCounter<E> c) {
    Comparator<E> comp = c.comparator();
    List<E> l = new ArrayList<E>(c.keySet());
    Collections.sort(l, comp);
    Collections.reverse(l);
    return l;
  }

  /**
   * Returns a {@link edu.stanford.nlp.util.PriorityQueue} of the c where the score of the object is its priority.
   */
  public static <E> alteredu.stanford.nlp.util.PriorityQueue toPriorityQueue(GenericCounter<E> c) {
	  alteredu.stanford.nlp.util.PriorityQueue queue = new BinaryHeapPriorityQueue();
    for (E key : c.keySet()) {
      double count = c.getCount(key);
      queue.add(key, count);
    }
    return queue;
  }

  /**
   * Great for debugging.
   *
   * @param a
   * @param b
   */
  public static <E> void printCounterComparison(GenericCounter<E> a, GenericCounter<E> b) {
    printCounterComparison(a, b, System.err);
  }

  /**
   * Great for debugging.
   *
   * @param a
   * @param b
   */
  public static <E> void printCounterComparison(GenericCounter<E> a, GenericCounter<E> b, PrintStream out) {
    if (a.equals(b)) {
      out.println("Counters are equal.");
      return;
    }
    for (E key : a.keySet()) {
      double aCount = a.getCount(key);
      double bCount = b.getCount(key);
      if (Math.abs(aCount - bCount) > 1e-5) {
        out.println("Counters differ on key " + key + "\t" + a.getCountAsString(key) + " vs. " + b.getCountAsString(key));
      }
    }
    // left overs
    Set<E> rest = new HashSet(b.keySet());
    rest.removeAll(a.keySet());

    for (E key : rest) {
      double aCount = a.getCount(key);
      double bCount = b.getCount(key);
      if (Math.abs(aCount - bCount) > 1e-5) {
        out.println("Counters differ on key " + key + "\t" + a.getCountAsString(key) + " vs. " + b.getCountAsString(key));
      }
    }
  }

  public static <E> Counter<Double> getCountCounts(GenericCounter<E> c) {
    Counter<Double> result = new Counter<Double>();
    for (E o : c.keySet()) {
      double count = c.getCount(o);
      result.incrementCount(new Double(count));
    }
    return result;
  }

  /**
   * Returns a new Counter which is scaled by the given scale factor.
   */
  public static <E> Counter<E> scale(GenericCounter<E> c, double s) {
    Counter<E> scaled = new Counter<E>(c.getMapFactory());
    for (E key : c.keySet()) {
      scaled.setCount(key, c.getCount(key) * s);
    }
    return scaled;
  }

  public static <E> void printCounterSortedByKeys(GenericCounter<E> c) {
    List keyList = new ArrayList(c.keySet());
    Collections.sort(keyList);
    for (Iterator<E> i = keyList.iterator(); i.hasNext();) {
      E o = (E) i.next();
      System.out.println(o + ":" + c.getCountAsString(o));
    }
  }

  /**
   * Loads a Counter from a text file. File must have the format of one key/count pair per line,
   * separated by whitespace.
   *
   * @param filename the path to the file to load the Counter from
   * @param c        the Class to instantiate each member of the set. Must have a String constructor.
   * @return The counter loaded from the file.
   */
  public static <E> Counter<E> loadCounter(String filename, Class c) throws Exception {
    Counter<E> counter = new Counter<E>();
    loadIntoCounter(filename, c, counter);
    return counter;
  }

  /**
   * Loads a Counter from a text file. File must have the format of one key/count pair per line,
   * separated by whitespace.
   *
   * @param filename the path to the file to load the Counter from
   * @param c        the Class to instantiate each member of the set. Must have a String constructor.
   * @return The counter loaded from the file.
   */
  public static IntCounter loadIntCounter(String filename, Class c) throws Exception {
    IntCounter counter = new IntCounter();
    loadIntoCounter(filename, c, counter);
    return counter;
  }

  /**
   * Loads a file into an GenericCounter.
   */
  private static <E> void loadIntoCounter(String filename, Class c, GenericCounter<E> counter) throws Exception {
    Constructor m = c.getConstructor(new Class[]{Class.forName("java.lang.String")});
    Counter<E> result = new Counter<E>();
    BufferedReader in = new BufferedReader(new FileReader(filename));
    String line = in.readLine();
    while (line != null && line.length()>0) {
      //      System.err.println("Got line: " + line);
      String[] fields = line.split("\\p{Space}+"); // split on whitespace
      //      System.err.println("Got fields " + Arrays.asList(fields));
      E o = (E) m.newInstance((E[]) new String[]{fields[0]});
      counter.setCount(o, fields[1]);
      line = in.readLine();
    }
    in.close();
  }

  /**
   * Saves a Counter to a text file. Counter written as one key/count pair per line,
   * separated by whitespace.
   *
   * @param c
   * @param filename
   * @throws IOException
   */
  public static <E> void saveCounter(GenericCounter<E> c, String filename) throws IOException {
    PrintWriter out = new PrintWriter(new FileWriter(filename));
    for (E key : c.keySet()) {
      out.println(key + " " + c.getCountAsString(key));
    }
    out.close();
  }

  public static void serializeCounter(GenericCounter c, String filename) throws IOException {
      // serialize to  file
      ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));
      out.writeObject(c);
      out.close();
  }

  public static Counter deserializeCounter(String filename) throws Exception {
      // reconstitute
      ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(filename)));
      Counter c = (Counter) in.readObject();
      in.close();
      return c;
  }
  
  public static <E> List<E> sortedKeys(Counter<E> x) {
    List<E> keys = new ArrayList<E>(x.keySet());
    Collections.sort(keys, x.comparator(false));
    return keys;
  }

  public static <E> String toBiggestValuesFirstString(Counter<E> c) {
    return toPriorityQueue(c).toString();
  }

  public static <E> String toBiggestValuesFirstString(Counter<E> c, int k) {
    PriorityQueue pq = toPriorityQueue(c);
    PriorityQueue largestK = new BinaryHeapPriorityQueue();
    while (largestK.size() < k && ((Iterator)pq).hasNext()) {
      double firstScore = pq.getPriority(pq.getFirst());
      Object first = pq.removeFirst();
      largestK.changePriority(first, firstScore);
    }
    return largestK.toString();
  }

  public static String toVerticalString(Counter c) {
    return toVerticalString(c, Integer.MAX_VALUE);
  }

  public static String toVerticalString(Counter c, int k) {
    PriorityQueue q = Counters.toPriorityQueue(c);
    List sortedKeys = q.toSortedList();
    StringBuffer sb = new StringBuffer();
    int i=0;
    for (Iterator keyI = sortedKeys.iterator(); keyI.hasNext() && i<k; i++) {
      Object key = keyI.next();
      sb.append(key);
      sb.append("\t");
      sb.append(q.getPriority(key));
      if (keyI.hasNext()) {
        sb.append("\n");
      }
    }
    return sb.toString();
  }

  /**
   *
   * @param c
   * @param restriction
   * @return Returns the maximum element of c that is within the restriction Collection
   */
  public static <E> Object restrictedArgMax(Counter<E> c, Collection<E> restriction) {
    Object maxKey = null;
    double max = Double.NEGATIVE_INFINITY;
    for (E key : restriction) {
      double count = c.getCount(key);
      if (count > max) {
        max = count;
        maxKey = key;
      }
    }
    return maxKey;
  }

  public static <T> Counter<T> toCounter(double[] counts, Index<T> index) {
    if (index.size()<counts.length) throw new IllegalArgumentException("Index not large enough to name all the array elements!");
    Counter<T> c = new Counter<T>();
    for (int i=0; i<counts.length; i++) {
      if (counts[i]!=0.0) c.setCount(index.get(i), counts[i]);
    }
    return c;
  }

  /**
   * Creates a new TwoDimensionalCounter where all the counts are scaled by d.
   * Internally, uses Counters.scale();
   *
   * @param c
   * @param d
   * @return The TwoDimensionalCounter
   */
  public static <T1, T2> TwoDimensionalCounter<T1, T2> scale(TwoDimensionalCounter<T1, T2> c, double d) {
    TwoDimensionalCounter<T1, T2> result = new TwoDimensionalCounter<T1, T2>(c.getMapFactory());
    for (T1 key : c.firstKeySet()) {
      Counter<T2> ctr = c.getCounter(key);
      result.setCounter(key, scale(ctr, d));
    }
    return result;
  }

  /**
   * Assumes c is normalized.
   * @param c
   * @param rand
   * @return A sample from c
   */
  public static <T> T sample(Counter<T> c, Random rand) {
    double r = rand.nextDouble();
    double total = 0.0;
    for (T t : c.keySet()) { // arbitrary ordering
      total += c.getCount(t);
      if (total>=r) return t;
    }
    // only chance of reaching here is if c isn't properly normalized, or if double math makes total<1.0
    return c.keySet().iterator().next();
  }

  public static <T> Counter<T> pow(Counter<T> c, double temp) {
    Counter<T> d = new Counter<T>();
    for (T t : c.keySet()) {
      d.setCount(t, Math.pow(c.getCount(t), temp));
    }
    return d;
  }

  public static <T> Counter<T> exp(Counter<T> c) {
    Counter<T> d = new Counter<T>();
    for (T t : c.keySet()) {
      d.setCount(t, Math.exp(c.getCount(t)));
    }
    return d;
  }
}
