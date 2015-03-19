package alteredu.stanford.nlp.process;


import alteredu.stanford.nlp.ling.HasWord;
import alteredu.stanford.nlp.ling.Word;
import alteredu.stanford.nlp.ling.FeatureLabel;
import alteredu.stanford.nlp.objectbank.TokenizerFactory;
import alteredu.stanford.nlp.util.StringUtils;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Tokenizer implementation that conforms to the Penn Treebank tokenization
 * conventions.
 * This tokenizer is a Java implementation of Professor Chris Manning's Flex
 * tokenizer, pgtt-treebank.l.  It reads raw text and outputs
 * tokens as edu.stanford.nlp.trees.Words in the Penn treebank format. It can
 * optionally return carriage returns as tokens.
 *
 * @author Tim Grow
 * @author Teg Grenager (grenager@stanford.edu)
 * @author Christopher Manning
 */
public class PTBTokenizer extends AbstractTokenizer {

  // whether carriage returns should be returned as tokens
  private boolean tokenizeCRs;

  // the underlying lexer
  PTBLexer lexer;
  LexedTokenFactory tokenFactory;
  private int position;

  /**
   * Constructs a new PTBTokenizer that treats carriage returns as normal whitespace.
   */
  public PTBTokenizer(Reader r) {
    this(r, false);
  }

  /**
   * Constructs a new PTBTokenizer that optionally returns carriage returns
   * as their own token. CRs come back as Words whose text is
   * the value of <code>PTBLexer.cr</code>.
   */
  public PTBTokenizer(Reader r, boolean tokenizeCRs) {
    this(r, tokenizeCRs, new WordTokenFactory());
  }

  /**
   * Constructs a new PTBTokenizer that optionally returns carriage returns
   * as their own token, and has a custom LexedTokenFactory.
   * CRs come back as Words whose text is
   * the value of <code>PTBLexer.cr</code>.
   *
   * @param tokenFactory The LexedTokenFactory to use to create
   *  tokens from the text.
   */
  public PTBTokenizer(Reader r, boolean tokenizeCRs,
      LexedTokenFactory tokenFactory)
  {
    this.tokenizeCRs = tokenizeCRs;
    this.tokenFactory = tokenFactory;
    setSource(r);
  }

  /**
   * Internally fetches the next token.
   *
   * @return the next token in the token stream, or null if none exists.
   */
  protected Object getNext() {
    if (lexer == null) {
      return null;
    }
    Object token = null;
    try {
      token = lexer.next();
      // get rid of CRs if necessary
      while (!tokenizeCRs && PTBLexer.cr.equals(((HasWord) token).word())) {
        token = lexer.next();
      }
    } catch (Exception e) {
      nextToken = null;
      // do nothing, return null
    }
    if(tokenFactory instanceof FeatureLabelTokenFactory) {
      addIndices((FeatureLabel)token);
    }
    return token;
  }

  private void addIndices(FeatureLabel token) {
    if (token==null) return;
    String before = (String) token.get("before");
    position += before.length();
    token.put("startPosition", new Integer(position));
    String current = (String) token.get("current");
    int cLen = current.length();
    position += (cLen);
    token.put("endPosition", new Integer(position));
    position += 1;
  }

  /**
   * Sets the source of this Tokenizer to be the Reader r.
   */
  public void setSource(Reader r) {
    lexer = new PTBLexer(r, tokenFactory);
    position = 0;
  }

  /**
   * Returns a presentable version of the given PTB-tokenized text.
   * PTB tokenization splits up punctuation and does various other things
   * that makes simply joining the tokens with spaces look bad. So join
   * the tokens with space and run it through this method to produce nice
   * looking text. It's not perfect, but it works pretty well.
   */
  public static String ptb2Text(String ptbText) {
    StringBuffer sb = new StringBuffer(ptbText.length()); // probably an overestimate
    PTB2TextLexer lexer = new PTB2TextLexer(new StringReader(ptbText));
    String token;
    try {
      while ((token = lexer.next()) != null) {
        sb.append(token);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return (sb.toString());
  }

  /**
   * Returns a presentable version of the given PTB-tokenized words.
   * Pass in a List of Words or Strings, or a Document and this method will
   * join the words with spaces and call {@link #ptb2Text(String) } on the
   * output. This method will check if the elements in the list are subtypes
   * of Word, and if so, it will take the word() values to prevent additional
   * text from creeping in (e.g., POS tags). Otherwise the toString value will
   * be used.
   */
  public static String ptb2Text(List ptbWords) {
    for (int i = 0, sz = ptbWords.size(); i < sz; i++) {
      if (ptbWords.get(i) instanceof Word) {
        ptbWords.set(i, ((Word) ptbWords.get(i)).word());
      }
    }

    return (ptb2Text(StringUtils.join(ptbWords)));
  }

  public static TokenizerFactory factory() {
    return new PTBTokenizerFactory();
  }

  public static TokenizerFactory factory(boolean tokenizeCRs, LexedTokenFactory factory) {
    return new PTBTokenizerFactory(tokenizeCRs, factory);
  }

  public static class PTBTokenizerFactory implements TokenizerFactory {

    protected boolean tokenizeCRs;
    protected LexedTokenFactory factory;

    /**
     * Constructs a new PTBTokenizerFactory that treats carriage returns as
     * normal whitespace.
     */
    public PTBTokenizerFactory() {
      this(false);
    }

    /**
     * Constructs a new PTBTokenizer that optionally returns carriage returns
     * as their own token. CRs come back as Words whose text is
     * the value of <code>PTBLexer.cr</code>.
     */
    public PTBTokenizerFactory(boolean tokenizeCRs) {
      this.tokenizeCRs = tokenizeCRs;
      this.factory = new WordTokenFactory();
    }

    public PTBTokenizerFactory(boolean tokenizeCRs, LexedTokenFactory factory) {
      this.tokenizeCRs = tokenizeCRs;
      this.factory = factory;
    }

    public Iterator getIterator(Reader r) {
      return getTokenizer(r);
    }

    public Tokenizer getTokenizer(Reader r) {
      return new PTBTokenizer(r, tokenizeCRs, factory);
    }



  }

  /**
   * Reads a file from the argument and prints its tokens one per line.
   * This is mainly as a testing aid, but it can also be quite useful
   * standalone to turn a corpus into a one token per line file of tokens.
   * This main method assumes that the input file is in utf-8 encoding,
   * unless it is specified.
   * <p/>
   * Usage: <code>java edu.stanford.nlp.process.PTBTokenizer [-charset charset] [-nl] filename
   * </code>
   *
   * @param args Command line arguments
   */
  public static void main(String[] args) throws IOException {
    if (args.length < 1) {
      System.err.println("usage: java edu.stanford.nlp.process.PTBTokenizer [-nl/-preserveLines/-ioFileList] filename");
      return;
    }
    int i = 0;
    String charset = "utf-8";
    Pattern parseInsideBegin = null;
    Pattern parseInsideEnd = null;
    boolean tokenizeNL = false;
    boolean preserveLines = false;
    boolean inputOutputFileList = false;

    while (args[i].charAt(0) == '-') {
      if ("-nl".equals(args[i])) {
        tokenizeNL = true;
      } else if ("-preserveLines".equals(args[i])) {
        preserveLines = true;
        tokenizeNL = true;
      } else if ("-ioFileList".equals(args[i])) {
        inputOutputFileList = true;
      }else if ("-charset".equals(args[i]) && i < args.length - 1) {
        i++;
        charset = args[i];
      } else if ("-parseInside".equals(args[i]) && i < args.length - 1) {
        i++;
        try {
          parseInsideBegin = Pattern.compile("<(?:" + args[i] + ")>");
          parseInsideEnd = Pattern.compile("</(?:" + args[i] + ")>");
        } catch (Exception e) {
          parseInsideBegin = null;
          parseInsideEnd = null;
        }
      } else {
        System.err.println("Unknown option: " + args[i]);
      }
      i++;
    }
    ArrayList<String> inputFileList = new ArrayList<String>();
    ArrayList<String> outputFileList=null;

    if (inputOutputFileList) {
      outputFileList = new ArrayList<String>();
      for (int j = i; j < args.length; j++) {
        BufferedReader r = new BufferedReader(
          new InputStreamReader(new FileInputStream(args[j]), charset));
        for (String inLine; (inLine = r.readLine()) != null; ) {
          String[] fields = inLine.split("\\s+");
          inputFileList.add(fields[0]);
          outputFileList.add(fields[1]);
        }
      }
    } else {
      for (int j = i; j < args.length; j++) inputFileList.add(args[j]);
    }
    for (int j = 0; j < inputFileList.size(); j++) {
      Reader r = new BufferedReader(new InputStreamReader(
        new FileInputStream(inputFileList.get(j)), charset));
      PrintWriter out = new PrintWriter(System.out, true);
      if (outputFileList != null) {
        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFileList.get(j)), charset)), true);
      }

      PTBTokenizer tokenizer = new PTBTokenizer(r, tokenizeNL);
      boolean printing = true;
      if (parseInsideBegin != null) {
        printing = false;
      }
      boolean beginLine = true;
      while (tokenizer.hasNext()) {
        Object obj = tokenizer.next();
        String str = obj.toString();
        if (parseInsideBegin != null && parseInsideBegin.matcher(str).matches()) {
          printing = true;
        } else if (parseInsideEnd != null && parseInsideEnd.matcher(str).matches()) {
          printing = false;
        } else if (printing) {
          if (preserveLines) {
            if ("*CR*".equals(str)) { beginLine=true; out.println(""); }
            else {
              if (!beginLine) out.print(" ");
              out.print(str); beginLine = false;
            }
          } else {
            out.println(str);
          }
        }
      }
      if (outputFileList != null) out.close();
    }
  }

}

