package alteredu.stanford.nlp.objectbank;

import alteredu.stanford.nlp.process.Function;
import alteredu.stanford.nlp.util.AbstractIterator;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * An Iterator that reads the contents of a buffer, delimited by the specified
 * delimiter, and then be subsequently processed by an Function to produce Objects.
 *
 * @author Jenny Finkel <A HREF="mailto:jrfinkel@stanford.edu>jrfinkel@stanford.edu</A>
 */
public class DelimitRegExIterator extends AbstractIterator {

  private Iterator tokens;
  Function op;
  Object nextToken = null;

  public DelimitRegExIterator(Reader in, String delimiter) {
    this(in, delimiter, new IdentityFunction());
  }

  public DelimitRegExIterator(Reader r, String delimiter, Function op) {
    this.op = op;
    BufferedReader in = new BufferedReader(r);
    try {
      String line;
      StringBuilder input = new StringBuilder();
      while ((line = in.readLine()) != null) {
        input.append(line).append("\n");
      }
      line = input.toString();
      Pattern p = Pattern.compile("^"+delimiter);
      Matcher m = p.matcher(line);
      line = m.replaceAll("");
      p = Pattern.compile(delimiter+"$");
      m = p.matcher(line);
      line = m.replaceAll("");
      line = line.trim();

      tokens = (Arrays.asList(line.split(delimiter))).iterator();
    } catch (Exception e) {
    }
    setNext();
  }

  private void setNext() {
    if (tokens.hasNext()) {
      String s = (String) tokens.next();
      nextToken = parseString(s);
    } else {
      nextToken = null;
    }
  }

  protected Object parseString(String s) {
    return op.apply(s);
  }

  public boolean hasNext() {
    return nextToken != null;
  }

  public Object next() {
    Object token = nextToken;
    setNext();
    return token;
  }

  public Object peek() {
    return nextToken;
  }

  /**
   * Returns a factory that vends DelimitRegExIterators that reads the contents of the
   * given Reader, splits on the specified delimiter, then returns the result.
   */
  public static IteratorFromReaderFactory getFactory(String delim) {
    return new DelimitRegExIteratorFactory(delim);
  }

  /**
   * Returns a factory that vends DelimitRegExIterators that reads the contents of the
   * given Reader, splits on the specified delimiter, then returns the result.
   */
  public static IteratorFromReaderFactory getFactory(String delim, boolean eolIsSignificant) {
    return new DelimitRegExIteratorFactory(delim, eolIsSignificant);
  }

  /**
   * Returns a factory that vends DelimitRegExIterators that reads the contents of the
   * given Reader, splits on the specified delimiter, applies op, then returns the result.
   */
  public static IteratorFromReaderFactory getFactory(String delim, Function op) {
    return new DelimitRegExIteratorFactory(delim, op);
  }

  /**
   * Returns a factory that vends DelimitRegExIterators that reads the contents of the
   * given Reader, splits on the specified delimiter, applies op, then returns the result.
   */
  public static IteratorFromReaderFactory getFactory(String delim, Function op, boolean eolIsSignificant) {
    return new DelimitRegExIteratorFactory(delim, op, eolIsSignificant);
  }

  public static class DelimitRegExIteratorFactory implements IteratorFromReaderFactory, Serializable {

    private static final long serialVersionUID = 6846060575832573082L;

    private String delim;
    private Function op;
    private boolean eolIsSignificant;

    public DelimitRegExIteratorFactory(String delim) {
      this(delim, new IdentityFunction());
    }

    public DelimitRegExIteratorFactory(String delim, boolean eolIsSignificant) {
      this(delim, new IdentityFunction(), eolIsSignificant);
    }

    public DelimitRegExIteratorFactory(String delim, Function op) {
      this(delim, op, true);
    }

    public DelimitRegExIteratorFactory(String delim, Function op, boolean eolIsSignificant) {
      this.delim = delim;
      this.op = op;
      this.eolIsSignificant = eolIsSignificant;
    }

    public Iterator getIterator(Reader r) {
      return new DelimitRegExIterator(r, delim, op);
    }

  }

  public static void main(String[] args) {

    String s = "@@123\nthis\nis\na\nsentence\n\n@@124\nThis\nis\nanother\n.\n\n@125\nThis\nis\nthe\nlast\n";
    DelimitRegExIterator di = new DelimitRegExIterator(new StringReader(s), "\n\n");
    while (di.hasNext()) {
      System.out.println("****\n" + di.next() + "\n****");
    }

  }

}
