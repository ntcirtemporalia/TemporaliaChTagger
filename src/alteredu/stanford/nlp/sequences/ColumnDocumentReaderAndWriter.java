package alteredu.stanford.nlp.sequences;

import alteredu.stanford.nlp.ling.FeatureLabel;
import alteredu.stanford.nlp.objectbank.DelimitRegExIterator;
import alteredu.stanford.nlp.objectbank.IteratorFromReaderFactory;
import alteredu.stanford.nlp.process.Function;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.io.PrintWriter;
import java.io.Reader;


/**
 * DocumentReader for column format
 *
 * @author Jenny Finkel
 */
public class ColumnDocumentReaderAndWriter implements DocumentReaderAndWriter {

  private SeqClassifierFlags flags = null;
  private String[] map = null;
  private IteratorFromReaderFactory factory;
  
  public void init(SeqClassifierFlags flags) { 
    this.flags = flags;
    this.map = FeatureLabel.mapStringToArray(flags.map);
    factory = DelimitRegExIterator.getFactory("\n(\\s*\n)+", new ColumnDocParser());
  }
  
  public Iterator<List<FeatureLabel>> getIterator(Reader r) {
    return factory.getIterator(r);
  }

  private class ColumnDocParser implements Function<String,List<FeatureLabel>> {
    public List<FeatureLabel> apply(String doc) {
      
      List<FeatureLabel> words = new ArrayList<FeatureLabel>();
      
      String[] lines = doc.split("\n");
      
      for (int i = 0; i < lines.length; i++) {
        if (lines[i].trim().length() < 1) {
          continue;
        }
        String[] info = lines[i].split("\\s+");      
        FeatureLabel wi = new FeatureLabel(map, info);
        words.add(wi);
      }
      return words;
    }
  }
  
  public void printAnswers(List<FeatureLabel> doc, PrintWriter out) {
    for (FeatureLabel wi : doc) {
      String answer = wi.answer();
      String goldAnswer = wi.goldAnswer();
      out.println(wi.word() + "\t" + goldAnswer + "\t" + answer);
    }
    out.println();
  }

}
