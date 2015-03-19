// AbstractSequenceClassifier -- a framework for probabilistic sequence models.
// Copyright (c) 2002-2006 The Board of Trustees of 
// The Leland Stanford Junior University. All Rights Reserved.
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// For more information, bug reports, fixes, contact:
//    Christopher Manning
//    Dept of Computer Science, Gates 1A
//    Stanford CA 94305-9010
//    USA
//    java-nlp-support@lists.stanford.edu
//    http://nlp.stanford.edu/downloads/crf-classifier.shtml

package alteredu.stanford.nlp.ie;

import alteredu.stanford.nlp.ling.*;
import alteredu.stanford.nlp.objectbank.ObjectBank;
import alteredu.stanford.nlp.objectbank.ResettableReaderIteratorFactory;
import alteredu.stanford.nlp.process.Function;
import alteredu.stanford.nlp.util.Index;
import alteredu.stanford.nlp.util.Timing;
import alteredu.stanford.nlp.util.StringUtils;
import alteredu.stanford.nlp.sequences.FeatureFactory;
import alteredu.stanford.nlp.sequences.DocumentReaderAndWriter;
import alteredu.stanford.nlp.sequences.PlainTextDocumentReaderAndWriter;
import alteredu.stanford.nlp.sequences.SeqClassifierFlags;
import alteredu.stanford.nlp.sequences.ObjectBankWrapper;
import alteredu.stanford.nlp.sequences.TrueCasingDocumentReaderAndWriter;
import alteredu.stanford.nlp.sequences.KBestSequenceFinder;
import alteredu.stanford.nlp.sequences.SequenceModel;
import alteredu.stanford.nlp.sequences.SequenceSampler;
import alteredu.stanford.nlp.stats.Counter;
import alteredu.stanford.nlp.stats.Sampler;

import java.io.*;
import java.util.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.zip.GZIPInputStream;

/** This class provides common functionality for (probabilistic) sequence
 *  models.  It is a superclass of our CMM and CRF sequence classifiers, 
 *  and is even used in the (deterministic) NumberSequenceClassifier.
 *  See implementing classes for more information.
 *
 *  @author Jenny Finkel
 *  @author Dan Klein
 *  @author Christopher Manning
 *  @author Dan Cer
 */
public abstract class AbstractSequenceClassifier implements Function {

  public static final String JAR_CLASSIFIER_PATH = "/classifiers/";

  public SeqClassifierFlags flags;
  public Index<String> classIndex;  // = null;
  protected DocumentReaderAndWriter readerAndWriter = null;
  public FeatureFactory featureFactory;
  protected FeatureLabel pad;
  public int windowSize;

  protected Set<String> knownLCWords = new HashSet<String>();

  /** This does nothing.  An implementing class should call
   *  init() in its constructor.
   */
  public AbstractSequenceClassifier() {
  }

  protected void init(Properties props) {
    SeqClassifierFlags newFlags = new SeqClassifierFlags();
    newFlags.setProperties(props);
    init(newFlags);
  }

  protected void init(SeqClassifierFlags flags) {
    this.flags = flags;
    pad = new FeatureLabel();
    windowSize = flags.maxLeft + 1;
    try {
      featureFactory = (FeatureFactory) Class.forName(flags.featureFactory).newInstance();
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e.getMessage());
    }
    reinit();
  }

  /** This method should be called after there have been changes to the
   *  flags (SeqClassifierFlags) variable, such as after deserializing
   *  a classifier.  It is called inside the loadClassifier methods.
   *  It assumes that the flags variable and the pad
   *  variable exist, but reinitializes things like the pad variable,
   *  featureFactory and readerAndWriter based on the flags.
   *  <p>
   *  <i>Implementation note:</i> At the moment this variable doesn't
   *  set windowSize or featureFactory, since they are being serialized
   *  separately in the
   *  file, but we should probably stop serializing them and just
   *  reinitialize them from the flags?
   */
  protected void reinit() {
    pad.setAnswer(flags.backgroundSymbol);
    pad.setGoldAnswer(flags.backgroundSymbol);

    try {
      readerAndWriter = (DocumentReaderAndWriter) Class.forName(flags.readerAndWriter).newInstance();
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e.getMessage(), e);
    }
    readerAndWriter.init(flags);
    featureFactory.init(flags);
  }


  public String backgroundSymbol() {
    return flags.backgroundSymbol;
  }

  public Set<String> labels() {
    return new HashSet<String>(classIndex.objectsList());
  }


  /**
   * Classify a {@link Sentence}.
   *
   * @param sentence The {@link Sentence} to be classified.
   * @return The classified {@link Sentence}, where the classifier output for
   * each token is stored in its "answer" field.
   */
  public List<FeatureLabel> testSentence(List<? extends HasWord> sentence) {
    List<FeatureLabel> document = new ArrayList<FeatureLabel>();
    int i = 0;
    for (HasWord word : sentence) {
      FeatureLabel wi = new FeatureLabel();
      wi.setWord(word.word());
      wi.put("position", Integer.toString(i));
      wi.setAnswer(backgroundSymbol());
      document.add(wi);
      i++;
    }
    ObjectBankWrapper wrapper = new ObjectBankWrapper(flags, null, knownLCWords);
    wrapper.processDocument(document);

    test(document);

    return document;
  }

  public SequenceModel getSequenceModel(List<FeatureLabel> doc) {
    throw new UnsupportedOperationException();
  }

  public Sampler<List<FeatureLabel>> getSampler(final List<FeatureLabel> input) {
    return new Sampler<List<FeatureLabel>>() {
      SequenceModel model = getSequenceModel(input);
      SequenceSampler sampler = new SequenceSampler();
      public List<FeatureLabel> drawSample() {
        int[] sampleArray = sampler.bestSequence(model);
        List<FeatureLabel> sample = new ArrayList<FeatureLabel>();
        int i=0;
        for (FeatureLabel word : input) {
          FeatureLabel newWord = new FeatureLabel(word);
          newWord.set("answer", classIndex.get(sampleArray[i++]));
          sample.add(newWord);
        }
        return sample;
      }
    };
  }


  public Counter<List<FeatureLabel>> testKBest(List<FeatureLabel> doc, String answerField, int k) {

    if (doc.isEmpty()) {
      return new Counter<List<FeatureLabel>>();
    }

    // i'm sorry that this is so hideous - JRF
    ObjectBankWrapper obw = new ObjectBankWrapper(flags, null, knownLCWords);
    doc = obw.processDocument(doc);

    SequenceModel model = getSequenceModel(doc);

    KBestSequenceFinder tagInference = new KBestSequenceFinder();
    Counter<int[]> bestSequences = tagInference.kBestSequences(model,k);

    Counter<List<FeatureLabel>> kBest = new Counter<List<FeatureLabel>>();

    for (int[] seq : bestSequences.keySet()) {
      List<FeatureLabel> kth = new ArrayList<FeatureLabel>();
      int pos = model.leftWindow();
      for (FeatureLabel fi : doc) {
        FeatureLabel newFL = new FeatureLabel(fi);
        String guess = classIndex.get(seq[pos]);
        fi.remove(FeatureLabel.ANSWER_KEY); // because fake answers will get added during testing
        newFL.set(answerField, guess);
        pos++;
        kth.add(newFL);
      }
      kBest.setCount(kth, bestSequences.getCount(seq));
    }

    return kBest;
  }


  /**
   * Classify a List of FeatureLabels using a TrueCasingDocumentReader.
   *
   * @param sentence a list of featureLabels to be classifierd
   * @return The classified list}.
   */
  public List<FeatureLabel> testSentenceWithCasing(List<FeatureLabel> sentence) {
    List<FeatureLabel> document = new ArrayList<FeatureLabel>();
    int i = 0;
    for (FeatureLabel word : sentence) {
      FeatureLabel wi = new FeatureLabel();
      if (readerAndWriter instanceof TrueCasingDocumentReaderAndWriter) {
        wi.setWord(word.word().toLowerCase());
        if (flags.useUnknown) {
          wi.put("unknown", (TrueCasingDocumentReaderAndWriter.known(wi.word()) ? "false" : "true"));
          //System.err.println(wi.word()+" : "+wi.get("unknown"));
        }
      } else {
        wi.setWord(word.word());
      }
      wi.put("position", Integer.toString(i));
      wi.setAnswer(backgroundSymbol());
      document.add(wi);
      i++;
    }
    test(document);
    i = 0;
    for (FeatureLabel wi : document) {
      FeatureLabel word = sentence.get(i);
      if (flags.readerAndWriter.equalsIgnoreCase("edu.stanford.nlp.sequences.TrueCasingDocumentReader")) {
        String w = word.word();
        if (wi.answer().equals("INIT_UPPER") || wi.get("position").equals("0")) {
          w = w.substring(0,1).toUpperCase()+w.substring(1).toLowerCase();
        } else if (wi.answer().equals("LOWER")) {
          w = w.toLowerCase();
        } else if (wi.answer().equals("UPPER")) {
          w = w.toUpperCase();
        }
        word.setWord(w);
      } else {
        word.setNER(wi.answer());
      }
      i++;
    }
    return sentence;
  }

   /**
   * Classify a {@link Sentence}.
   *
   * @param sentences The sentence(s) to be classified.
   * @return {@link List} of classified {@link Sentence}s.
   */
  public List<List<FeatureLabel>> testSentences(String sentences) {
    DocumentReaderAndWriter oldRW = readerAndWriter;
    readerAndWriter = new PlainTextDocumentReaderAndWriter();
    ObjectBank<List<FeatureLabel>> documents = makeObjectBank(new BufferedReader(new StringReader(sentences)), true);
    List<List<FeatureLabel>> result = new ArrayList<List<FeatureLabel>>();

    for (List<FeatureLabel> document : documents) {
      test(document);

      List<FeatureLabel> sentence = new ArrayList<FeatureLabel>();
      for (FeatureLabel wi : document) {
        // TaggedWord word = new TaggedWord(wi.word(), wi.answer());
        // sentence.add(word);
        sentence.add(wi);
      }
      result.add(sentence);
    }
    readerAndWriter = oldRW;
    return result;
  }

   /**
   * Classify a {@link Sentence}.
   *
   * @param filename Contains the sentence(s) to be classified.
   * @return {@link List} of classified {@link Sentence}s.
   */
  public List<List<FeatureLabel>> testFile(String filename) {
	  try {
		  return testReader(new FileReader(filename));
	  } catch (IOException e) {
		  throw new RuntimeException(e);
	  }
  }
  
  /**
   * Classify a {@link Sentence} or multiple sentences.
   * 
   * @param reader Provides the sentence(s) to be classified
   * @return {@link List} of classified {@link Sentence}s.
   */
  public List<List<FeatureLabel>> testReader(Reader reader) {
	  
    BufferedReader br = null;
    try {
      DocumentReaderAndWriter oldRW = readerAndWriter;
      readerAndWriter = new PlainTextDocumentReaderAndWriter();
      br = new BufferedReader(reader);
      ObjectBank<List<FeatureLabel>> documents = makeObjectBank(br, true);
      List<List<FeatureLabel>> result = new ArrayList<List<FeatureLabel>>();

      for (List<FeatureLabel> document : documents) {
    	  
    	//-
        test(document);

        List<FeatureLabel> sentence = new ArrayList<FeatureLabel>();
        for (FeatureLabel wi : document) {
          sentence.add(wi);
        }
        result.add(sentence);
      }
      readerAndWriter = oldRW;
      return result;
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException ioe) {
        }
      }
    }
  }


  /**
   * Maps a String input to an XML-formatted rendition of applying NER to
   * the String.  Implements the Function interface.  Calls
   * testStringInlineXML(Stringa) [q.v.].
   */
  public Object apply(Object in) {
    return testStringInlineXML((String) in);
  }

  /**
   * Classify the contents of a {@link String}.  Plain text or XML is
   * expected and the {@link PlainTextDocumentReaderAndWriter} is used. Output
   * is in inline XML format (e.g. &lt;PERSON&gt;Bill Smith&lt;/PERSON&gt;
   * went to &lt;LOCATION&gt;Paris&lt;/LOCATION&gt; .)
   *
   * @param sentences The string to be classified
   * @return A {@link String} with annotated with classification
   *         information.
   */
  public String testStringInlineXML(String sentences) {
    DocumentReaderAndWriter tmp = readerAndWriter;
    readerAndWriter = new PlainTextDocumentReaderAndWriter();

    ObjectBank<List<FeatureLabel>> documents = makeObjectBank(new BufferedReader(new StringReader(sentences)), true);
    readerAndWriter = tmp;

    StringBuilder sb = new StringBuilder();
    for (List<FeatureLabel> doc : documents) {
      test(doc);
      sb.append(PlainTextDocumentReaderAndWriter.getAnswersInlineXML(doc));
    }
    return sb.toString();
  }

  /**
   * Classify the contents of a {@link String}.  Plain text or XML is
   * expected and the {@link PlainTextDocumentReaderAndWriter} is used. Output
   * is in XML format.
   *
   * @param sentences The string to be classified
   * @return A {@link String} with annotated with classification
   *         information.
   */
  public String testStringXML(String sentences) {
    DocumentReaderAndWriter tmp = readerAndWriter;
    readerAndWriter = new PlainTextDocumentReaderAndWriter();

    ObjectBank<List<FeatureLabel>> documents = makeObjectBank(new BufferedReader(new StringReader(sentences)), true);
    readerAndWriter = tmp;

    StringBuilder sb = new StringBuilder();
    for (List<FeatureLabel> doc : documents) {
      test(doc);
      sb.append(PlainTextDocumentReaderAndWriter.getAnswersXML(doc));
    }
    return sb.toString();
  }

  /**
   * Classify the contents of a {@link String}.  Plain text or XML is
   * expected and the {@link PlainTextDocumentReaderAndWriter} is used. Output
   * looks like: My/O name/O is/O Bill/PERSON Smith/PERSON ./O
   *
   * @param sentences The string to be classified
   * @return A {@link String} with annotated with classification
   *         information.
   */
  public String testString(String sentences) {
    DocumentReaderAndWriter tmp = readerAndWriter;
    readerAndWriter = new PlainTextDocumentReaderAndWriter();

    ObjectBank<List<FeatureLabel>> documents = makeObjectBank(new BufferedReader(new StringReader(sentences)), true);
    readerAndWriter = tmp;

    StringBuilder sb = new StringBuilder();
    for (List<FeatureLabel> doc : documents) {
      test(doc);
      sb.append(PlainTextDocumentReaderAndWriter.getAnswers(doc));
    }
    return sb.toString();
  }

  /**
   * ONLY USE IF LOADED A CHINESE WORD SEGMENTER!!!!!
   *
   * @param sentence The string to be classified
   * @return List of words
   */
  public List<String> segmentString(String sentence) {
    ObjectBank<List<FeatureLabel>> docs =
      makeObjectBank(new BufferedReader(new StringReader(sentence)));

    // @ cer  - previously, there was the following todo here:
    //
    //    TODO: use printAnswers(List<FeatureLabel> doc, PrintWriter pw)
    //    instead
    //
    // I went ahead and did the TODO. However, given that the TODO
    // was incredibly easy to do, I'm wondering if it was left
    // as a todo for a reason. For example,  I'm concerned that something
    // else bizarrely breaks if this method calls printAnswers, as the method
    // arguably should, instead of manually building up the output string,
    // as was being done before.
    //
    // In any case, by doing the TODO, I was able to improve the online
    // parser/segmenter since all of the wonderful post processing
    // stuff is now being done to the segmented strings.
    //
    // However, if anything I'm not aware of broke, please just shot me
    // an e-mail (cerd@cs.colorado.edu) and I will look into and fix
    // the problem asap.

    // Also...
    //
    // Using a temporary file for flags.testFile is not elegant
    // However, I think all more elegant solutions would require
    // touching more source files. Touching more source files
    // risks incurring the wrath of whoever regularly works-with
    // and/or 'owns' this part of the codebase.
    //
    // (...the testFile stuff is necessary for segmentation whitespace
    //  normalization)

    String oldTestFile = flags.testFile;
    try {
      File tempFile = File.createTempFile("segmentString", ".txt");
      tempFile.deleteOnExit();
      flags.testFile = tempFile.getPath();
      FileWriter tempWriter = new FileWriter(tempFile);
      tempWriter.write(sentence);
      tempWriter.close();
    } catch (IOException e) {
      System.err.println("Warning(segmentString): " +
         "couldn't create temporary file for flags.testFile");
      flags.testFile = "";
    }

    StringWriter stringWriter = new StringWriter();
    PrintWriter stringPrintWriter = new PrintWriter(stringWriter);
    for (List<FeatureLabel> doc : docs) { test(doc);
      readerAndWriter.printAnswers(doc, stringPrintWriter);
      stringPrintWriter.println();
    }
    stringPrintWriter.close();
    String segmented = stringWriter.toString();

    flags.testFile = oldTestFile;
    return Arrays.asList(segmented.split("\\s"));
  }

  /**
   * Classify the contents of {@link SeqClassifierFlags scf.testFile}.
   * The file should be in the format
   * expected based on {@link SeqClassifierFlags scf.documentReader}.
   *
   * @return A {@link List} of {@link List}s of classified
   *         {@link FeatureLabel}s where each
   *         {@link List} refers to a document/sentence.
   */
//   public ObjectBank<List<FeatureLabel>> test() {
//     return test(flags.testFile);
//   }

  /**
   * Classify the contents of a file.  The file should be in the format
   * expected based on {@link SeqClassifierFlags scf.documentReader} if the
   * file is specified in {@link SeqClassifierFlags scf.testFile}.  If the
   * file being read is from {@link SeqClassifierFlags scf.textFile} then
   * the {@link PlainTextDocumentReaderAndWriter} is used.
   *
   * @param filename The path to the specified file
   * @return A {@link List} of {@link List}s of classified {@link FeatureLabel}s where each
   *         {@link List} refers to a document/sentence.
   */
//   public ObjectBank<List<FeatureLabel>> test(String filename) {
//     // only for the OCR data does this matter
//     flags.ocrTrain = false;

//     ObjectBank<List<FeatureLabel>> docs = makeObjectBank(filename);
//     return testDocuments(docs);
//   }

  /**
   * Classify a {@link List} of {@link FeatureLabel}s.
   *
   * @param document A {@link List} of {@link FeatureLabel}s.
   * @return the same {@link List}, but with the elements annotated
   *         with their answers (with <code>setAnswer()</code>).
   */
  public abstract List<FeatureLabel> test(List<FeatureLabel> document);

  public void train() {
    train(flags.trainFile);
  }

  public void train(String filename) {
    // only for the OCR data does this matter
    flags.ocrTrain = true;
    train(makeObjectBank(filename));
  }

  public abstract void train(ObjectBank<List<FeatureLabel>> docs);

  public ObjectBank<List<FeatureLabel>> makeObjectBank(String filename) {
    BufferedReader in;
    try {
      if (flags.inputEncoding == null) {
        in = new BufferedReader(new FileReader(filename));
      } else {
        in = new BufferedReader(new InputStreamReader(new FileInputStream(filename), flags.inputEncoding));
      }
      return makeObjectBank(in);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
      return null;
    }
  }

  protected ObjectBank<List<FeatureLabel>> makeObjectBank(BufferedReader in) {
    return makeObjectBank(in, false);
  }

  /** Set up an ObjectBank that will allow one to iterate over a
   *  collection of documents obtained from the passed in Reader.
   *  Each document will be represented as a list of FeatureLabel.
   *  If the ObjectBank iterator() is called until hasNext() returns false,
   *  then the Reader will be read till end of file, but no
   *  reading is done at the time of this call.  Reading is done using the
   *  reading method specified in <code>flags.documentReader</code>,
   *  and for some reader choices, the column mapping given in
   *  <code>flags.map</code>.
   *
   * @param in      Input data
   * @param quietly Print less messages if this is true (use when calling
   *                it repeatedly on small bits of text)
   * @return The list of documents
   */
  protected ObjectBank<List<FeatureLabel>> makeObjectBank(BufferedReader in, boolean quietly) {
    if (!quietly) {
      System.err.print("Reading data using ");
      System.err.println(flags.readerAndWriter);
    }

    return new ObjectBankWrapper(flags, new ObjectBank<List<FeatureLabel>>(new ResettableReaderIteratorFactory(in), readerAndWriter), knownLCWords);
  }


  /**
   * Takes the file, reads it in, and prints out the likelihood of
   * each possible label at each point.
   *
   * @param filename The path to the specified file
   */
  public void printProbs(String filename) {
    // only for the OCR data does this matter
    flags.ocrTrain = false;

    ObjectBank<List<FeatureLabel>> docs = makeObjectBank(filename);
    printProbsDocuments(docs);
  }

  /**
   * Takes a {@link List} of documents and prints the likelihood
   * of each possible label at each point.
   *
   * @param documents A {@link List} of {@link List} of {@link FeatureLabel}s.
   */
  public void printProbsDocuments(ObjectBank<List<FeatureLabel>> documents) {
    for (List<FeatureLabel> doc : documents) {
      printProbsDocument(doc);
      System.out.println();
    }
  }

  public abstract void printProbsDocument(List<FeatureLabel> document);


  /** Load a test file, run the classifier on it, and then print the answers
   *  to stdout (with timing to stderr).  This uses the value of
   *  flags.documentReader to determine testFile format.
   *
   *  @param testFile The file to test on.
   */
  public void testAndWriteAnswers(String testFile) throws Exception {
    Timing timer = new Timing();
    ObjectBank<List<FeatureLabel>> documents = makeObjectBank(testFile);
    int numWords = 0;
    int numDocs = 0;
    for (List<FeatureLabel> doc : documents) {
      test(doc);
      numWords += doc.size();
      writeAnswers(doc);
      numDocs++;
    }
    long millis = timer.stop();
    double wordspersec = numWords / (((double) millis) / 1000);
    NumberFormat nf = new DecimalFormat("0.00"); // easier way!
    System.err.println(StringUtils.getShortClassName(this) +
                       " tagged " + numWords + " words in " + numDocs +
                       " documents at " + nf.format(wordspersec) +
                       " words per second.");
  }


  /** Load a test file, run the classifier on it, and then print the answers
   *  to stdout (with timing to stderr).  This uses the value of
   *  flags.documentReader to determine testFile format.
   *
   *  @param testFile The file to test on.
   */
  public void testAndWriteAnswersKBest(String testFile, int k) throws Exception {
    Timing timer = new Timing();
    ObjectBank<List<FeatureLabel>> documents = makeObjectBank(testFile);
    int numWords = 0;
    int numDocs = 0;
    List<FeatureLabel> doc = new ArrayList<FeatureLabel>();
    for (List<FeatureLabel> l : documents) {
      doc.addAll(l);
      numDocs++;
    }

    Counter<List<FeatureLabel>> kBest = testKBest(doc, "answer", k);
    numWords += doc.size();
    for (List<FeatureLabel> l : kBest.keySet()) {
      System.out.println(kBest.getCount(l));
      writeAnswers(l);
    }

    long millis = timer.stop();
    double wordspersec = numWords / (((double) millis) / 1000);
    NumberFormat nf = new DecimalFormat("0.00"); // easier way!
    System.err.println(this.getClass().getName()+" tagged " + numWords + " words in " + numDocs +
                       " documents at " + nf.format(wordspersec) +
                       " words per second.");
  }


  /** Write the classifications of the Sequence classifier out in a format
   *  determined by the DocumentReaderAndWriter used.
   */
  public void writeAnswers(List<FeatureLabel> doc) throws Exception {
    if (flags.lowerNewgeneThreshold) {
      return;
    }
    if (flags.numRuns <= 1) {
      PrintWriter out = new PrintWriter(System.out);
      readerAndWriter.printAnswers(doc, out);
      out.flush();
      System.out.println();
    }
  }

  public abstract void serializeClassifier(String serializePath);

  /**
   * Loads a classifier from the given input stream.
   */
  public void loadClassifierNoExceptions(BufferedInputStream in) {
    // load the classifier
    try {
      loadClassifier(in);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }

  }

  public void loadClassifier(InputStream in) throws IOException, ClassCastException, ClassNotFoundException {
    loadClassifier(in, null);
  }

  /** Load a classsifier from the specified input stream.
   *  The classifier is reinitialized from the flags serialized in the
   *  classifier.
   *
   * @param in The InputStream to load the serialized classifier from
   * @param props This Properties object will be used to update the SeqClassifierFlags which
   *               are read from the serialized classifier
   *
   * @throws IOException
   * @throws ClassCastException
   * @throws ClassNotFoundException
   */
  public abstract void loadClassifier(InputStream in, Properties props) throws IOException, ClassCastException, ClassNotFoundException;

  /**
   * Loads a classifier from the file specified by loadPath.  If loadPath
   * ends in .gz, uses a GZIPInputStream, else uses a regular FileInputStream.
   */
  public void loadClassifier(String loadPath) throws ClassCastException, IOException, ClassNotFoundException {
    loadClassifier(new File(loadPath));
  }

  public void loadClassifierNoExceptions(String loadPath) {
    loadClassifierNoExceptions(new File(loadPath));
  }

  public void loadClassifierNoExceptions(String loadPath, Properties props) {
    loadClassifierNoExceptions(new File(loadPath), props);
  }


  public void loadClassifier(File file) throws ClassCastException, IOException, ClassNotFoundException {
    loadClassifier(file, null);
  }
  /**
   * Loads a classifier from the file specified by loadPath.  If loadPath
   * ends in .gz, uses a GZIPInputStream, else uses a regular FileInputStream.
   */
  public void loadClassifier(File file, Properties props) throws ClassCastException, IOException, ClassNotFoundException {
    Timing.startDoing("Loading classifier from " + file.getAbsolutePath());
    BufferedInputStream bis;
    if (file.getName().endsWith(".gz")) {
      bis = new BufferedInputStream(new GZIPInputStream(new FileInputStream(file)));
    } else {
      bis = new BufferedInputStream(new FileInputStream(file));
    }
    loadClassifier(bis);
    bis.close();
    Timing.endDoing();
  }


  public void loadClassifierNoExceptions(File file) {
    loadClassifierNoExceptions(file, null);
  }

  public void loadClassifierNoExceptions(File file, Properties props) {
    try {
      loadClassifier(file, props);
    } catch (Exception e) {
      System.err.println("Error deserializing " + file.getAbsolutePath());
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * This function will load a classifier that is stored inside a jar file
   * (if it is so stored).  The classifier should be specified as its full
   * filename, but the path in the jar file (<code>/classifiers/</code>) is
   * coded in this class.  If the classifier is not stored in the jar file
   * or this is not run from inside a jar file, then this function will
   * throw a RuntimeException.
   *
   * @param modelName The name of the model file.  Iff it ends in .gz, then
   *             it is assumed to be gzip compressed.
   * @param props A Properties object which can override certain properties
   *             in the serialized file, such as the DocumentReaderAndWriter.
   *             You can pass in <code>null</code> to override nothing.
   */
  public void loadJarClassifier(String modelName, Properties props) {
    Timing.startDoing("Loading JAR-internal classifier " + modelName);
    try {
      InputStream is;
      is = this.getClass().getResourceAsStream(JAR_CLASSIFIER_PATH + modelName);
      if (modelName.endsWith(".gz")) {
        is = new GZIPInputStream(is);
      }
      is = new BufferedInputStream(is);
      loadClassifier(is, props);
      is.close();
      Timing.endDoing();
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

}
