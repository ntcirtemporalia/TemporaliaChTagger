package alteredu.stanford.nlp.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * Factory for vending Collections.  It's a class instead of an interface because I guessed that it'd primarily be used for its inner classes.
 *
 * @author Dan Klein (klein@cs.stanford.edu)
 */
abstract public class CollectionFactory implements Serializable {
  public static final CollectionFactory ARRAY_LIST_FACTORY = new ArrayListFactory();
  public static final CollectionFactory HASH_SET_FACTORY = new HashSetFactory();

  public static class ArrayListFactory extends CollectionFactory {
    public Collection newCollection() {
      return new ArrayList();
    }

    public Collection newEmptyCollection() {
      return Collections.EMPTY_LIST;
    }
  }

  public static class HashSetFactory extends CollectionFactory {
    public Collection newCollection() {
      return new HashSet();
    }

    public Collection newEmptyCollection() {
      return Collections.EMPTY_SET;
    }
  }

  abstract public Collection newCollection();

  abstract public Collection newEmptyCollection();
}
