package alteredu.stanford.nlp.process;


import alteredu.stanford.nlp.ling.FeatureLabel;
import alteredu.stanford.nlp.objectbank.TokenizerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;

/**
 * Tokenizer implementation that conforms to the Penn Treebank tokenization
 * conventions.
 * This tokenizer is a Java implementation of Professor Chris Manning's Flex
 * tokenizer, pgtt-treebank.l.  It reads raw text and outputs
 * tokens as edu.stanford.nlp.trees.MapLabels in the Penn treebank format.
 *
 * @author Jenny Finkel (<a href="mailto:jrfinkel@stanford.edu">jrfinkel@stanford.edu</a>)
 */
public class InvertiblePTBTokenizer extends AbstractTokenizer<FeatureLabel> {

  // the underlying lexer
  InvertiblePTBLexer lexer;
  // the position of the next token in the document
  int position;
  private boolean tokenizeCRs;

  public static final String BEFORE_KEY = "before";
  public static final String CURRENT_KEY = "current";
  public static final String AFTER_KEY = "after";
  public static final String START_POSITION_KEY = "startPosition";
  public static final String END_POSITION_KEY = "endPosition";

  /**
   * Constructs a new PTBTokenizer that treats carriage returns as normal whitespace.
   */
  public InvertiblePTBTokenizer(Reader r) {
    this(r, false);
  }
  /**
   * Constructs a new PTBTokenizer.
   */
  public InvertiblePTBTokenizer(Reader r, boolean tokenizeCRs) {
    this.tokenizeCRs = tokenizeCRs;
    setSource(r);
  }

  /**
   * Internally fetches the next token.
   *
   * @return the next token in the token stream, or null if none exists.
   */
  protected FeatureLabel getNext() {
    if (lexer == null) {
      return null;
    }
    FeatureLabel token = null;
    try {
      token = (FeatureLabel)lexer.next();
//       while (!tokenizeCRs && InvertiblePTBLexer.cr.equals(token.word())) {
//         token = (FeatureLabel)lexer.next();
//       }
    } catch (Exception e) {
      e.printStackTrace();
      nextToken = null;
      // do nothing, return null
    }
    addIndices(token);
    return token;
  }

  private void addIndices(FeatureLabel token) {
    if (token==null) return;
    String before = (String) token.get(BEFORE_KEY);
    position += before.length();
    token.put(START_POSITION_KEY, new Integer(position));
    String current = (String) token.get(CURRENT_KEY);
    int cLen = current.length();
    position += (cLen);
    token.put(END_POSITION_KEY, new Integer(position));
  }


  /**
   * Reads a file from the argument and prints its tokens one per line.
   * This is mainly as a testing aid, but it can also be quite useful
   * standalone to turn a corpus into a one token per line file of tokens.
   * <p/>
   * Usage: <code>java edu.stanford.nlp.process.PTBTokenizer filename
   * </code>
   *
   * @param args Command line arguments
   */
  public static void main(String[] args) throws IOException {
    if (args.length < 1) {
      System.err.println("usage: java edu.stanford.nlp.process." + "InvertablePTBTokenizer filename+");
      return;
    }

    for (int j = 0; j < args.length; j++) {
      InvertiblePTBTokenizer tokenizer = new InvertiblePTBTokenizer(new FileReader(args[j]), true);

      List words = tokenizer.tokenize();

      Iterator iter = words.iterator();

      while (iter.hasNext()) {
        FeatureLabel iw = (FeatureLabel) iter.next();
        System.err.println(iw.word());
      }

      System.err.println("===============================");

      iter = words.iterator();

      while (iter.hasNext()) {
        FeatureLabel iw = (FeatureLabel) iter.next();
        System.out.print((String)iw.get(BEFORE_KEY) + (String)iw.get(CURRENT_KEY));
        if (!iter.hasNext()) {
          System.out.print(iw.get(AFTER_KEY));
        }
      }
    }
  }


  /**
   * Sets the source of this Tokenizer to be the Reader r.
   */
  public void setSource(Reader r) {
    lexer = new InvertiblePTBLexer(r);
    lexer.tokenizeCRs = this.tokenizeCRs;
    position = 0;
  }

  public static TokenizerFactory factory() {
    return new InvertiblePTBTokenizerFactory(false);
  }

  public static TokenizerFactory factory(boolean tokenizeCRs) {
    return new InvertiblePTBTokenizerFactory(tokenizeCRs);
  }

  public static class InvertiblePTBTokenizerFactory implements TokenizerFactory<FeatureLabel> {
    boolean tokenizeCRs;

    public Iterator<FeatureLabel> getIterator(Reader r) {
      return getTokenizer(r);
    }

    public Tokenizer<FeatureLabel> getTokenizer(Reader r) {
      return new InvertiblePTBTokenizer(r, tokenizeCRs);
    }

    public InvertiblePTBTokenizerFactory(boolean tokenizeCRs) {
      this.tokenizeCRs = tokenizeCRs;
    }	 

  }

}

