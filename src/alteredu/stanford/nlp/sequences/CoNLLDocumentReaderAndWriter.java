package alteredu.stanford.nlp.sequences;

import alteredu.stanford.nlp.ling.FeatureLabel;
import alteredu.stanford.nlp.io.RuntimeIOException;
import alteredu.stanford.nlp.util.PaddedList;
import alteredu.stanford.nlp.util.StringUtils;
import alteredu.stanford.nlp.util.AbstractIterator;

import java.util.*;
import java.util.regex.*;
import java.io.*;

/**
 * DocumentReader for CoNLL 03 format.  In this format, there is one word
 * per line, with extra attributes of a word (POS tag, chunk, etc.) in other
 * space or tab separated columns, where leading and trailing whitespace on
 * the line are ignored.  Sentences are supposedly
 * separated by a blank line (one with no non-whitespace characters), but
 * where blank lines occur is in practice often fairly random. In particular,
 * entities not infrequently span blank lines.
 *
 * @author Jenny Finkel
 * @author Huy Nguyen
 * @author Christopher Manning
 */
public class CoNLLDocumentReaderAndWriter implements DocumentReaderAndWriter {

  public static final String BOUNDARY = "*BOUNDARY*";
  public static final String OTHER = "O";
  private SeqClassifierFlags flags = null;
  
  public void init(SeqClassifierFlags flags) { 
    this.flags = flags;
  }
  
  public String toString() {
    return "CoNLLDocumentReaderAndWriter[entitySubclassification: " +
        flags.entitySubclassification + ", intern: " + flags.intern + "]";
  }


  public Iterator<List<FeatureLabel>> getIterator(Reader r) {
    return new CoNLLIterator(r);
  }

  private class CoNLLIterator extends AbstractIterator<List<FeatureLabel>> {

    public CoNLLIterator (Reader r) {
      stringIter = splitIntoDocs(r);
    }
    
    public boolean hasNext() { return stringIter.hasNext(); }
    public List<FeatureLabel> next() { return processDocument(stringIter.next()); }
    
    private Iterator<String> stringIter = null;
  }
  
  private Iterator<String>splitIntoDocs(Reader r) {
    return (Collections.singleton(StringUtils.slurpReader(r))).iterator();
  }
  
  private static Pattern white = Pattern.compile("^\\s*$");
  
  private List<FeatureLabel> processDocument(String doc) {
    List<FeatureLabel> lis = new ArrayList<FeatureLabel>();
    String[] lines = doc.split("\n");
    for (String line : lines) {
      if ( ! flags.deleteBlankLines || ! white.matcher(line).matches()) {
        lis.add(makeFeatureLabel(line));
      }
    }
    entitySubclassify(lis, flags.entitySubclassification);
    return lis;
  }
  
  /**
   * This was used on the CoNLL data to map from a representation where
   * normally entities were marked I-PERS, but the beginning of non-first
   * items of an entity sequences were marked B-PERS (IOB1 representation). 
   * It changes this representation to other representations:
   * a 4 way representation of all entities, like S-PERS, B-PERS, 
   * I-PERS, E-PERS for single word, beginning, internal, and end of entity
   * (SBIEO); always marking the first word of an entity (IOB2); 
   * the reverse IOE1 and IOE2 and IO.
   * This code is very specific to the particular CoNLL way of labeling 
   * classes.  It will work on any of these styles of input, however, except
   * for IO which necessarily loses information.
   */
  private void entitySubclassify(List<FeatureLabel> lineInfos,
                                 String style) {
    int how;
    if ("iob1".equalsIgnoreCase(style)) {
      how = 0;
    } else if ("iob2".equalsIgnoreCase(style)) {
      how = 1;
    } else if ("ioe1".equalsIgnoreCase(style)) {
      how = 2;
    } else if ("ioe2".equalsIgnoreCase(style)) {
      how = 3;
    } else if ("io".equalsIgnoreCase(style)) {
      how = 4;
    } else if ("sbieo".equalsIgnoreCase(style)) {
      how = 5;
    } else {
      System.err.println("entitySubclassify: unknown style: " + style);
      how = 4;
    }
    lineInfos = new PaddedList<FeatureLabel>(lineInfos, new FeatureLabel());
    int k = lineInfos.size();
    String[] newAnswers = new String[k];
    for (int i = 0; i < k; i++) {
      FeatureLabel c = lineInfos.get(i);
      FeatureLabel p = lineInfos.get(i - 1);
      FeatureLabel n = lineInfos.get(i + 1);
      if (c.answer().length() > 1 && c.answer().charAt(1) == '-') {
        String base = c.answer().substring(2, c.answer().length());
        String pBase = (p.answer().length() > 2 ? p.answer().substring(2, p.answer().length()) : p.answer());
        String nBase = (n.answer().length() > 2 ? n.answer().substring(2, n.answer().length()) : n.answer());
        char prefix = c.answer().charAt(0);
        char pPrefix = (p.answer().length() > 0) ? p.answer().charAt(0) : ' ';
        char nPrefix = (n.answer().length() > 0) ? n.answer().charAt(0) : ' ';
        boolean isStartAdjacentSame = base.equals(pBase) &&
          (prefix == 'B' || prefix == 'S' || pPrefix == 'E' || pPrefix == 'S');
        boolean isEndAdjacentSame = base.equals(nBase) &&
          (prefix == 'E' || prefix == 'S' || nPrefix == 'B' || pPrefix == 'S');
        boolean isFirst = (!base.equals(pBase)) || c.answer().charAt(0) == 'B';
        boolean isLast = (!base.equals(nBase)) || n.answer().charAt(0) == 'B';
        switch (how) {
        case 0:
          if (isStartAdjacentSame) {
            newAnswers[i] = intern("B-" + base);
          } else {
            newAnswers[i] = intern("I-" + base);
          }
          break;
        case 1:
          if (isFirst) {
            newAnswers[i] = intern("B-" + base);
          } else {
            newAnswers[i] = intern("I-" + base);
          }
          break;
        case 2:
          if (isEndAdjacentSame) {
            newAnswers[i] = intern("E-" + base);
          } else {
            newAnswers[i] = intern("I-" + base);
          }
          break;
        case 3:
          if (isLast) {
            newAnswers[i] = intern("E-" + base);
          } else {
            newAnswers[i] = intern("I-" + base);
          }
          break;
        case 4:
          newAnswers[i] = intern("I-" + base);
          break;
        case 5:
          if (isFirst && isLast) {
            newAnswers[i] = intern("S-" + base);
          } else if ((!isFirst) && isLast) {
            newAnswers[i] = intern("E-" + base);
          } else if (isFirst && (!isLast)) {
            newAnswers[i] = intern("B-" + base);
          } else {
            newAnswers[i] = intern("I-" + base);
          }
        }
      } else {
        newAnswers[i] = c.answer();
      }
    }
    for (int i = 0; i < k; i++) {
      FeatureLabel c = lineInfos.get(i);
      c.setAnswer(newAnswers[i]);
    }
  }
  
  private FeatureLabel makeFeatureLabel(String line) {
    FeatureLabel wi = new FeatureLabel();
    // wi.line = line;
    String[] bits = line.split("\\s+");
    switch (bits.length) {
    case 0:
    case 1:
      wi.setWord(BOUNDARY);
      wi.setAnswer(OTHER);
      break;
    case 2:
      wi.setWord(bits[0]);
      wi.setAnswer(bits[1]);
      break;
    case 3:
      wi.setWord(bits[0]);
      wi.setTag(bits[1]);
      wi.setAnswer(bits[2]);
      break;
    case 4:
      wi.setWord(bits[0]);
      wi.setTag(bits[1]);
      wi.put("chunk", bits[2]);
      wi.setAnswer(bits[3]);
      break;
    case 5:
      if (flags.useLemmaAsWord) {
        wi.setWord(bits[1]);
      } else {
        wi.setWord(bits[0]);
        }
      wi.put("lemma", bits[1]);
      wi.setTag(bits[2]);
      wi.put("chunk", bits[3]);
      wi.setAnswer(bits[4]);
      break;
    default:
      throw new RuntimeIOException("Unexpected input (many fields): " + line);
    }
    wi.put("origAnswer", wi.answer());
    // This collapses things to do neither iob1 or iob2 but just IO. Remove?
    // if (wi.answer().length() > 1 && wi.answer().charAt(1) == '-' && !flags.useFourWayEntitySubclassification) {
    //  wi.setAnswer("I-" + wi.answer().substring(2));
    // }
    return wi;
  }

  private String intern(String s) {
    if (flags.intern) {
      return s.intern();
    } else {
      return s;
    }
  }
  
  /** Return the marking scheme to IOB1 marking, regardless of what it was. */
  private void deEndify(List<FeatureLabel> lineInfos) {
    if (flags.retainEntitySubclassification) {
      return;
    }
    lineInfos = new PaddedList<FeatureLabel>(lineInfos, new FeatureLabel());
    int k = lineInfos.size();
    String[] newAnswers = new String[k];
    for (int i = 0; i < k; i++) {
      FeatureLabel c = lineInfos.get(i);
      FeatureLabel p = lineInfos.get(i - 1);
      if (c.answer().length() > 1 && c.answer().charAt(1) == '-') {
        String base = c.answer().substring(2);
        String pBase = (p.answer().length() <= 2 ? p.answer() : p.answer().substring(2));
        boolean isSecond = (base.equals(pBase));
        boolean isStart = (c.answer().charAt(0) == 'B' || c.answer().charAt(0) == 'S');
        if (isSecond && isStart) {
          newAnswers[i] = intern("B-" + base);
        } else {
          newAnswers[i] = intern("I-" + base);
        }
      } else {
        newAnswers[i] = c.answer();
      }
    }
    for (int i = 0; i < k; i++) {
      FeatureLabel c = lineInfos.get(i);
      c.setAnswer(newAnswers[i]);
    }
  }

  
  /** 
   * @param doc The document: A List of FeatureLabel
   * @param out Where to send the answers to
   */
  public void printAnswers(List<FeatureLabel> doc, PrintWriter out) {
    boolean tagsMerged = flags.mergeTags;
    boolean useHead = flags.splitOnHead;

    if ( ! "iob1".equalsIgnoreCase(flags.entitySubclassification)) {
      deEndify(doc);
    }
    String prevGold = "";
    String prevGuess = "";
    
    for (FeatureLabel fl : doc) {
      String word = fl.word();
      if (word == BOUNDARY) {
        out.println();
      } else {
        String gold = fl.getString("origAnswer");
        String guess = fl.answer();
        // System.err.println(fl.word() + "\t" + fl.goldAnswer() + "\t" + fl.answer());
        if (false) {
          // chris aug 2005
          // this bit of code was here, and it appears like it would
          // always mark the first of an entity sequence as B-, i.e., 
          // IOB2, but CoNLL uses IOB1, which only marks with B- when two
          // entities are adjacent, an annotation we just lose on.
          // now just record unmucked with origAnswer so can't need to do this
          if ( ! gold.equals(OTHER) && gold.length() >= 2) {
            if ( ! gold.substring(2).equals(prevGold)) {
              gold = "B-" + gold.substring(2);
            }
            prevGold = gold.substring(2);
          }
          if ( ! guess.equals(OTHER) && guess.length() >= 2) {
            if ( ! guess.substring(2).equals(prevGuess)) {
              guess = "B-" + guess.substring(2);
            }
            prevGuess = guess;
          }
        }
        String pos = fl.tag();
        String chunk = fl.getString("chunk");
        out.println(fl.word() + "\t" + pos + "\t" + chunk + "\t" +
                    gold + "\t" + guess);
      }
    }
  }

  /** Count some stats on what occurs in a file.
   */
  public static void main(String[] args) throws IOException, ClassNotFoundException {
//     CoNLLDocumentReaderAndWriter f = new CoNLLDocumentReaderAndWriter();
//     int numTokens = 0;
//     int numEntities = 0;
//     String lastAnsBase = "";
//     List<FeatureLabel> ll = f.processDocument(args[0]);
//     for (FeatureLabel fl : ll) {
//       // System.out.println("FL " + (++i) + " was " + fl);
//       if (fl.word().equals(BOUNDARY)) {
//         continue;
//       }
//       String ans = fl.answer();
//       String ansBase;
//       String ansPrefix;
//       String[] bits = ans.split("-");
//       if (bits.length == 1) {
//         ansBase = bits[0];
//         ansPrefix = "";
//       } else {
//         ansBase = bits[1];
//         ansPrefix = bits[0];
//       }
//       numTokens++;
//       if (ansBase.equals("O")) {
//       } else if (ansBase.equals(lastAnsBase)) {
//         if (ansPrefix.equals("B")) {
//           numEntities++;
//         }
//       } else {
//         numEntities++;
//       }
//     }
//     System.out.println("File " + args[0] + " has " + numTokens + 
//                        " tokens and " + numEntities + " entities.");
  } // end main

} // end class CoNLLDocumentReaderAndWriter
