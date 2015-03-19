package alteredu.stanford.nlp.sequences;

import alteredu.stanford.nlp.ling.FeatureLabel;
import alteredu.stanford.nlp.ling.FeatureLabel;
import alteredu.stanford.nlp.objectbank.IteratorFromReaderFactory;
import alteredu.stanford.nlp.process.InvertiblePTBTokenizer;
import alteredu.stanford.nlp.objectbank.XMLBeginEndIterator;
import alteredu.stanford.nlp.process.WordToSentenceProcessor;
import alteredu.stanford.nlp.util.StringUtils;

import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Jenny Finkel
 */
public class TrueCasingDocumentReaderAndWriter implements DocumentReaderAndWriter {

  public void init(SeqClassifierFlags flags) {}

  private static Pattern sgml = Pattern.compile("<[^>]*>");
  private static WordToSentenceProcessor wts = new WordToSentenceProcessor();

  private static Pattern allLower = Pattern.compile("[^A-Z]*?[a-z]+[^A-Z]*?");
  private static Pattern allUpper = Pattern.compile("[^a-z]*?[A-Z]+[^a-z]*?");
  private static Pattern startUpper = Pattern.compile("[A-Z].*");

  public static Set knownWords = null;

  public static boolean known(String s) {
    return knownWords.contains(s.toLowerCase());
  }

  public Iterator<List<FeatureLabel>> getIterator(Reader r) {

    List<List<FeatureLabel>> documents = new ArrayList();

    String s = StringUtils.slurpReader(r);    

    Set<String> wordsSeenOnce = new HashSet();
    Set<String> wordsSeenMultiple = new HashSet();
      
    XMLBeginEndIterator xmlIter = new XMLBeginEndIterator(new StringReader(s), "TEXT");      
    while (xmlIter.hasNext()) {
      InvertiblePTBTokenizer ptb = new InvertiblePTBTokenizer(new StringReader((String)xmlIter.next()));
        
      List<FeatureLabel> document = new ArrayList();
      Set<String> words = new HashSet();

      while (ptb.hasNext()) {
        FeatureLabel w = ptb.next();
        words.add(w.word().toLowerCase());
        Matcher m = sgml.matcher(w.word());
        if (m.matches()) {
          if (document.size() > 0) {
            documents.addAll(wts.process(document));
            document = new ArrayList();
          }
          continue;
        }
        document.add(w);
      }
      if (document.size() > 0) {
        documents.addAll(wts.process(document));
      }

      for (String word : words) {
        if (wordsSeenMultiple.contains(word)) {
          continue;
        } else if (wordsSeenOnce.contains(word)) {
          wordsSeenOnce.remove(word);
          wordsSeenMultiple.add(word);
        } else {
          wordsSeenOnce.add(word);
        }
      }      

    }

    xmlIter = new XMLBeginEndIterator(new StringReader(s), "TXT");      
    while (xmlIter.hasNext()) {
      InvertiblePTBTokenizer ptb = new InvertiblePTBTokenizer(new StringReader((String)xmlIter.next()));

      List<FeatureLabel> document = new ArrayList();
      Set<String> words = new HashSet();

      while (ptb.hasNext()) {
        FeatureLabel w = ptb.next();
        words.add(w.word().toLowerCase());
        Matcher m = sgml.matcher(w.word());
        if (m.matches()) {
          if (document.size() > 0) {
            documents.addAll(wts.process(document));
            document = new ArrayList();
          }
          continue;
        }
        document.add(w);
      }
      if (document.size() > 0) {
        documents.addAll(wts.process(document));
      }

      for (String word : words) {
        if (wordsSeenMultiple.contains(word)) {
          continue;
        } else if (wordsSeenOnce.contains(word)) {
          wordsSeenOnce.remove(word);
          wordsSeenMultiple.add(word);
        } else {
          wordsSeenOnce.add(word);
        }
      }      

    }

    knownWords = wordsSeenMultiple;
    knownWords.addAll(wordsSeenOnce);
    wordsSeenMultiple = null;

    List<List<FeatureLabel>> docs = new ArrayList();

    for (List<FeatureLabel> document : documents) {
      System.err.println(document);
      List<FeatureLabel> doc = new ArrayList();
      int pos = 0;
      for (FeatureLabel w : document) {
        FeatureLabel wi = new FeatureLabel();

        Matcher lowerMatcher = allLower.matcher(w.word());

        if (lowerMatcher.matches()) {
          wi.setAnswer("LOWER");
        } else {
          Matcher upperMatcher = allUpper.matcher(w.word());
          if (upperMatcher.matches()) {
            wi.setAnswer("UPPER");
          } else {
            Matcher startUpperMatcher = startUpper.matcher(w.word());
            if (startUpperMatcher.matches()) {
              wi.setAnswer("INIT_UPPER");
            } else {
              wi.setAnswer("O");
            }
          }
        }
        
        wi.setWord(w.word().toLowerCase());
        wi.put("unknown", (wordsSeenOnce.contains(w.word().toLowerCase()) ? "true" : "false"));
        wi.put("position", pos + "");
        if (wi.get("unknown").equals("true")) {
          System.err.println(wi.word()+" :: "+wi.get("unknown")+" :: "+wi.get("position"));
        }
        doc.add(wi);
        pos++;
      }
      System.err.println();
      docs.add(doc);
    }
    return docs.iterator();
  }

  public void printAnswers(List<FeatureLabel> doc, PrintWriter out) {
    
    for (FeatureLabel wi : doc) {
      String w = wi.word();
      
      if (wi.answer().equals("UPPER")) {
        w = w.toUpperCase();
      } else if (wi.answer().equals("LOWER")) {
        w = w.toLowerCase();
      } else if (wi.answer().equals("INIT_UPPER")) {
        w = w.substring(0,1).toUpperCase()+w.substring(1);
      }
      
      String prev = (String) wi.get("_prevSGML");
      String after = (String) wi.get("_afterSGML");
      out.print(prev + w + after);
    }
  }

}
