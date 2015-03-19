package alteredu.stanford.nlp.objectbank;

import alteredu.stanford.nlp.process.Function;
import alteredu.stanford.nlp.util.AbstractIterator;
import alteredu.stanford.nlp.util.XMLUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * A class which iterates over Strings occuring between the begin and end of 
 * a selected tag or tags. The element is specified by a regexp, matched
 * against the name of the element (i.e., excluding the angle bracket
 * characters) using <code>matches()</code>).
 * The class ignores all other characters in the input Reader.
 *
 * @author Teg Grenager (grenager@stanford.edu)
 */
public class XMLBeginEndIterator extends AbstractIterator {

  private Pattern tagNamePattern;
  private BufferedReader in;
  private Object nextToken;
  private Function op;
  private boolean keepInternalTags;
  private boolean keepDelimitingTags;

  public XMLBeginEndIterator(Reader in, String tagNameRegexp) {
    this(in, tagNameRegexp, new IdentityFunction(), false);
  }

  public XMLBeginEndIterator(Reader in, String tagNameRegexp, boolean keepInternalTags) {
    this(in, tagNameRegexp, new IdentityFunction(), keepInternalTags);
  }

  public XMLBeginEndIterator(Reader in, String tagNameRegexp, Function op, boolean keepInternalTags) {
    this(in, tagNameRegexp, op, keepInternalTags, false);
  }

  public XMLBeginEndIterator(Reader in, String tagNameRegexp, boolean keepInternalTags, boolean keepDelimitingTags) {
    this(in, tagNameRegexp, new IdentityFunction(), keepInternalTags, keepDelimitingTags);
  }

  public XMLBeginEndIterator(Reader in, String tagNameRegexp, Function op, boolean keepInternalTags, boolean keepDelimitingTags) {
    this.tagNamePattern = Pattern.compile(tagNameRegexp);
    this.op = op;
    this.keepInternalTags = keepInternalTags;
    this.keepDelimitingTags = keepDelimitingTags;
    this.in = new BufferedReader(in);
    setNext();
  }

  private void setNext() {
    String s = getNext();
    nextToken = parseString(s);
  }

  // returns null if there is no next object
  private String getNext() {
    StringBuilder result = new StringBuilder();
    try {
      XMLUtils.XMLTag tag;
      do {
        String text = XMLUtils.readUntilTag(in);
        // there may or may not be text before the next tag, but we discard it
        //        System.out.println("outside text: " + text );
        tag = XMLUtils.readAndParseTag(in);
        //        System.out.println("outside tag: " + tag);
        if (tag == null) {
          return null; // couldn't find any more tags, so no more elements
        }
      } while ( ! tagNamePattern.matcher(tag.name).matches() || tag.isEndTag);
      if (keepDelimitingTags) {
        result.append(tag.toString());
      }
      while (true) {
        String text = XMLUtils.readUntilTag(in);
        if (text != null) {
          // if the text isn't null, we append it
          //        System.out.println("inside text: " + text );
          result.append(text);
        }
        String tagString = XMLUtils.readTag(in);
        tag = XMLUtils.parseTag(tagString);
        if (tag == null) {
          return null; // unexpected end of this element, so no more elements
        }
        if (tagNamePattern.matcher(tag.name).matches() && tag.isEndTag) {
          if (keepDelimitingTags) {
            result.append(tagString);
          }
          // this is our end tag so we stop
          break;
        } else {
          // not our end tag, so we optionally append it and keep going
          if (keepInternalTags) {
            result.append(tagString);
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return result.toString();
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
   * Returns a factory that vends BeginEndIterators that reads the contents of
   * the given Reader, extracts text between the specified Strings, then
   * returns the result.
   *
   * @param tag 
   */
  public static IteratorFromReaderFactory getFactory(String tag) {
    return new XMLBeginEndIterator.XMLBeginEndIteratorFactory(tag, new IdentityFunction(), false, false);
  }

  public static IteratorFromReaderFactory getFactory(String tag, boolean keepInternalTags, boolean keepDelimitingTags) {
    return new XMLBeginEndIterator.XMLBeginEndIteratorFactory(tag, new IdentityFunction(), keepInternalTags, keepDelimitingTags);
  }

  public static IteratorFromReaderFactory getFactory(String tag, Function op) {
    return new XMLBeginEndIterator.XMLBeginEndIteratorFactory(tag, op, false, false);
  }
  
  public static IteratorFromReaderFactory getFactory(String tag, Function op, boolean keepInternalTags, boolean keepDelimitingTags) {
    return new XMLBeginEndIterator.XMLBeginEndIteratorFactory(tag, op, keepInternalTags, keepDelimitingTags);
  }
  
  static class XMLBeginEndIteratorFactory implements IteratorFromReaderFactory {

    private String tag;
    private Function op;
    private boolean keepInternalTags;
    private boolean keepDelimitingTags;

    public XMLBeginEndIteratorFactory(String tag, Function op, boolean keepInternalTags, boolean keepDelimitingTags) {
      this.tag = tag;
      this.op = op;
      this.keepInternalTags = keepInternalTags;
      this.keepDelimitingTags = keepDelimitingTags;
    }
    
    public Iterator getIterator(Reader r) {
      return new XMLBeginEndIterator(r, tag, op, keepInternalTags, keepDelimitingTags);
    }
  }

  public static void main(String[] args) throws Exception {
    Reader in = new FileReader(args[0]);
    Iterator iter = new XMLBeginEndIterator(in, args[1], args[2].equalsIgnoreCase("true"));
    while (iter.hasNext()) {
      String s = (String) iter.next();
      System.out.println("*************************************************");
      System.out.println(s);
    }
    in.close();
  }
}

