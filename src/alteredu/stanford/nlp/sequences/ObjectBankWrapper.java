package alteredu.stanford.nlp.sequences;

import alteredu.stanford.nlp.ling.FeatureLabel;
import alteredu.stanford.nlp.objectbank.ObjectBank;
import alteredu.stanford.nlp.process.Americanize;
import alteredu.stanford.nlp.process.WordShapeClassifier;
import alteredu.stanford.nlp.util.AbstractIterator;
import alteredu.stanford.nlp.util.StringUtils;

import java.util.*;
import java.util.regex.Pattern;


/**
 * This class is used to wrap the ObjectBank used by the sequence
 * models and is where any sort of general processing, like the IOB mapping 
 * stuff and wordshape stuff, should go.
 * It checks the SeqClassifierFlags to decide what to do.
 * <p>
 * TODO: We should rearchitect this so that the FeatureFactory-specific
 * stuff is done by a callback to the relevant FeatureFactory.
 *
 * @author Jenny Finkel
 */
public class ObjectBankWrapper extends ObjectBank<List<FeatureLabel>> {

  private SeqClassifierFlags flags;
  private ObjectBank<List<FeatureLabel>> wrapped;
  private Set<String> knownLCWords;
  
  public ObjectBankWrapper(SeqClassifierFlags flags, ObjectBank<List<FeatureLabel>> wrapped, Set<String> knownLCWords) {
    super(null,null);
    this.flags = flags;
    this.wrapped = wrapped;
    this.knownLCWords = knownLCWords;
  }


  public Iterator<List<FeatureLabel>> iterator() {
    Iterator<List<FeatureLabel>> iter = new WrappedIterator(wrapped.iterator());
    
    // If using WordShapeClassifier, we have to make an extra pass through the
    // data before we really process it, so that we can build up the
    // database of known lower case words in the data.  We do that here.
    if ((flags.wordShape > WordShapeClassifier.NOWORDSHAPE) && (!flags.useShapeStrings)) {
      while (iter.hasNext()) { 
        List<FeatureLabel> doc = iter.next();
        for (FeatureLabel fl : doc) {
          String word = fl.word();
          if (word.length() > 0) {
            char ch = word.charAt(0);
            if (Character.isLowerCase(ch)) {
              knownLCWords.add(word);
            }
          }
        }
      }
      iter = new WrappedIterator(wrapped.iterator());
    }
    return iter;
  }

  private class WrappedIterator extends AbstractIterator<List<FeatureLabel>> {
    Iterator<List<FeatureLabel>> wrappedIter;
    Iterator<List<FeatureLabel>> spilloverIter;
    
    public WrappedIterator(Iterator<List<FeatureLabel>> wrappedIter) {
      this.wrappedIter = wrappedIter;
    }
    
    public boolean hasNext() { 
      return wrappedIter.hasNext() || 
        (spilloverIter != null && spilloverIter.hasNext()); 
    }

    public List<FeatureLabel> next() {
      if (spilloverIter == null || !spilloverIter.hasNext()) {
        List<FeatureLabel> doc = wrappedIter.next();
        List<List<FeatureLabel>> docs = new ArrayList<List<FeatureLabel>>();
        docs.add(doc);
        fixDocLengths(docs);
        spilloverIter = docs.iterator();
      }
        
      return processDocument(spilloverIter.next());
    }
  }
  
  public List<FeatureLabel> processDocument(List<FeatureLabel> doc) {
    if (flags.mergeTags) { mergeTags(doc); }
    if (flags.iobTags) { iobTags(doc); }
    doBasicStuff(doc);
    
    return doc;
  }

  private String intern(String s) {
    if (flags.intern) {
      return s.intern();
    } else {
      return s;
    }
  }


  private Pattern monthDayPattern = Pattern.compile("Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday|January|February|March|April|May|June|July|August|September|October|November|December", Pattern.CASE_INSENSITIVE);

  private String fix(String word) {
    if (flags.normalizeTerms || flags.normalizeTimex) {
      // Same case for days/months: map to lowercase
      if (monthDayPattern.matcher(word).matches()) {
        return word.toLowerCase();
      }
    }
    if (flags.normalizeTerms) {
      return Americanize.americanize(word, false);
    }
    return word;
  }

  
  private void doBasicStuff(List<FeatureLabel> doc) {
    int position = 0;
    for (FeatureLabel fl : doc) {

      // position in document
      fl.set("position", (position++));

      // word shape
      if ((flags.wordShape > WordShapeClassifier.NOWORDSHAPE) && (!flags.useShapeStrings)) {
        String s = intern(WordShapeClassifier.wordShape(fl.word(), flags.wordShape, knownLCWords));
        fl.setShape(s);
      }

      // normalizing and interning
      // was the following; should presumably now be 
      // if ("CTBSegDocumentReader".equalsIgnoreCase(flags.documentReader)) {
      if ("edu.stanford.nlp.wordseg.Sighan2005DocumentReaderAndWriter".equalsIgnoreCase(flags.readerAndWriter)) {
        // for Chinese segmentation, "word" is no use and ignore goldAnswer for memory efficiency. 
        fl.put("char",intern(fix((String)fl.get("char"))));
      } else {
        fl.setWord(intern(fix(fl.word())));
        fl.setGoldAnswer(fl.answer());
      }
    }
  }

  /**
   * Take a {@link List} of documents (which are themselves {@link List}s
   * of {@link FeatureLabel}s) and if any are longer than the length
   * specified by flags.maxDocSize split them up.  It tries to be smart
   * and split on sentence bounaries, hard-coded to the English-specific token
   * '.'.
   */
  private void fixDocLengths(List<List<FeatureLabel>> docs) {
    int maxSize = flags.maxDocSize;
    if (maxSize < 0) {
      return;
    }
    int size = docs.size();
    for (int i = 0; i < size; i++) {
      List<FeatureLabel> document = docs.get(i);
      List<List<FeatureLabel>> newDocuments = new ArrayList<List<FeatureLabel>>();
      while (document.size() > maxSize) {
        int splitIndex = 0;
        for (int j = maxSize; j > maxSize / 2; j--) {
          FeatureLabel wi = document.get(j);
          if (wi.word().equals(".")) {
            splitIndex = j + 1;
            break;
          }
        }
        if (splitIndex == 0) {
          splitIndex = maxSize;
        }
        List<FeatureLabel> newDoc = document.subList(0, splitIndex);
        newDocuments.add(newDoc);
        document = document.subList(splitIndex, document.size());
      }
      if ( ! document.isEmpty()) {
        newDocuments.add(document);
      }
      docs.remove(i);
      Collections.reverse(newDocuments);
      for (List<FeatureLabel> item : newDocuments) {
        docs.add(i, item);
      }
      i += newDocuments.size() - 1;
    }
  }

  
  private void iobTags(List<FeatureLabel> doc) {
    String lastTag = "";
    for (FeatureLabel wi : doc) {
      String answer = (String)wi.get(FeatureLabel.ANSWER_KEY);
      if (!answer.equals(flags.backgroundSymbol)) {
        int index = answer.indexOf('-');
        String prefix;
        String label;
        if (index < 0) {
          prefix = "";
          label = answer;
        } else {
          prefix = answer.substring(0,1);
          label = answer.substring(2);
        }

        if (!prefix.equals("B")) {
          if (!lastTag.equals(label)) {
            wi.setAnswer("B-" + label);
          } else {
            wi.setAnswer("I-" + label);
          }
        }
        lastTag = label;
      } else {
        lastTag = answer;
      }
    }
  }

  
  private void mergeTags(List<FeatureLabel> doc) {
    for (FeatureLabel wi : doc) {
      String answer = (String)wi.get(FeatureLabel.ANSWER_KEY);
      if (!answer.equals(flags.backgroundSymbol) && answer.indexOf('-') >= 0) {
        answer = answer.substring(2);
      }
      wi.setAnswer(answer);
    }
  }


  // all the other the crap from ObjectBank
  public boolean add(List<FeatureLabel> o) { return wrapped.add(o); }
  public boolean addAll(Collection<? extends List<FeatureLabel>> c) { return wrapped.addAll(c); }
  public void clear() { wrapped.clear(); }
  public void clearMemory() { wrapped.clearMemory(); }
  public boolean contains(List<FeatureLabel> o) { return wrapped.contains(o); }
  public boolean containsAll(Collection c) { return wrapped.containsAll(c); }
  public boolean isEmpty() { return wrapped.isEmpty(); }
  public void keepInMemory(boolean keep) { wrapped.keepInMemory(keep); }
  public boolean remove(List<FeatureLabel> o) { return wrapped.remove(o); }
  public boolean removeAll(Collection c) { return wrapped.removeAll(c); }
  public boolean retainAll(Collection c) { return wrapped.retainAll(c); }
  public int size() { return wrapped.size(); }
  public Object[] toArray() { return wrapped.toArray(); }
  public List<FeatureLabel>[] toArray(List<FeatureLabel>[] o) { return wrapped.toArray(o); }
}
