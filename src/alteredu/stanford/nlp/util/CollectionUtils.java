package alteredu.stanford.nlp.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Collection of useful static methods for working with Collections. Includes
 * methods to increment counts in maps and cast list/map elements to common
 * types.
 *
 * @author Joseph Smarr (jsmarr@stanford.edu)
 */
public class CollectionUtils {
  /**
   * Private constructor to prevent direct instantiation.
   */
  private CollectionUtils() {
  }

  /**
   * Adds the given delta to the Integer value stored for the given key in
   * the given Map. Returns whether the entry had to be created (i.e., it
   * wasn't in the map), in which case it's inserted with delta as the
   * initial value.
   *
   * @param map   Map from keys to Integer values representing key counts
   * @param key   key in map for Integer to increment
   * @param delta amount to change Integer value count by
   * @return whether a new entry for the given key was created in the
   *         process (i.e., if it wasn't in the map before).
   */
  public static boolean incrementCount(Map map, Object key, int delta) {
    boolean created = false;
    Integer count = (Integer) map.get(key);
    if (count == null) {
      count = new Integer(0);
      created = true;
    }
    map.put(key, new Integer(count.intValue() + delta));
    return (created);
  }

  /**
   * Increments the Integer count of the given key in the given Map by 1.
   *
   * @see #incrementCount(Map,Object,int)
   */
  public static boolean incrementCount(Map map, Object key) {
    return (incrementCount(map, key, 1));
  }

  // list casting methods

  /**
   * Returns <tt>(String)list.get(index)</tt>.
   */
  public static String getString(List list, int index) {
    return ((String) list.get(index));
  }

  /**
   * Returns <tt>(Integer)list.get(index)</tt>.
   */
  public static Integer getInteger(List list, int index) {
    return ((Integer) list.get(index));
  }

  /**
   * Returns <tt>((Integer)list.get(index)).intValue()</tt>.
   */
  public static int getInt(List list, int index) {
    return (getInteger(list, index).intValue());
  }

  /**
   * Returns <tt>(Double)list.get(index)</tt>.
   */
  public static Double getDouble(List list, int index) {
    return ((Double) list.get(index));
  }

  /**
   * Returns <tt>((Double)list.get(index)).doubleValue()</tt>.
   */
  public static double getdouble(List list, int index) {
    return (getDouble(list, index).doubleValue());
  }

  /**
   * Returns <tt>((Boolean)list.get(index)).booleanValue()</tt>.
   */
  public static boolean getBoolean(List list, int index) {
    return (((Boolean) list.get(index)).booleanValue());
  }

  // map casting methods

  /**
   * Returns <tt>(String)map.get(key)</tt>.
   */
  public static String getString(Map map, Object key) {
    return ((String) map.get(key));
  }

  /**
   * Returns <tt>(Integer)map.get(key)</tt>.
   */
  public static Integer getInteger(Map map, Object key) {
    return ((Integer) map.get(key));
  }

  /**
   * Returns <tt>((Integer)map.get(key)).intValue()</tt>.
   */
  public static int getInt(Map list, Object key) {
    return (getInteger(list, key).intValue());
  }

  /**
   * Returns <tt>(Double)map.get(key)</tt>.
   */
  public static Double getDouble(Map map, Object key) {
    return ((Double) map.get(key));
  }

  /**
   * Returns <tt>((Double)map.get(key)).doubleValue()</tt>.
   */
  public static double getdouble(Map map, Object key) {
    return (getDouble(map, key).doubleValue());
  }

  /**
   * Returns <tt>((Boolean)map.get(key)).booleanValue()</tt>.
   */
  public static boolean getBoolean(Map map, Object key) {
    return (((Boolean) map.get(key)).booleanValue());
  }

  // Utils for making collections out of arrays of primitive types.

  public static List asList(int[] a) {
    List result = new ArrayList();
    for (int i = 0; i < a.length; i++) {
      result.add(new Integer(a[i]));
    }
    return result;
  }

  public static List asList(double[] a) {
    List result = new ArrayList();
    for (int i = 0; i < a.length; i++) {
      result.add(new Double(a[i]));
    }
    return result;
  }

  /** Returns a new List containing the specified objects. */
  public static List asList(Object ... args) {
    List result = new ArrayList();
    for (int i = 0; i < args.length; i++) {
      result.add(args[i]);
    }
    return result;
  }

  /** Returns a new List containing the given object. */
  public static <T> List<T> makeList(T e) {
    List<T> s = new ArrayList<T>();
    s.add(e);
    return s;
  }

  /** Returns a new List containing the given objects. */
  public static <T> List<T> makeList(T e1, T e2) {
    List<T> s = new ArrayList<T>();
    s.add(e1);
    s.add(e2);
    return s;
  }

  /** Returns a new List containing the given objects. */
  public static <T> List<T> makeList(T e1, T e2, T e3) {
    List<T> s = new ArrayList<T>();
    s.add(e1);
    s.add(e2);
    s.add(e3);
    return s;
  }

  /** Returns a new Set containing all the objects in the specified array. */
  public static Set asSet(Object[] o) {
    return new HashSet(Arrays.asList(o));
  }

  // Utils for loading and saving Collections to/from text files

  /**
   * @param filename the path to the file to load the List from
   * @param c        the Class to instantiate each member of the List. Must have a String constructor.
   * @param cf
   * @return
   */
  public static Collection loadCollection(String filename, Class c, CollectionFactory cf) throws Exception {
    return loadCollection(new File(filename), c, cf);
  }
  /**
   * @param file     the file to load the List from
   * @param c        the Class to instantiate each member of the List. Must have a String constructor.
   * @return
   */
  public static Collection loadCollection(File file, Class c, CollectionFactory cf) throws Exception {
    Constructor m = c.getConstructor(new Class[]{Class.forName("java.lang.String")});
    Collection result = cf.newCollection();
    BufferedReader in = new BufferedReader(new FileReader(file));
    String line = in.readLine();
    while (line != null && line.length()>0) {
      try {
        Object o = m.newInstance(line);
        result.add(o);
      } catch (Exception e) {
        System.err.println("Couldn't build object from line: " + line);
        e.printStackTrace();
      }
      line = in.readLine();
    }
    in.close();
    return result;
  }

  public static Map getMapFromString(String s, Class keyClass, Class valueClass, MapFactory mapFactory) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
    Constructor keyC = keyClass.getConstructor(new Class[]{Class.forName("java.lang.String")});
    Constructor valueC = valueClass.getConstructor(new Class[]{Class.forName("java.lang.String")});
    if (s.charAt(0)!='{') throw new RuntimeException("");
    s = s.substring(1); // get rid of first brace
    String[] fields = s.split("\\s+");
    Map m = mapFactory.newMap();
    // populate m
    for (int i=0; i<fields.length; i++) {
//      System.err.println("Parsing " + fields[i]);
      fields[i] = fields[i].substring(0, fields[i].length()-1); // get rid of following comma or brace
      String[] a = fields[i].split("=");
      Object key = keyC.newInstance(a[0]);
      Object value = valueC.newInstance(a[1]);
      m.put(key, value);
    }
    return m;
  }
  /**
   * Checks whether a Collection contains a specified Object.  Object equality (==), rather than .equals(), is used.
   */
  public static boolean containsObject(Collection c, Object o) {
    for (Object o1 : c) {
      if (o == o1) {
        return true;
      }
    }
    return false;
  }

  /**
   * Removes the first occurrence in the list of the specified object, using object identity (==) not equality as the criterion
   * for object presence. If this list does not contain the element, it is unchanged.
   *
   * @param l The {@link List} from which to remove the object
   * @param o The object to be removed.
   * @return Whether or not the List was changed.
   */
  public static boolean removeObject(List l, Object o) {
    int i = 0;
    for(Object o1 : l) {
      if(o == o1) {
        l.remove(i);
        return true;
      }
      else
        i++;
    }
    return false;
  }


  /**
   * Returns the index of the first occurrence in the list of the specified object, using object identity (==) not equality as the criterion
   * for object presence. If this list does not contain the element, return -1.
   *
   * @param l The {@link List} to find the object in.
   * @param o The sought-after object. 
   * @return Whether or not the List was changed.
   */
  public static int getIndex(List l, Object o) {
    int i = 0;
    for(Object o1 : l) {
      if(o == o1)
        return i;
      else
        i++;
    }
    return -1;
  }

  /**
   * Samples without replacement from a collection
   * @param c The collection to be sampled from
   * @param n The number of samples to take
   * @return a new collection with the sample
   */
  public static <E> Collection<E> sampleWithoutReplacement(Collection<E> c, int n) {
    return sampleWithoutReplacement(c,n,new Random());
  }

  /**
   * Samples without replacement from a collection, using your own {@link Random} number generator
   * @param c The collection to be sampled from
   * @param n The number of samples to take
   * @param r the random number generator
   * @return a new collection with the sample
   */
  public static <E> Collection<E> sampleWithoutReplacement(Collection<E> c, int n, Random r) {
    List<E> copy = new ArrayList<E>(c.size());
    copy.addAll(c);
    Collection<E> result = new ArrayList<E>(n);
    for(int k = 0; k < n; k++) {
      double d = r.nextDouble();
      int x = (int) (d*copy.size());
      result.add(copy.remove(x));
    }
    return result;
  }


  /**
   * Samples with replacement from a collection
   * @param c The collection to be sampled from
   * @param n The number of samples to take
   * @return a new collection with the sample
   */
  public static <E> Collection<E> sampleWithReplacement(Collection<E> c, int n) {
    return sampleWithReplacement(c,n,new Random());
  }

  /**
   * Samples with replacement from a collection, using your own {@link Random} number generator
   * @param c The collection to be sampled from
   * @param n The number of samples to take
   * @param r the random number generator
   * @return a new collection with the sample
   */
  public static <E> Collection<E> sampleWithReplacement(Collection<E> c, int n, Random r) {
    List<E> copy = new ArrayList<E>(c.size());
    copy.addAll(c);
    Collection<E> result = new ArrayList<E>(n);
    for(int k = 0; k < n; k++) {
      double d = r.nextDouble();
      int x = (int) (d*copy.size());
      result.add(copy.get(x));
    }
    return result;
  }

  /**
   * Returns true iff l1 is a sublist of l (i.e., every member of l1 is in l, and for every e1 < e2 in l1, there is
   * an e1 < e2 occurrence in l).
   * @param l
   * @param l1
   * @return
   */
  public static boolean isSubList(List l1, List l) {
    Iterator it = l.iterator();
    Iterator it1 = l1.iterator();
    while(it1.hasNext()) {
      Object o1 = it1.next();
      if(! it.hasNext())
        return false;
      Object o = it.next();
      while((o == null && ! (o1 == null)) || ! o.equals(o1)) {
        if(! it.hasNext())
          return false;
        o = it.next();
      }
    }
    return true;
  }

  public static String toVerticalString(Map m) {
    StringBuilder b = new StringBuilder();
    Set<Map.Entry> entries = m.entrySet();
    for (Map.Entry e : entries) {
      b.append(e.getKey() + "=" + e.getValue() + "\n");
    }
    return b.toString();
  }

  public static void main(String[] args) {
    Collection<String> c = new ArrayList<String>();
    c.add("a");
    c.add("b");
    c.add("c");
    c.add("d");
    c.add("e");
    c.add("f");
    c.add("g");
    c.add("h");
    c.add("i");
    for(int i = 0; i < 10; i++)
      System.out.println(sampleWithoutReplacement(c,4));
  }

}
