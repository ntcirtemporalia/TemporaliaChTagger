package alteredu.stanford.nlp.process;

import alteredu.stanford.nlp.ling.BasicDocument;
import alteredu.stanford.nlp.ling.Document;
import alteredu.stanford.nlp.ling.HasWord;
import alteredu.stanford.nlp.ling.Sentence;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Transforms a Document of Words into a Document of Sentences by grouping the
 * Words.  The word stream is assumed to already be adequately tokenized,
 * and this class just divides the list into sentences, perhaps discarding
 * some separator tokens based on the setting of the following three sets:
 * <ul>
 * <li>sentenceBoundaryTokens are tokens that are left in a sentence, but are
 * to be regarded as ending a sentence.  A canonical example is a period.
 * If two of these follow each other, the second will be a sentence
 * consisting of only the sentenceBoundaryToken.
 * <li>sentenceBoundaryFollowers are tokens that are left in a sentence, and
 * which can follow a sentenceBoundaryToken while still belonging to
 * the previous sentence.  They cannot begin a sentence (except at the
 * beginning of a document).  A canonical example is a close parenthesis
 * ')'.
 * <li>sentenceBoundaryToDiscard are tokens which separate sentences and
 * which should be thrown away.  In web documents, a typical example would
 * be a '&lt;p&gt;' tag.  If two of these follow each other, they are
 * coalesced: no empty Sentence is output.  The end-of-file is not
 * represented in this Set, but the code behaves as if it were a member.
 * <li>sentenceRegionBeginPattern A regular expression for marking the start
 * of a sentence region.  Not included in the sentence.
 * <li>sentenceRegionEndPattern A regular expression for marking the end
 * of a sentence region.  Not included in the sentence.
 * </ul>
 *
 * @author Joseph Smarr (jsmarr@stanford.edu)
 * @author Christopher Manning
 * @author Teg Grenager (grenager@stanford.edu)
 */
public class WordToSentenceProcessor extends AbstractListProcessor {

  private static final boolean DEBUG = false;

  /**
   * Set of tokens (Strings) that qualify as sentence-final tokens.
   */
  private Set sentenceBoundaryTokens;

  /**
   * Set of tokens (Strings) that qualify as tokens that can follow
   * what normally counts as an end of sentence token, and which are
   * attributed to the preceding sentence.  For example ")" coming after
   * a period.
   */
  private Set sentenceBoundaryFollowers;

  /**
   * Set of tokens (Strings) that are sentence boundaries to be discarded.
   */
  private Set sentenceBoundaryToDiscard;

  private Pattern sentenceRegionBeginPattern;

  private Pattern sentenceRegionEndPattern;


  /**
   * Returns a List of Sentences where each element is built from a run
   * of Words in the input Document. Specifically, reads through each word in
   * the input document and breaks off a sentence after finding a valid
   * sentence boundary token or end of file.
   * Note that for this to work, the words in the
   * input document must have been tokenized with a tokenizer that makes
   * sentence boundary tokens their own tokens (e.g., {@link PTBTokenizer}).
   *
   * @param words A list of already tokenized words (must implement HasWord)
   * @return A list of Sentence
   * @see #WordToSentenceProcessor(Set, Set, Set)
   * @see edu.stanford.nlp.ling.Sentence
   */
  public List process(List words) {
    List sentences = new ArrayList();
    List currentSentence = null;
    List lastSentence = null;
    boolean insideRegion = false;
    for (Iterator iter = words.iterator(); iter.hasNext();) {
      Object o = iter.next();
      String w = null;
      if (o instanceof HasWord) {
        HasWord h = (HasWord) o;
        w = h.word();
      } else if (o instanceof String) {
        w = (String) o;
      } else {
        throw new RuntimeException("Expected token to be either Word or String.");
      }
      if (DEBUG) {
        System.err.println("Word is " + w);
      }
      if (currentSentence == null) {
        currentSentence = new Sentence();
      }
      if (sentenceRegionBeginPattern != null && !insideRegion) {
        if (sentenceRegionBeginPattern.matcher(w).matches()) {
          insideRegion = true;
        }
        if (DEBUG) {
          System.err.println("  outside region");
        }
        continue;
      }
      if (sentenceBoundaryFollowers.contains(w) && lastSentence != null && currentSentence.size() == 0) {
        lastSentence.add(o);
        if (DEBUG) {
          System.err.println("  added to last");
        }
      } else {
        boolean newSent = false;
        if (sentenceBoundaryToDiscard.contains(w)) {
          newSent = true;
        } else if (sentenceRegionEndPattern != null && sentenceRegionEndPattern.matcher(w).matches()) {
          insideRegion = false;
          newSent = true;
        } else if (sentenceBoundaryTokens.contains(w)) {
          currentSentence.add(o);
          if (DEBUG) {
            System.err.println("  added to current");
          }
          newSent = true;
        } else {
          currentSentence.add(o);
          if (DEBUG) {
            System.err.println("  added to current");
          }
        }
        if (newSent && currentSentence.size() > 0) {
          if (DEBUG) {
            System.err.println("  beginning new sentence");
          }
          sentences.add(currentSentence);
          // adds this sentence now that it's complete
          lastSentence = currentSentence;
          currentSentence = null; // clears the current sentence
        }
      }
    }

    // add any words at the end, even if there isn't a sentence
    // terminator at the end of file
    if (currentSentence != null && currentSentence.size() > 0) {
      sentences.add(currentSentence); // adds last sentence
    }
    return (sentences);
  }


  /**
   * Create a <code>WordToSentenceProcessor</code> using a sensible default
   * list of tokens to split on.  The default set is: {".","?","!"}.
   */
  public WordToSentenceProcessor() {
    this(new HashSet(Arrays.asList(new String[]{".", "?", "!"})));
  }

  /**
   * Flexibly set the set of acceptable sentence boundary tokens, but with
   * a default set of allowed boundary following tokens.
   * The allowed set of boundary followers is:
   * {")","]","\"","\'", "''", "-RRB-", "-RSB-"}.
   */
  public WordToSentenceProcessor(Set boundaryTokens) {
    this(boundaryTokens, new HashSet(Arrays.asList(new String[]{")", "]", "\"", "\'", "''", "-RRB-", "-RSB-"})));
  }

  /**
   * Flexibly set the set of acceptable sentence boundary tokens and
   * also the set of tokens commonly following sentence boundaries, and
   * the set of discarded separator tokens.
   * The default set of discarded separator tokens is: {"\n"}.
   */
  public WordToSentenceProcessor(Set boundaryTokens, Set boundaryFollowers) {
    this(boundaryTokens, boundaryFollowers, Collections.singleton("\n"));
  }


  /**
   * Flexibly set the set of acceptable sentence boundary tokens,
   * the set of tokens commonly following sentence boundaries, and also
   * the set of tokens that are sentences boundaries that should be
   * discarded.
   */
  public WordToSentenceProcessor(Set boundaryTokens, Set boundaryFollowers, Set boundaryToDiscard) {
    this(boundaryTokens, boundaryFollowers, boundaryToDiscard, null, null);
  }

  public WordToSentenceProcessor(Pattern regionBeginPattern, Pattern regionEndPattern) {
    this(Collections.EMPTY_SET, Collections.EMPTY_SET, Collections.EMPTY_SET, regionBeginPattern, regionEndPattern);
  }

  /**
   * Flexibly set the set of acceptable sentence boundary tokens,
   * the set of tokens commonly following sentence boundaries, and also
   * the set of tokens that are sentences boundaries that should be
   * discarded.
   * This is private because it is a dangerous constructor. It's not clear what the semantics
   * should be if there are both boundary token sets, and patterns to match.
   */
  private WordToSentenceProcessor(Set boundaryTokens, Set boundaryFollowers, Set boundaryToDiscard, Pattern regionBeginPattern, Pattern regionEndPattern) {
    sentenceBoundaryTokens = boundaryTokens;
    sentenceBoundaryFollowers = boundaryFollowers;
    sentenceBoundaryToDiscard = boundaryToDiscard;
    sentenceRegionBeginPattern = regionBeginPattern;
    sentenceRegionEndPattern = regionEndPattern;
    //    System.out.println("boundaryTokens=" + boundaryTokens);
    //    System.out.println("boundaryFollowers=" + boundaryFollowers);
    //    System.out.println("boundaryToDiscard=" + boundaryToDiscard);
  }


  /* -- for testing only
  private void printSet(Set s) {
for (Iterator i = s.iterator(); i.hasNext();) {
    System.out.print(i.next() + " ");
}
System.out.println();
  }
  -- */


  /**
   * This will print out as sentences some text.  It can be used to
   * test sentence division.  <br>
   * Usage: java edu.stanford.nlp.process.WordToSentenceProcessor fileOrUrl+
   *
   * @param args Command line argument: files or URLs
   */
  public static void main(String[] args) {
    if (args.length == 0) {
      System.out.println("usage: java edu.stanford.nlp.process.WordToSentenceProcessor fileOrUrl");
      System.exit(0);
    }
    try {
      for (int i = 0; i < args.length; i++) {
        String filename = args[i];
        Document d; // always initialized below
        if (filename.startsWith("http://")) {
          Document dpre = new BasicDocument().init(new URL(filename));
          Processor notags = new StripTagsProcessor();
          d = notags.processDocument(dpre);
        } else {
          d = new BasicDocument().init(new File(filename));
        }
        WordToSentenceProcessor proc = new WordToSentenceProcessor();
        List sentd = proc.processDocument(d);
        for (Iterator it = sentd.iterator(); it.hasNext();) {
          Sentence sent = (Sentence) it.next();
          System.out.println(sent);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
