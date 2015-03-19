package alteredu.stanford.nlp.stats;

import alteredu.stanford.nlp.util.MapFactory;
import alteredu.stanford.nlp.util.Pair;
import alteredu.stanford.nlp.util.StringUtils;
import alteredu.stanford.nlp.math.ArrayMath;

import java.util.*;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * A class representing a mapping between pairs of typed objects and double values.
 * 
 * @author Teg Grenager
 */
public class TwoDimensionalCounter<K1, K2> implements Serializable {
  
  public static final long serialVersionUID = 1L;

  // the outermost Map
  private Map<K1, Counter<K2>> map;

  // the total of all counts
  private double total;

  // the MapFactory used to make new Maps/Counters
  private MapFactory mf;

  /**
   * @param o
   * @return the inner Counter associated with key o
   */
  public Counter<K2> getCounter(K1 o) {
    Counter<K2> c = map.get(o);
    if (c == null) {
      c = new Counter<K2>(mf);
      map.put(o, c);
    }
    return c;
  }

  public Set<Map.Entry<K1,Counter<K2>>> entrySet(){
    return map.entrySet();
  }

  /**
   * @return total number of entries (key pairs)
   */
  public int size() {
    int result = 0;
    for (Iterator iter = firstKeySet().iterator(); iter.hasNext();) {
      Object o = (Object) iter.next();
      Counter c = (Counter) map.get(o);
      result += c.size();
    }
    return result;
  }

  public boolean containsKey(K1 o1, K2 o2) {
    if (!map.containsKey(o1)) return false;
    Counter c = map.get(o1);
    if (!c.containsKey(o2)) return false;
    return true;
  }

  /**
   * @param o1
   * @param o2
   */
  public void incrementCount(K1 o1, K2 o2) {
    incrementCount(o1, o2, 1.0);
  }

  /**
   * @param o1
   * @param o2
   * @param count
   */
  public void incrementCount(K1 o1, K2 o2, double count) {
    Counter c = getCounter(o1);
    c.incrementCount(o2, count);
    total += count;
  }

  /**
   * @param o1
   * @param o2
   * @param count
   */
  public void setCount(K1 o1, K2 o2, double count) {
    Counter c = getCounter(o1);
    double oldCount = getCount(o1, o2);
    total -= oldCount;
    c.setCount(o2, count);
    total += count;
  }

  /**
   * @param o1
   * @param o2
   * @return
   */
  public double getCount(K1 o1, K2 o2) {
    Counter c = getCounter(o1);
    return c.getCount(o2);
  }

  /**
   * Takes linear time.
   * 
   * @return
   */
  public double totalCount() {
    return total;
  }

  /**
   * @return
   */
  public double totalCount(K1 k1) {
    Counter c = getCounter(k1);
    return c.totalCount();
  }

  public Set<K1> firstKeySet() {
    return map.keySet();
  }

  public Counter<K2> setCounter(K1 o, Counter<K2> c) {
    Counter<K2> old = getCounter(o);
    total -= old.totalCount();
    map.put(o, c);
    total += c.totalCount();
    return old;
  }

  /**
   * Produces a new ConditionalCounter.
   * 
   * @param cc
   * @return a new ConditionalCounter, where order of indices is reversed
   */
  public static TwoDimensionalCounter reverseIndexOrder(TwoDimensionalCounter cc) {
    TwoDimensionalCounter result = new TwoDimensionalCounter(cc.mf);
    for (Iterator iter1 = cc.firstKeySet().iterator(); iter1.hasNext();) {
      Object key1 = (Object) iter1.next();
      Counter c = cc.getCounter(key1);
      for (Iterator iter2 = c.keySet().iterator(); iter2.hasNext();) {
        Object key2 = (Object) iter2.next();
        double count = c.getCount(key2);
        result.setCount(key2, key1, count);
      }
    }
    return result;
  }

  public String toString() {
    StringBuffer buff = new StringBuffer();
    for (Iterator<K1> iter1 = firstKeySet().iterator(); iter1.hasNext();) {
      K1 key1 = iter1.next();
      Counter<K2> c = getCounter(key1);
      for (Iterator<K2> iter2 = c.keySet().iterator(); iter2.hasNext(); ) {
        K2 key2 = iter2.next();
        double count = c.getCount(key2);
        buff.append(key1 + " " + key2 + " " + count + "\n");
      }
    }
    return buff.toString();
  }

  public String toDatFileString() {
    StringBuilder buff = new StringBuilder();
    for (K1 key1 : map.keySet()) {
      Counter<K2> c = getCounter(key1);
      for (K2 key2 : c.keySet()) {
        double score = c.getCount(key2);
        buff.append(key1 + " " + key2 + " " + score + "\n");
      }
    }
    return buff.toString();
  }

  public String toMatrixString(int cellSize) {
    List<K1> firstKeys = new ArrayList<K1>(firstKeySet());
    List<K2> secondKeys = new ArrayList<K2>(secondKeySet());
    double[][] counts = toMatrix(firstKeys, secondKeys);
    return ArrayMath.toString(counts, cellSize, firstKeys.toArray(), secondKeys.toArray(), new DecimalFormat(), true);
  }

  /**
   * Given an ordering of the first (row) and second (column) keys, will produce a double matrix.
   * 
   * @param firstKeys
   * @param secondKeys
   * @return
   */
  public double[][] toMatrix(List<K1> firstKeys, List<K2> secondKeys) {
    double[][] counts = new double[firstKeys.size()][secondKeys.size()];
    for (int i = 0; i < firstKeys.size(); i++) {
      for (int j = 0; j < secondKeys.size(); j++) {
        counts[i][j] = getCount(firstKeys.get(i), secondKeys.get(j));
      }
    }
    return counts;
  }

  public String toCSVString(NumberFormat nf) {
    List rowLabels = new ArrayList(firstKeySet());
    Collections.sort(rowLabels);
    List colLabels = new ArrayList(secondKeySet());
    Collections.sort(colLabels);
    StringBuilder b = new StringBuilder();
    String[] headerRow = new String[colLabels.size() + 1];
    headerRow[0] = "";
    for (int j = 0; j < colLabels.size(); j++) {
      headerRow[j + 1] = colLabels.get(j).toString();
    }
    b.append(StringUtils.toCSVString(headerRow) + "\n");
    for (int i = 0; i < rowLabels.size(); i++) {
      String[] row = new String[colLabels.size() + 1];
      K1 rowLabel = (K1) rowLabels.get(i);
      row[0] = rowLabel.toString();
      for (int j = 0; j < colLabels.size(); j++) {
        K2 colLabel = (K2) colLabels.get(j);
        row[j + 1] = nf.format(getCount(rowLabel, colLabel));
      }
      b.append(StringUtils.toCSVString(row) + "\n");
    }
    return b.toString();
  }

  public Set<K2> secondKeySet() {
    Set<K2> result = new HashSet<K2>();
    for (K1 k1 : firstKeySet()) {
      for (K2 k2 : getCounter(k1).keySet()) {
        result.add(k2);
      }
    }
    return result;
  }

  public boolean isEmpty() {
    return map.isEmpty();
  }

  public Counter<Pair<K1, K2>> flatten() {
    Counter<Pair<K1, K2>> result = new Counter<Pair<K1, K2>>();
    for (K1 key1 : firstKeySet()) {
      Counter<K2> inner = getCounter(key1);
      for (K2 key2 : inner.keySet()) {
        result.setCount(new Pair<K1, K2>(key1, key2), inner.getCount(key2));
      }
    }
    return result;
  }

  public void addAll(TwoDimensionalCounter<K1, K2> c) {
    for (K1 key : c.firstKeySet()) {
      Counter<K2> inner = c.getCounter(key);
      Counter<K2> myInner = getCounter(key);
      myInner.addAll(inner);
      total += inner.totalCount();
    }
  }

  public void subtractAll(TwoDimensionalCounter<K1, K2> c, boolean removeKeys) {
    for (K1 key : c.firstKeySet()) {
      Counter<K2> inner = c.getCounter(key);
      Counter<K2> myInner = getCounter(key);
      myInner.subtractAll(inner, removeKeys);
      total -= inner.totalCount();
    }
  }

  public MapFactory getMapFactory() {
    return mf;
  }

  public TwoDimensionalCounter() {
    this(MapFactory.HASH_MAP_FACTORY);
  }

  public TwoDimensionalCounter(MapFactory factory) {
    mf = factory;
    map = mf.newMap();
    total = 0.0;
  }

  public static void main(String[] args) {
    TwoDimensionalCounter cc = new TwoDimensionalCounter();
    cc.setCount("a", "c", 1.0);
    cc.setCount("b", "c", 1.0);
    cc.setCount("a", "d", 1.0);
    cc.setCount("a", "d", -1.0);
    cc.setCount("b", "d", 1.0);
    System.out.println(cc);
    cc.incrementCount("b", "d", 1.0);
    System.out.println(cc);
    TwoDimensionalCounter cc2 = TwoDimensionalCounter.reverseIndexOrder(cc);
    System.out.println(cc2);
  }

}
