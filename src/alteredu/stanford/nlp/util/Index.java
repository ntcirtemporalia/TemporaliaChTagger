package alteredu.stanford.nlp.util;

import java.io.*;
import java.util.*;

/**
 * An Index is a collection that maps between an Object vocabulary and a
 * contiguous non-negative integer index beginning (inclusively) at 0.  It supports constant-time lookup in
 * both directions (via <code>get(int)</code> and <code>indexOf(Object)</code>.
 * The <code>indexOf(Object)</code> method compares objects by
 * <code>equals</code>, as other Collections.
 * <p/>
 * The typical usage would be:
 * <p><code>Index index = new Index(collection);</code>
 * <p> followed by
 * <p><code>int i = index.indexOf(object);</code>
 * <p> or
 * <p><code>Object o = index.get(i);</code>
 * <p>The source contains a concrete example of use as the main method.
 * <p>An Index can be locked or unlocked: a locked index cannot have new
 * items added to it.
 *
 * @author <a href="mailto:klein@cs.stanford.edu">Dan Klein</a>
 * @version 1.0
 * @see AbstractCollection
 * @since 1.0
 */
public class Index<E> extends AbstractCollection<E> implements Serializable, RandomAccess {

  protected List<E> objects = new ArrayList<E>();
  protected Map<Object,Integer> indexes = new HashMap<Object,Integer>();
  protected boolean locked = false;

  /**
   * Clears this Index.
   */
  public void clear() {
    objects.clear();
    indexes.clear();
  }

  /**
   * Returns the index of each elem in an array.
   */
  public int[] indices(List<E> elems) {
    int[] indices = new int[elems.size()];
    for (int i = 0; i < elems.size(); i++) {
      indices[i] = indexOf(elems.get(i));
    }
    return (indices);
  }

  /**
   * Looks up the objects corresponding to an array of indices, and returns them in a {@link Collection}.
   * @param indices
   * @return a {@link Collection} of the objects corresponding to the indices argument.
   */
  public Collection<E> objects(final int[] indices) {
    return new AbstractList<E>() {
      public E get(int index) {
        return objects.get(indices[index]);
      }

      public int size() {
        return indices.length;
      }
    };
  }

  /**
   * Checks the number of indexed objects.
   * @return the number of indexed objects.
   */
  public int size() {
    return objects.size();
  }

  /**
   * Gets the object whose index is the integer argument.
   * @param i the integer index to be queried for the corresponding argument
   * @return the object whose index is the integer argument.
   */
  public E get(int i) {
    return objects.get(i);
  }

  /**
   * Returns a complete {@link List} of indexed objects, in the order of their indices.  <b>DANGER!</b>
   * The current implementation returns the actual index list, not a defensive copy.  Messing with this List
   * can seriously screw up the state of the Index.  (perhaps this method needs to be eliminated? I don't think it's
   * ever used in ways that we couldn't use the Index itself for directly.  --Roger, 12/29/04)   
   * @return a complete {@link List} of indexed objects
   */
  public List<E> objectsList() {
    return objects;
  }

  /**
   * Queries the Index for whether it's locked or not.
   * @return whether or not the Index is locked
   */
  public boolean isLocked() {
    return locked;
  }

  /** Locks the Index.  A locked index cannot have new elements added to it (calls to {@link #add} will
   * leave the Index unchanged and return <code>false</code>).*/
  public void lock() {
    locked = true;
  }

  /** Unlocks the Index.  A locked index cannot have new elements added to it (calls to {@link #add} will
   * leave the Index unchanged and return <code>false</code>).*/
  public void unlock() {
    locked = false;
  }

  /**
   * Takes an Object and returns the integer index of the Object.  Returns -1 if the Object is not in the Index.
   * @param o the Object whose index is desired.
   * @return the index of the Object argument.  Returns -1 if the object is not in the index.
   */
  public int indexOf(Object o) {
    return indexOf(o, false);
  }

  /**
   * Takes an Object and returns the integer index of the Object.  Returns -1 if the Object is not in the Index.
   * @param o the Object whose index is desired.
   * @return the index of the Object argument.  Returns -1 if the object is not in the index.
   */
  public int indexOf(Object o, boolean add) {
    Integer index = indexes.get(o);
    if (index == null) {
      if (add) {
        add((E)o);
        index = indexes.get(o);
      } else {
        return -1;
      }
    }
    return index.intValue();
  }

 /**
   * Adds every member of Collection to the Index. Does nothing for members already in the Index.
   *
   * @return true if some item was added to the index and false if no
   *         item was already in the index or if the index is locked
   */
 public boolean addAll(Collection<? extends E> c) {
    boolean changed = false;
    for (E element: c){
      changed &= add(element);
    }
    return changed;
  }
  
  /**
   * Adds an object to the Index. If it was already in the Index,
   * then nothing is done.  If it is not in the Index, then it is
   * added iff the Index hasn't been locked.
   *
   * @return true if the item was added to the index and false if the
   *         item was already in the index or if the index is locked
   */
  public boolean add(E o) {
    Integer index = indexes.get(o);
    if (index == null && !locked) {
      index = new Integer(objects.size());
      objects.add(o);
      indexes.put(o, index);
      //modCount++; // not 100% sure that I don't need this anymore -- Roger
      return true;
    }
    return false;
  }

  /**
   * Checks whether an Object already has an index in the Index
   * @param o the object to be queried.
   * @return true iff there is an index for the queried object.
   */
  public boolean contains(Object o) {
    return indexes.containsKey(o);
  }

  /**
   * Creates a new Index.
   */
  public Index() {
    super();
  }

  /**
   * Creates a new Index and adds every member of c to it.
   */
  public Index(Collection<? extends E> c) {
    this();
    addAll(c);
  }

  public void serializeReadable(String file) {
    try {
      BufferedWriter bw = new BufferedWriter(new FileWriter(file));
      for (int i = 0; i < size(); i++) {
        bw.write(i + "=" + get(i) + "\n");
      }
      bw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static Index deserializeReadable(String file) {
    Index index = new Index();
    try {
      BufferedReader br = new BufferedReader(new FileReader(file));
      String line;
      while ((line = br.readLine()) != null) {
        int start = line.indexOf('=');
        if (start == -1 || start == line.length() - 1) {
          continue;
        }
        index.add(line.substring(start + 1));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return index;
  }

  public String toString() {
    StringBuffer buff = new StringBuffer("[");
    for (int i = 0; i < objects.size(); i++) {
      E e = objects.get(i);
      buff.append(i+"="+e);
      if (i<objects.size()-1) buff.append(",");
    }
    buff.append("]");
    return buff.toString();
  }
  public String firstNToString(int n) {
    StringBuffer buff = new StringBuffer("[");
		int i;
    for (i = 0; i < objects.size() && i<n; i++) {
      E e = objects.get(i);
      buff.append(i+"="+e);
      if (i<objects.size()-1) buff.append(",");
    }
		if (i<objects.size()) buff.append("...");
    buff.append("]");
    return buff.toString();
  }

  public static void main(String[] args) {
    List list = new ArrayList();
    list.add("A");
    list.add("B");
    list.add("A");
    list.add("C");
    Index index = new Index(list);
    System.out.println("Index size: " + index.size());
    System.out.println("Index has A? : " + index.contains("A"));
    System.out.println("Index of A: " + index.indexOf("A"));
    System.out.println("Index of B: " + index.indexOf("B"));
    System.out.println("Index of C: " + index.indexOf("C"));
    System.out.println("Object 0: " + index.get(0));
    index = index.unmodifiableView();
    System.out.println("Index size: " + index.size());
    System.out.println("Index has A? : " + index.contains("A"));
    System.out.println("Index of A: " + index.indexOf("A"));
    System.out.println("Index of B: " + index.indexOf("B"));
    System.out.println("Index of C: " + index.indexOf("C"));
    System.out.println("Object 0: " + index.get(0));
    
  }

  private static final long serialVersionUID = 5398562825928375260L;

  /**
   * Returns an iterator over the elements of the collection.
   * @return
   */
  public Iterator<E> iterator() {
    return objects.iterator();
  }

  /**
   * Removes an object from the index, if it exists (otherwise nothing
   * happens).  Note, the indices of other
   * elements will not be changed, so indices will no longer necessarily
   * be contiguous
   * @param o the object to remove
   * @return whether anything was removed
   */
  public boolean remove(Object o) {
    Integer oldIndex = indexes.remove(o);
    if (oldIndex == null) {
      return false;
    } else {
      objects.set(oldIndex, null);
      return true;
    }
  }

  /**
   * Returns and unmodifiable view of the Index.  It is just
   * a locked index that cannot be unlocked, so if you
   * try to add something, nothing will happen (it won't throw
   * an excpetion).  Trying to unlock it will throw an
   * UnsupportedOperationException.  If the
   * underlying Index is modified, the change will
   * "write-through" to the view.
   */
  public Index<E> unmodifiableView() {
    Index newIndex = new Index<E>() {
      public void unlock() { throw new UnsupportedOperationException("This is an unmodifiable view!"); }
    };
    newIndex.objects = objects;
    newIndex.indexes = indexes;
    newIndex.lock();
    return newIndex;
  }
}
