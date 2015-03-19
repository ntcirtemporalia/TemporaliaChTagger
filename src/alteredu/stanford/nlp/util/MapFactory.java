package alteredu.stanford.nlp.util;

import java.io.Serializable;
import java.util.*;

/**
 * A factory class for vending different sorts of Maps.
 *
 * @author Dan Klein (klein@cs.stanford.edu)
 * @author Kayur Patel (kdpatel@cs)
 */
abstract public class MapFactory implements Serializable {

  private MapFactory() {
  }

  private static final long serialVersionUID = 4529666940763477360L;

  public static final MapFactory HASH_MAP_FACTORY = new HashMapFactory();

  public static final MapFactory IDENTITY_HASH_MAP_FACTORY = new IdentityHashMapFactory();

  public static final MapFactory WEAK_HASH_MAP_FACTORY = new WeakHashMapFactory();

  public static final MapFactory TREE_MAP_FACTORY = new TreeMapFactory();

  public static final MapFactory ARRAY_MAP_FACTORY = new ArrayMapFactory();


  private static class HashMapFactory extends MapFactory {

    private static final long serialVersionUID = -9222344631596580863L;

    public Map newMap() {
      return new HashMap();
    }

    public Map newMap(int initCapacity) {
      return new HashMap(initCapacity);
    }

    public <K1, V1> Map<K1, V1> setMap(Map<K1,V1> map) {
      map = new HashMap<K1,V1>();
      return map;
    }

    public <K1, V1> Map<K1, V1> setMap(Map<K1,V1> map, int initCapacity) {
      map = new HashMap<K1,V1>(initCapacity);
      return map;
    }

  } // end class HashMapFactory

  private static class IdentityHashMapFactory extends MapFactory {

    private static final long serialVersionUID = -9222344631596580863L;

    public Map newMap() {
      return new IdentityHashMap();
    }

    public Map newMap(int initCapacity) {
      return new IdentityHashMap(initCapacity);
    }

    public <K1, V1> Map<K1, V1> setMap(Map<K1,V1> map) {
      map = new IdentityHashMap<K1,V1>();
      return map;
    }

    public <K1, V1> Map<K1, V1> setMap(Map<K1,V1> map, int initCapacity) {
      map = new IdentityHashMap<K1,V1>(initCapacity);
      return map;
    }

  } // end class IdentityHashMapFactory

  private static class WeakHashMapFactory extends MapFactory {

    private static final long serialVersionUID = 4790014244304941000L;

    public Map newMap() {
      return new WeakHashMap();
    }

    public Map newMap(int initCapacity) {
      return new WeakHashMap(initCapacity);
    }


    public <K1, V1> Map<K1, V1> setMap(Map<K1,V1> map) {
      map = new WeakHashMap<K1,V1>();
      return map;
    }

    public <K1, V1> Map<K1, V1> setMap(Map<K1,V1> map, int initCapacity) {
      map = new WeakHashMap<K1,V1>(initCapacity);
      return map;
    }

  } // end class WeakHashMapFactory

  private static class TreeMapFactory extends MapFactory {

    private static final long serialVersionUID = -9138736068025818670L;

    public Map newMap() {
      return new TreeMap();
    }   

    public Map newMap(int initCapacity) {
      return newMap();
    }


    public <K1, V1> Map<K1, V1> setMap(Map<K1,V1> map) {
      map = new TreeMap<K1,V1>();
      return map;
    }

    public <K1, V1> Map<K1, V1> setMap(Map<K1,V1> map, int initCapacity) {
      map = new TreeMap<K1,V1>();
      return map;
    }

  } // end class TreeMapFactory

  private static class ArrayMapFactory extends MapFactory {

    public Map newMap() {
      return new ArrayMap();
    }

    public Map newMap(int initCapacity) {
      return new ArrayMap(initCapacity);
    }

    public <K1, V1> Map<K1, V1> setMap(Map<K1, V1> map) {
      return new ArrayMap<K1,V1>();
    }

    public <K1, V1> Map<K1, V1> setMap(Map<K1,V1> map, int initCapacity) {
      map = new ArrayMap<K1,V1>(initCapacity);
      return map;
    }

  } // end class ArrayMapFactory


  /**
   * Returns a new non-parameterized map of a particular sort.
   */
  abstract public Map newMap();

  /**
   * Returns a new non-parameterized map of a particular sort with an initial capacity.
   * @param initCapacity initial capacity of the map
   */
  abstract public Map newMap(int initCapacity);

  /**
   * A method to get a parameterized (genericized) map out.
   *
   * @param map A type-parameterized {@link Map} argument
   * @return A {@link Map} with type-parameterization identical to that of
   *         the argument. 
   */
  abstract public <K1, V1> Map<K1, V1> setMap(Map<K1,V1> map);

  abstract public <K1, V1> Map<K1, V1> setMap(Map<K1,V1> map, int initCapacity);

}
