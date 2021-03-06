package alteredu.stanford.nlp.util;

import java.util.AbstractList;
import java.util.List;
import java.io.Serializable;

/**
 * A PaddedList wraps another list, presenting an apparently infinite
 * list by padding outside the real confines of the list with a default
 * value.  Note that <code>size()</code> returns the true size, but
 * <code>get()</code> works for any number.
 *
 * @author Christopher Manning
 */
public class PaddedList<E> extends AbstractList<E> implements Serializable {

  private final List<E> l;
  private final E padding;

  public E getPad() { 
    return padding; 
  }
  
  public int size() {
    return l.size();
  }

  public E get(int i) {
    if (i < 0 || i >= size()) {
      return padding;
    } else {
      return l.get(i);
    }
  }

  public String toString() {
    return l.toString();
  }
  
  /** With this constructor, get() will return <code>null</code> for 
   *  elements outside the real list.
   */
  public PaddedList(List<E> l) {
    this(l, null);
  }

  public PaddedList(List<E> l, E padding) {
    this.l = l;
    this.padding = padding;
  }

  /** A static method that provides an easy way to create a list of a
   *  certain parametric type.
   */
  public static <F> PaddedList<F> valueOf(List<F> list) {
    return new PaddedList<F>(list);
  }

  /** Returns true if this PaddedList and another are wrapping the
   *  same list.  This is tested as ==. Kinda yucky, but sometimes you
   *  want to know.
   */
  public boolean sameInnerList(PaddedList<E> p) {
    return p != null && l == p.l;
  }

  private static final long serialVersionUID = 2064775966439971729L;

}
