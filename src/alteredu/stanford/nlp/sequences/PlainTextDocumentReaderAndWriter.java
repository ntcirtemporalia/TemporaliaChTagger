package alteredu.stanford.nlp.sequences;

import alteredu.stanford.nlp.ling.FeatureLabel;
import alteredu.stanford.nlp.objectbank.IteratorFromReaderFactory;
import alteredu.stanford.nlp.process.InvertiblePTBTokenizer;
import alteredu.stanford.nlp.process.WordToSentenceProcessor;
import alteredu.stanford.nlp.util.StringUtils;
import alteredu.stanford.nlp.util.AbstractIterator;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * @author Jenny Finkel
 */
public class PlainTextDocumentReaderAndWriter implements DocumentReaderAndWriter {

  private SeqClassifierFlags flags = null;
  
  public void init(SeqClassifierFlags flags) { 
    this.flags = flags;
  }

  private static Pattern sgml = Pattern.compile("<[^>]*>");
  private static WordToSentenceProcessor wts = new WordToSentenceProcessor();

  public Iterator<List<FeatureLabel>> getIterator(Reader r) {
    InvertiblePTBTokenizer ptb = new InvertiblePTBTokenizer(r);
    List firstSplit = new ArrayList();
    List d = new ArrayList();
    
    while (ptb.hasNext()) {
      FeatureLabel w = ptb.next();
      Matcher m = sgml.matcher(w.word());
      if (m.matches()) {
        if (d.size() > 0) {
          firstSplit.add(d);
          d = new ArrayList();
        }
        firstSplit.add(w);
        continue;
      }
      d.add(w);
    }
    if (d.size() > 0) {
      firstSplit.add(d);
    }
    
    List secondSplit = new ArrayList();
    for (Iterator iter = firstSplit.iterator(); iter.hasNext();) {
      Object o = iter.next();
      if (o instanceof List) {
        secondSplit.addAll(wts.process((List) o));
      } else {
        secondSplit.add(o);
      }
    }
    
    String prevTags = "";
    FeatureLabel lastWord = null;
    
    List<List<FeatureLabel>> documents = new ArrayList<List<FeatureLabel>>();
    
    boolean first = true;
    
    for (Iterator iter = secondSplit.iterator(); iter.hasNext();) {
      Object o = iter.next();
      if (o instanceof List) {
        List doc = (List) o;
        List document = new ArrayList();
        int pos = 0;
        for (Iterator wordIter = doc.iterator(); wordIter.hasNext(); pos++) {      
          FeatureLabel w = (FeatureLabel) wordIter.next();
          FeatureLabel wi = new FeatureLabel();
          wi.setWord(w.word());
          wi.put("before", w.before());
          wi.put("current", w.current());
          wi.put("after", w.after());
          wi.put("position", pos + "");
          wi.put("startPosition", w.get("startPosition"));
          wi.put("endPosition",w.get("endPosition"));
          if (first && prevTags.length() > 0) {
            wi.put("_prevSGML", prevTags);
          }
          first = false;
          lastWord = wi;
          document.add(wi);
        }
        documents.add(document);
      } else {
        //String tag = ((Word) o).word();
        FeatureLabel word = (FeatureLabel)o;
        String tag = word.before()+word.current();
        if (first) {
          prevTags = tag;
        } else {
          String t = (String) lastWord.get("_afterSGML");
          tag = t + tag;
          lastWord.put("_afterSGML", tag);
        }
      }
    }
    return documents.iterator();
  }
  
  public static String getAnswers(List<FeatureLabel> l) {

    StringBuffer sb = new StringBuffer();
    for (FeatureLabel wi : l) {
      String prev = (String) wi.get("_prevSGML");
      String after = (String) wi.get("_afterSGML");
      sb.append(prev + wi.word() + "/" + wi.answer() + after + " ");
    }

    return sb.toString();
  }

  public void printAnswers(List<FeatureLabel> l) {
    PrintWriter pw = new PrintWriter(System.out);
    printAnswers(l, pw);
    pw.flush();
    pw.close();
  }

  public void printAnswers(List<FeatureLabel> l, PrintWriter out) {

    for (FeatureLabel wi : l) {
      String prev = (String) wi.get("_prevSGML");
      String after = (String) wi.get("_afterSGML");
      out.print(prev + wi.word() + "/" + wi.answer() + after + " ");
    }
  }

  public static String getAnswersXML(List<FeatureLabel> l) {
    StringWriter sw = new StringWriter();
    printAnswersXML(l, new PrintWriter(sw));
    sw.flush();
    return sw.toString();
  }

  public static void printAnswersXML(List<FeatureLabel> l) {
    PrintWriter pw = new PrintWriter(System.out);
    printAnswersXML(l, pw);
    pw.flush();
    pw.close();
  }

  public static void printAnswersXML(List<FeatureLabel> doc, PrintWriter out) {
    int num = 0;
    for (Iterator wordIter = doc.iterator(); wordIter.hasNext();) {
      FeatureLabel wi = (FeatureLabel) wordIter.next();
      String prev = (String) wi.get("_prevSGML");
      out.print(prev);
      StringBuffer tag = new StringBuffer();
      tag.append("<wi num=");
      //tag.append(wi.get("position"));
      tag.append(num++);
      tag.append(" entity=");
      tag.append(wi.answer());
      tag.append(">");
      tag.append(wi.word());
      tag.append("</wi>");
      out.print(tag);
      String after = (String) wi.get("_afterSGML");
      out.println(after);
    }
  }

  public static String getAnswersInlineXML(List<FeatureLabel> l) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    printAnswersInlineXML(l, pw);
    pw.flush();
    return sw.toString();
  }

  public void printAnswersInlineXML(List<FeatureLabel> l) {
    PrintWriter pw = new PrintWriter(System.out);
    printAnswersInlineXML(l, pw);
    pw.flush();
    pw.close();
  }

  public static void printAnswersInlineXML(List<FeatureLabel> doc, PrintWriter out) {
    String prevTag = "O";
    for (Iterator wordIter = doc.iterator(); wordIter.hasNext();) {
      FeatureLabel wi = (FeatureLabel) wordIter.next();
      String prev = (String) wi.get("_prevSGML");
      out.print(prev);
      if (prev.length() > 0) {
        prevTag = "O";
      }
      String tag = wi.answer();
      if (!tag.equals(prevTag)) {
        if (!prevTag.equals("O") && !tag.equals("O")) {
          out.print("</" + prevTag + ">" + wi.get("before") + "<" + tag + ">");
        } else if (!prevTag.equals("O")) {
          out.print("</" + prevTag + ">" + wi.get("before"));
        } else if (!tag.equals("O")) {
          out.print(wi.get("before") + "<" + tag + ">");
        }
      } else {
        out.print(wi.get("before"));
      }
      out.print(wi.get("current"));
      String after = (String) wi.get("_afterSGML");
      if (!tag.equals("O") && (!wordIter.hasNext() || after.length() > 0)) {
        out.print("</" + tag + ">");
        prevTag = "O";
      } else {
        prevTag = tag;
      }
      out.print(after);
    }
  }

}
