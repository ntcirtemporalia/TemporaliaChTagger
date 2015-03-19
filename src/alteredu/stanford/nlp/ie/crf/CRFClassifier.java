// CRFClassifier -- a probabilistic (CRF) sequence model, mainly used for NER.
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

package alteredu.stanford.nlp.ie.crf;

import alteredu.stanford.nlp.ie.*;
import alteredu.stanford.nlp.io.IOUtils;
import alteredu.stanford.nlp.math.ArrayMath;
import alteredu.stanford.nlp.maxent.Convert;
import alteredu.stanford.nlp.optimization.*;
import alteredu.stanford.nlp.sequences.*;
import alteredu.stanford.nlp.util.Index;
import alteredu.stanford.nlp.util.PaddedList;
import alteredu.stanford.nlp.util.Pair;
import alteredu.stanford.nlp.util.StringUtils;
import alteredu.stanford.nlp.ling.FeatureLabel;
import alteredu.stanford.nlp.objectbank.ObjectBank;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Does Sequence Classification using a Conditional Random Field model.
 * The code has functionality for different document encodings, but when
 * using the standard <code>ColumnDocumentReaderAndWriter</code> for training
 * or testing models, input files are expected to
 * be one word per line with the columns indicating things like the word,
 * POS, chunk, and class.  When run on a file with <code>-textFile</code>,
 * the file is assumed to be plain English text (or perhaps HTML/XML),
 * and a reasonable attempt is made at tokenization by
 * <code>PlainTextDocumentReaderAndWriter</code>.
 * <p/>
 * <b>Typical usage</b>
 * <p>For running a trained model with a provided serialized classifier on a
 * text file: <p>
 * <code>
 * java -server -mx500m edu.stanford.nlp.ie.crf.CRFClassifier -loadClassifier
 * conll.ner.gz -textFile samplesentences.txt
 * </code><p>
 * When specifying all parameters in a properties file (train, test, or
 * runtime):<p>
 * <code>
 * java -server -mx1000m edu.stanford.nlp.ie.crf.CRFClassifier -prop propFile
 * </code><p>
 * To train and test a model from the command line:<p>
 * <code>java -mx1000m edu.stanford.nlp.ie.crf.CRFClassifier
 * -trainFile trainFile -testFile testFile -macro &gt; output </code>
 * <p/>
 * Features are defined by a {@link edu.stanford.nlp.sequences.FeatureFactory}.
 * {@link NERFeatureFactory} is used by default, and
 * you should look there for feature templates and properties or flags that
 * will cause certain features to be used when training an NER classifier.
 * There is also
 * a {@link ChineseFeatureFactory}, which is used for Chinese
 * word segmentation.
 * Features are specified either by a Properties file (which is the
 * recommended method) or on the command line.  The features are read into
 * a {@link SeqClassifierFlags} object, which the
 * user need not concern himself with unless he wishes to add new features.
 * <p/>
 * CRFClassifier may also be used programatically.  When creating a new
 * instance, you <i>must</i>
 * specify a properties file.  The other way to get a CRFClassifier is to
 * deserialize one via {@link CRFClassifier#getClassifier(String)}, which
 * returns a deserialized
 * classifier.  You may then tag sentences using either the assorted
 * <code>test</code> or <code>testSentence</code> methods.
 *
 * @author Jenny Finkel
 */


public class CRFClassifier extends AbstractSequenceClassifier {

  Index[] labelIndices;
  /** Parameter weights of the classifier. */
  double[][] weights;
  Index featureIndex;
  int[] map;  // caches the featureIndex

  public static final String DEFAULT_CLASSIFIER = "ner-eng-ie.crf-3-all2006.ser.gz";
  private static final boolean VERBOSE = false;

  // List selftraindatums = new ArrayList();


  protected CRFClassifier() {
    init(new SeqClassifierFlags());
  }

  public CRFClassifier(Properties props) {
    init(props);
  }

  public void dropFeaturesBelowThreshold(double threshold) {
    Index newFeatureIndex = new Index();
    for (int i = 0; i < weights.length; i++) {
      double smallest = weights[i][0];
      double biggest = weights[i][0];
      for (int j = 1; j < weights[i].length; j++) {
        if (weights[i][j] > biggest) {
          biggest = weights[i][j];
        }
        if (weights[i][j] < smallest) {
          smallest = weights[i][j];
        }
        if (biggest - smallest > threshold) {
          newFeatureIndex.add(featureIndex.get(i));
          break;
        }
      }
    }

    int[] newMap = new int[newFeatureIndex.size()];
    for (int i = 0; i < newMap.length; i++) {
      int index = featureIndex.indexOf(newFeatureIndex.get(i));
      newMap[i] = map[index];
    }
    map = newMap;
    featureIndex = newFeatureIndex;
  }

  /**
   * Convert a document List into arrays storing the data features and labels.
   *
   * @param document
   * @return A Pair, where the first element is an int[][][] representing the data
   * and the second element is an int[] representing the labels
   */
  public Pair<int[][][],int[]> documentToDataAndLabels(List<FeatureLabel> document) {

    int docSize = document.size();
    // first index is position in the document also the index of the clique/factor table
    // second index is the number of elements in the clique/window these features are for (starting with last element)
    // third index is position of the feature in the array that holds them
    // element in data[j][k][m] is the index of the mth feature occurring in position k of the jth clique
    int[][][] data = new int[docSize][windowSize][];
    // index is the position in the document
    // element in labels[j] is the index of the correct label (if it exists) at position j of document
    int[] labels = new int[docSize];

    if (flags.useReverse) {
      Collections.reverse(document);
    }

    //System.err.println("docSize:"+docSize);
    for (int j = 0; j < docSize; j++) {
      CRFDatum d = makeDatum(document, j, featureFactory);

      List features = d.asFeatures();
      for (int k = 0, fSize = features.size(); k < fSize; k++) {
        Collection cliqueFeatures = (Collection) features.get(k);
        data[j][k] = new int[cliqueFeatures.size()];
        int m = 0;
        for (Iterator iter = cliqueFeatures.iterator(); iter.hasNext(); ) {
          String feature = (String)iter.next();
          int index = featureIndex.indexOf(feature);
          if (index >= 0) {
            data[j][k][m] = index;
            m++;
          } else {
            // this is where we end up when we do feature threshhold cutoffs
          }
        }
        if (m < data[j][k].length) {
          int[] f = new int[m];
          System.arraycopy(data[j][k], 0, f, 0, m);
          data[j][k] = f;
        }
      }

      FeatureLabel wi = document.get(j);
      labels[j] = classIndex.indexOf(wi.answer());
    }

    if (flags.useReverse) {
      Collections.reverse(document);
    }

    // 	System.err.println("numClasses: "+classIndex.size()+" "+classIndex);
    // 	System.err.println("numDocuments: 1");
    // 	System.err.println("numDatums: "+data.length);
    // 	System.err.println("numFeatures: "+featureIndex.size());

    return new Pair<int[][][],int[]>(data, labels);
  }

  /** Convert an ObjectBank to arrays of data features and labels.
   *
   * @param documents
   * @return A Pair, where the first element is an int[][][][] representing the data
   *    and the second element is an int[][] representing the labels.
   */
  public Pair<int[][][][],int[][]> documentsToDataAndLabels(ObjectBank<List<FeatureLabel>> documents) {

    // first index is the number of the document
    // second index is position in the document also the index of the clique/factor table
    // third index is the number of elements in the clique/window thase features are for (starting with last element)
    // fourth index is position of the feature in the array that holds them
    // element in data[i][j][k][m] is the index of the mth feature occurring in position k of the jth clique of the ith document
//    int[][][][] data = new int[documentsSize][][][];
    List<int[][][]> data = new ArrayList<int[][][]>();

    // first index is the number of the document
    // second index is the position in the document
    // element in labels[i][j] is the index of the correct label (if it exists) at position j in document i
//    int[][] labels = new int[documentsSize][];
    List<int[]> labels = new ArrayList<int[]>();

    int numDatums = 0;

    for (List<FeatureLabel> doc : documents) {
      Pair<int[][][],int[]> docPair = documentToDataAndLabels(doc);
      data.add(docPair.first());
      labels.add(docPair.second());
      numDatums += doc.size();
    }

    System.err.println("numClasses: " + classIndex.size() + " " + classIndex);
    System.err.println("numDocuments: " + data.size());
    System.err.println("numDatums: " + numDatums);
    System.err.println("numFeatures: " + featureIndex.size());

    int[][][][] dataA = new int[0][][][];
    int[][] labelsA = new int[0][];

    return new Pair<int[][][][],int[][]>(data.toArray(dataA), labels.toArray(labelsA));
  }


  /** This routine builds the <code>labelIndices</code> which give the
   *  empirically legal label sequences (of length (order) at most
   *  <code>windowSize</code>)
   *  and the <code>classIndex</code>,
   *  which indexes known answer classes.
   *
   * @param ob The training data: Read from an ObjectBank, each
   *                  item in it is a List<FeatureLabel>.
   */
  private void makeAnswerArraysAndTagIndex(ObjectBank<List<FeatureLabel>> ob) {

    HashSet[] featureIndices = new HashSet[windowSize];
    for (int i = 0; i < windowSize; i++) {
      featureIndices[i] = new HashSet();
    }

    labelIndices = new Index[windowSize];
    for (int i = 0; i < labelIndices.length; i++) {
      labelIndices[i] = new Index();
    }

    Index labelIndex = labelIndices[windowSize - 1];

    classIndex = new Index();
    //classIndex.add("O");
    classIndex.add(flags.backgroundSymbol);

    HashSet[] seenBackgroundFeatures = new HashSet[2];
    seenBackgroundFeatures[0] = new HashSet();
    seenBackgroundFeatures[1] = new HashSet();

    //int count = 0;
    for (List<FeatureLabel> doc : ob) {
      //if (count % 100 == 0) {
      //System.err.println(count);
      //}
      //count++;

      if (flags.useReverse) {
        Collections.reverse(doc);
      }

      int docSize = doc.size();
      //create the full set of labels in classIndex
      //note: update to use addAll later
      for (int j = 0; j < docSize; j++) {
        String ans = doc.get(j).answer();
        classIndex.add(ans);
      }

      for (int j = 0; j < docSize; j++) {

        CRFDatum d = makeDatum(doc, j, featureFactory);
        labelIndex.add(d.label());

        List features = d.asFeatures();
        for (int k = 0; k < features.size(); k++) {
          Collection cliqueFeatures = (Collection) features.get(k);
          if (k < 2 && flags.removeBackgroundSingletonFeatures) {
            String ans = doc.get(j).answer();
            boolean background = ans.equals(flags.backgroundSymbol);
            if (k == 1 && j > 0 && background) {
              ans = doc.get(j - 1).answer();
              background = ans.equals(flags.backgroundSymbol);
            }
            if (background) {
              for (Object f : cliqueFeatures) {
                if (!featureIndices[k].contains(f)) {
                  if (seenBackgroundFeatures[k].contains(f)) {
                    seenBackgroundFeatures[k].remove(f);
                    featureIndices[k].add(f);
                  } else {
                    seenBackgroundFeatures[k].add(f);
                  }
                }
              }
            } else {
              seenBackgroundFeatures[k].removeAll(cliqueFeatures);
              featureIndices[k].addAll(cliqueFeatures);
            }
          } else {
            featureIndices[k].addAll(cliqueFeatures);
          }
        }
      }

      if (flags.useReverse) {
        Collections.reverse(doc);
      }
    }

    //     String[] fs = new String[featureIndices[0].size()];
    //     for (Iterator iter = featureIndices[0].iterator(); iter.hasNext(); ) {
    //       System.err.println(iter.next());
    //     }

    int numFeatures = 0;
    for (int i = 0; i < windowSize; i++) {
      numFeatures += featureIndices[i].size();
    }

    featureIndex = new Index();
    map = new int[numFeatures];
    for (int i = 0; i < windowSize; i++) {
      featureIndex.addAll(featureIndices[i]);
      for (Iterator fIter = featureIndices[i].iterator(); fIter.hasNext(); ) {
        map[featureIndex.indexOf(fIter.next())] = i;
      }
    }

    if (flags.useObservedSequencesOnly) {
      for (int i = 0; i < labelIndex.size(); i++) {
        CRFLabel label = (CRFLabel) labelIndex.get(i);
        for (int j = windowSize - 2; j >= 0; j--) {
          label = label.getOneSmallerLabel();
          labelIndices[j].add(label);
        }
      }
    } else {
      for (int i = 0; i < labelIndices.length; i++) {
        labelIndices[i] = allLabels(i + 1, classIndex);
      }
    }

    if (VERBOSE) {
      for (int i = 0; i < featureIndex.size(); i++) {
        System.out.println(i + ": " + featureIndex.get(i));
      }
    }
  }

  protected Index allLabels(int window, Index classIndex) {
    int[] label = new int[window];
    // cdm july 2005: below array initialization isn't necessary: JLS (3rd ed.) 4.12.5
    // Arrays.fill(label, 0);
    int numClasses = classIndex.size();
    Index labelIndex = new Index();
  OUTER: while (true) {
    CRFLabel l = new CRFLabel(label);
    labelIndex.add(l);
    int[] label1 = new int[window];
    System.arraycopy(label, 0, label1, 0, label.length);
    label = label1;
    for (int j = 0; j < label.length; j++) {
      label[j]++;
      if (label[j] >= numClasses) {
        label[j] = 0;
        if (j == label.length - 1) {
          break OUTER;
        }
      } else {
        break;
      }
    }
  }
    return labelIndex;
  }

  public CRFDatum makeDatum(List<FeatureLabel> info, int loc) {
    return makeDatum(info, loc, featureFactory);
  }

  public CRFDatum makeDatum(List<FeatureLabel> info, int loc, alteredu.stanford.nlp.sequences.FeatureFactory featureFactory) {
    //pad.setAnswer("O");
    pad.setAnswer(flags.backgroundSymbol);
    PaddedList<FeatureLabel> pInfo = new PaddedList<FeatureLabel>(info, pad);

    List features = new ArrayList();

//     for (int i = 0; i < windowSize; i++) {
//       List featuresC = new ArrayList();
//       for (int j = 0; j < FeatureFactory.win[i].length; j++) {
//         featuresC.addAll(featureFactory.features(info, loc, FeatureFactory.win[i][j]));
//       }
//       features.add(featuresC);
//     }

    Collection<Clique> done = new HashSet<Clique>();
    for (int i = 0; i < windowSize; i++) {
      List featuresC = new ArrayList();
      Collection<Clique> windowCliques = featureFactory.getCliques(i, 0);
      windowCliques.removeAll(done);
      done.addAll(windowCliques);
      for (Clique c : windowCliques) {
        featuresC.addAll(featureFactory.getCliqueFeatures(pInfo, loc, c));
      }
      features.add(featuresC);
    }

    int[] labels = new int[windowSize];

    for (int i = 0; i < windowSize; i++) {
      String answer = pInfo.get(loc + i - windowSize + 1).answer();

      labels[i] = classIndex.indexOf(answer);
    }

    CRFDatum d = new CRFDatum(features, new CRFLabel(labels));
    //System.err.println(d);
    return d;
  }

  public static class TestSequenceModel implements SequenceModel {

    private int window;
    private int numClasses;
    //private FactorTable[] factorTables;
    private CRFCliqueTree cliqueTree;
    private int[] tags;
    private int[] backgroundTag;

    //public Scorer(FactorTable[] factorTables) {
    public TestSequenceModel(CRFCliqueTree cliqueTree) {
      //this.factorTables = factorTables;
      this.cliqueTree = cliqueTree;
      //this.window = factorTables[0].windowSize();
      this.window = cliqueTree.window();
      //this.numClasses = factorTables[0].numClasses();
      this.numClasses = cliqueTree.getNumClasses();
      tags = new int[numClasses];
      for (int i = 0; i < tags.length; i++) {
        tags[i] = i;
      }
      backgroundTag = new int[]{cliqueTree.backgroundIndex()};
    }

    public int length() {
      return cliqueTree.length();
    }

    public int leftWindow() {
      return window - 1;
    }

    public int rightWindow() {
      return 0;
    }

    public int[] getPossibleValues(int pos) {
      if (pos < window - 1) {
        return backgroundTag;
      }
      return tags;
    }

    public double scoreOf(int[] tags, int pos) {
      int[] previous = new int[window - 1];
      int realPos = pos - window + 1;
      for (int i = 0; i < window - 1; i++) {
        previous[i] = tags[realPos + i];
      }
      return cliqueTree.condLogProbGivenPrevious(realPos, tags[pos], previous);
    }

    public double[] scoresOf(int[] tags, int pos) {
      int realPos = pos - window + 1;
      double[] scores = new double[numClasses];
      int[] previous = new int[window - 1];
      for (int i = 0; i < window - 1; i++) {
        previous[i] = tags[realPos + i];
      }
      for (int i = 0; i < numClasses; i++) {
        scores[i] = cliqueTree.condLogProbGivenPrevious(realPos, i, previous);
      }
      return scores;
    }

    public double scoreOf(int[] sequence) {
      throw new UnsupportedOperationException();
    }

  } // end class TestSequenceModel

  public List<FeatureLabel> test(List<FeatureLabel> document) {
    if (flags.doGibbs) {
      try {
        return testGibbs(document);
      } catch (Exception e) {
        System.err.println("Error running testGibbs inference!");
        e.printStackTrace();
        return null;
      }
    } else if (flags.crfType.equalsIgnoreCase("maxent")) {
      return testMaxEnt(document);
    } else {
      throw new RuntimeException();
    }
  }

  public SequenceModel getSequenceModel(List<FeatureLabel> doc) {
    Pair<int[][][],int[]> p = documentToDataAndLabels(doc);
    int[][][] data = p.first();

    CRFCliqueTree cliqueTree = CRFCliqueTree.getCalibratedCliqueTree(weights, data, labelIndices, classIndex.size(), classIndex, flags.backgroundSymbol);

    //Scorer scorer = new Scorer(factorTables);
    SequenceModel model = new TestSequenceModel(cliqueTree);
    return model;
  }

  public List<FeatureLabel> testMaxEnt(List<FeatureLabel> document) {
    if (document.isEmpty()) {
      return document;
    }

    SequenceModel model = getSequenceModel(document);

    if (flags.inferenceType == null) { flags.inferenceType = "Viterbi"; }

    BestSequenceFinder tagInference;
    if (flags.inferenceType.equalsIgnoreCase("Viterbi")) {
      tagInference = new ExactBestSequenceFinder();
    } else if (flags.inferenceType.equalsIgnoreCase("Beam")) {
      tagInference = new BeamBestSequenceFinder(flags.beamSize);
    } else {
      throw new RuntimeException("Unknown inference type: "+flags.inferenceType+". Your options are Viterbi|Beam.");
    }

    int[] bestSequence = tagInference.bestSequence(model);

    if (flags.useReverse) {
      Collections.reverse(document);
    }
    for (int j = 0, docSize = document.size(); j < docSize; j++) {
      FeatureLabel wi = document.get(j);
      String guess = classIndex.get(bestSequence[j + windowSize - 1]);
      wi.setAnswer(guess);
    }
    if (flags.useReverse) {
      Collections.reverse(document);
    }
    return document;
  }


  public List<FeatureLabel> testGibbs(List<FeatureLabel> document) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException  {
    System.err.println("Testing using Gibbs sampling.");
    Pair<int[][][],int[]> p = documentToDataAndLabels(document);
    int[][][] data = p.first();

    List<FeatureLabel> newDocument = document; // reversed if necessary
    if (flags.useReverse) {
      Collections.reverse(document);
      newDocument = new ArrayList<FeatureLabel>(document);
      Collections.reverse(document);
    }

    CRFCliqueTree cliqueTree = CRFCliqueTree.getCalibratedCliqueTree(weights, data, labelIndices, classIndex.size(), classIndex, flags.backgroundSymbol);

    SequenceModel model = cliqueTree;
    SequenceListener listener = cliqueTree;

    EntityCachingAbstractSequencePrior prior;

    Class c;
    if (flags.useNERPrior) {
      c = Class.forName("EmpiricalNERPrior");
      // EmpiricalNERPrior prior = new EmpiricalNERPrior(flags.backgroundSymbol, classIndex, newDocument);
      // SamplingNERPrior prior = new SamplingNERPrior(flags.backgroundSymbol, classIndex, newDocument);
      // model = new FactoredSequenceModel(model, prior);
      // listener = new FactoredSequenceListener(listener, prior);
    } else if (flags.useAcqPrior) {
      c = Class.forName("AcquisitionsPrior");
      // AcquisitionsPrior prior = new AcquisitionsPrior(flags.backgroundSymbol, classIndex, newDocument);
      // model = new FactoredSequenceModel(model, prior);
      // listener = new FactoredSequenceListener(listener, prior);
    } else { // if (flags.useSemPrior)
      c = Class.forName("SeminarsPrior");
      // SeminarsPrior prior = new SeminarsPrior(flags.backgroundSymbol, classIndex, newDocument);
      // model = new FactoredSequenceModel(model, prior);
      // listener = new FactoredSequenceListener(listener, prior);
    }

    // To do the whole stuff by reflection...
    Constructor c2 = c.getConstructor(new Class[]{String.class,Index.class,List.class});
    prior = (EntityCachingAbstractSequencePrior)c2.newInstance(flags.backgroundSymbol, classIndex, newDocument);
    model = new FactoredSequenceModel(model, prior);
    listener = new FactoredSequenceListener(listener, prior);

    SequenceGibbsSampler sampler = new SequenceGibbsSampler(0, 0, listener);
    int[] sequence = new int[cliqueTree.length()];

    if (flags.initViterbi) {
      TestSequenceModel testSequenceModel = new TestSequenceModel(cliqueTree);
      ExactBestSequenceFinder tagInference = new ExactBestSequenceFinder();
      int[] bestSequence = tagInference.bestSequence(testSequenceModel);
      System.arraycopy(bestSequence, windowSize-1, sequence, 0, sequence.length);
    } else {
      int[] initialSequence = SequenceGibbsSampler.getRandomSequence(model);
      System.arraycopy(initialSequence, 0, sequence, 0, sequence.length);
    }

    sampler.verbose = 0;

    if (flags.annealingType.equalsIgnoreCase("linear")) {
      sequence = sampler.findBestUsingAnnealing(model, CoolingSchedule.getLinearSchedule(1.0, flags.numSamples), sequence);
    } else if (flags.annealingType.equalsIgnoreCase("exp") || flags.annealingType.equalsIgnoreCase("exponential")) {
      sequence = sampler.findBestUsingAnnealing(model, CoolingSchedule.getExponentialSchedule(1.0, flags.annealingRate, flags.numSamples), sequence);
    } else {
      throw new RuntimeException("No annealing type specified");
    }

    //System.err.println(ArrayMath.toString(sequence));

    if (flags.useReverse) {
      Collections.reverse(document);
    }

    for (int j = 0; j < newDocument.size(); j++) {
      FeatureLabel wi = document.get(j);
      if (wi==null) throw new RuntimeException("");
      if (classIndex==null) throw new RuntimeException("");
      wi.setAnswer(classIndex.get(sequence[j]));
    }

    if (flags.useReverse) {
      Collections.reverse(document);
    }

    return document;
  }

  /**
   * Takes a {@link List} of {@link FeatureLabel}s and prints the likelihood
   * of each possible label at each point.
   *
   * @param document A {@link List} of {@link FeatureLabel}s.
   */
  public void printProbsDocument(List<FeatureLabel> document) {

    Pair<int[][][],int[]> p = documentToDataAndLabels(document);
    int[][][] data = p.first();

    //FactorTable[] factorTables = CRFLogConditionalObjectiveFunction.getCalibratedCliqueTree(weights, data, labelIndices, classIndex.size());
    CRFCliqueTree cliqueTree = CRFCliqueTree.getCalibratedCliqueTree(weights, data, labelIndices, classIndex.size(), classIndex, flags.backgroundSymbol);

    //    for (int i = 0; i < factorTables.length; i++) {
    for (int i = 0; i < cliqueTree.length(); i++) {
      FeatureLabel wi = document.get(i);
      System.out.print(wi.word() + "\t");
      for (Iterator iter = classIndex.iterator(); iter.hasNext();) {
        Object label = iter.next();
        int index = classIndex.indexOf(label);
        //        double prob = Math.pow(Math.E, factorTables[i].logProbEnd(index));
        double prob = cliqueTree.prob(i, index);
        System.out.print(label + "=" + prob);
        if (iter.hasNext()) {
          System.out.print("\t");
        } else {
          System.out.print("\n");
        }
      }
    }
  }

  /**
   * Takes the file, reads it in, and prints out the likelihood of
   * each possible label at each point.
   *
   * @param filename The path to the specified file
   */
  public void printFirstOrderProbs(String filename) {
    // only for the OCR data does this matter
    flags.ocrTrain = false;

    ObjectBank<List<FeatureLabel>> docs = makeObjectBank(filename);
    printFirstOrderProbsDocuments(docs);
  }

  /**
   * Takes a {@link List} of documents and prints the likelihood
   * of each possible label at each point.
   *
   * @param documents A {@link List} of {@link List} of {@link FeatureLabel}s.
   */
  public void printFirstOrderProbsDocuments(ObjectBank<List<FeatureLabel>> documents) {
    for (List<FeatureLabel> doc : documents) {
      printFirstOrderProbsDocument(doc);
      System.out.println();
    }
  }

  /**
   * Takes a {@link List} of {@link FeatureLabel}s and prints the likelihood
   * of each possible label at each point.
   *
   * @param document A {@link List} of {@link FeatureLabel}s.
   */
  public void printFirstOrderProbsDocument(List<FeatureLabel> document) {

    Pair<int[][][],int[]> p = documentToDataAndLabels(document);
    int[][][] data = p.first();

    //FactorTable[] factorTables = CRFLogConditionalObjectiveFunction.getCalibratedCliqueTree(weights, data, labelIndices, classIndex.size());
    CRFCliqueTree cliqueTree = CRFCliqueTree.getCalibratedCliqueTree(weights, data, labelIndices, classIndex.size(), classIndex, flags.backgroundSymbol);

    //    for (int i = 0; i < factorTables.length; i++) {
    for (int i = 0; i < cliqueTree.length(); i++) {
      FeatureLabel wi = document.get(i);
      System.out.print(wi.word() + "\t");
      for (Iterator iter = classIndex.iterator(); iter.hasNext();) {
        Object label = iter.next();
        int index = classIndex.indexOf(label);
        if (i == 0) {
          //double prob = Math.pow(Math.E, factorTables[i].logProbEnd(index));
          double prob = cliqueTree.prob(i, index);
          System.out.print(label + "=" + prob);
          if (iter.hasNext()) {
            System.out.print("\t");
          } else {
            System.out.print("\n");
          }
        } else {
          for (Iterator iter1 = classIndex.iterator(); iter1.hasNext();) {
            Object label1 = iter1.next();
            int index1 = classIndex.indexOf(label1);
            //double prob = Math.pow(Math.E, factorTables[i].logProbEnd(new int[]{index1, index}));
            double prob = cliqueTree.prob(i, new int[]{index1, index});
            System.out.print(label1 + "_" + label + "=" + prob);
            if (iter.hasNext() || iter1.hasNext()) {
              System.out.print("\t");
            } else {
              System.out.print("\n");
            }
          }
        }
      }
    }
  }


  /** Train a classifier:
   */
  public void train(ObjectBank<List<FeatureLabel>> docs) {
    makeAnswerArraysAndTagIndex(docs);

    for (int i = 0; i <= flags.numTimesPruneFeatures; i++) {

      Pair dataAndLabels = documentsToDataAndLabels(docs);
      if (flags.numTimesPruneFeatures == i) {
        docs = null; // hopefully saves memory
      }

      // save feature index to disk and read in later
      File featIndexFile = null;

      if (flags.saveFeatureIndexToDisk) {
        try {
          System.err.println("Writing feature index to temporary file.");
          featIndexFile = IOUtils.writeObjectToTempFile(featureIndex, "featIndex" + i+ ".tmp");
          featureIndex = null;
        } catch (IOException e) {
          throw new RuntimeException("Could not open temporary feature index file for writing.");
        }
      }

      // first index is the number of the document
      // second index is position in the document also the index of the clique/factor table
      // third index is the number of elements in the clique/window thase features are for (starting with last element)
      // fourth index is position of the feature in the array that holds them
      // element in data[i][j][k][m] is the index of the mth feature occurring in position k of the jth clique of the ith document
      int[][][][] data = (int[][][][]) dataAndLabels.first();
      // first index is the number of the document
      // second index is the position in the document
      // element in labels[i][j] is the index of the correct label (if it exists) at position j in document i
      int[][] labels = (int[][]) dataAndLabels.second();


      if (flags.loadProcessedData != null) {
        List processedData = loadProcessedData(flags.loadProcessedData);
        if (processedData != null) {
          // enlarge the data and labels array
          int[][][][] allData = new int[data.length + processedData.size()][][][];
          int[][] allLabels = new int[labels.length + processedData.size()][];
          System.arraycopy(data, 0, allData, 0, data.length);
          System.arraycopy(labels, 0, allLabels, 0, labels.length);
          // add to the data and labels array
          addProcessedData(processedData, allData, allLabels, data.length);
          data = allData;
          labels = allLabels;
        }
      }

      if (flags.useFloat) {
        CRFLogConditionalObjectiveFloatFunction func = new CRFLogConditionalObjectiveFloatFunction(data, labels, featureIndex, windowSize, classIndex, labelIndices, map, flags.backgroundSymbol, flags.sigma);
        func.crfType = flags.crfType;

        QNMinimizer minimizer;
        if (flags.interimOutputFreq != 0) {
          FloatFunction monitor = new ResultStoringFloatMonitor(flags.interimOutputFreq, flags.serializeTo);
          minimizer = new QNMinimizer(monitor);
        } else {
          minimizer = new QNMinimizer();
        }

        if (i == 0) {
          minimizer.setM(flags.QNsize);
        } else {
          minimizer.setM(flags.QNsize2);
        }

        float[] initialWeights;
        if (flags.initialWeights == null) {
          initialWeights = func.initial();
        } else {
          try {
            System.err.println("Reading initial weights from file " + flags.initialWeights);
            DataInputStream dis = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(flags.initialWeights))));
            initialWeights = Convert.readFloatArr(dis);
          } catch (IOException e) {
            throw new RuntimeException("Could not read from float initial weight file " + flags.initialWeights);
          }
        }
        System.err.println("numWeights: " + initialWeights.length);
        float[] weights = minimizer.minimize(func, (float) flags.tolerance, initialWeights);
        this.weights = ArrayMath.floatArrayToDoubleArray(func.to2D(weights));

      } else {

        CRFLogConditionalObjectiveFunction func = new CRFLogConditionalObjectiveFunction(data, labels, featureIndex, windowSize, classIndex, labelIndices, map, flags.backgroundSymbol, flags.sigma);

        func.crfType = flags.crfType;

        QNMinimizer minimizer;
        if (flags.interimOutputFreq != 0) {
          Function monitor = new ResultStoringMonitor(flags.interimOutputFreq, flags.serializeTo);
          minimizer = new QNMinimizer(monitor);
        } else {
          minimizer = new QNMinimizer();
        }

        if (i == 0) {
          minimizer.setM(flags.QNsize);
        } else {
          minimizer.setM(flags.QNsize2);
        }

        double[] initialWeights;
        if (flags.initialWeights == null) {
          initialWeights = func.initial();
        } else {
          try {
            System.err.println("Reading initial weights from file " + flags.initialWeights);
            DataInputStream dis = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(flags.initialWeights))));
            initialWeights = Convert.readDoubleArr(dis);
          } catch (IOException e) {
            throw new RuntimeException("Could not read from double initial weight file " + flags.initialWeights);
          }
        }
        System.err.println("numWeights: " + initialWeights.length);
        double[] weights = minimizer.minimize(func, flags.tolerance, initialWeights);
        this.weights = func.to2D(weights);
      }

      // save feature index to disk and read in later
      if (flags.saveFeatureIndexToDisk) {
        try {
          System.err.println("Reading temporary feature index file.");
          featureIndex = (Index) IOUtils.readObjectFromFile(featIndexFile);
        } catch (Exception e) {
          throw new RuntimeException("Could not open temporary feature index file for reading.");
        }
      }

      if (i != flags.numTimesPruneFeatures) {
        dropFeaturesBelowThreshold(flags.featureDiffThresh);
        System.err.println("Removing features with weight below " + flags.featureDiffThresh + " and retraining...");
      }

    }
  }

  /**
   * Creates a new CRFDatum from the preprocessed allData format, given the document number,
   * position number, and a List of Object labels
   *
   * @param allData
   * @param beginPosition
   * @param endPosition
   * @param labeledWordInfos
   * @return A new CRFDatum
   */
  protected List<CRFDatum> extractDatumSequence(int[][][] allData, int beginPosition, int endPosition, List labeledWordInfos) {
    List<CRFDatum> result = new ArrayList<CRFDatum>();
    int beginContext = beginPosition - windowSize + 1;
    if (beginContext < 0) {
      beginContext = 0;
    }
    // for the beginning context, add some dummy datums with no features!
    // TODO: is there any better way to do this?
    for (int position = beginContext; position < beginPosition; position++) {
      List cliqueFeatures = new ArrayList();
      for (int i = 0; i < windowSize; i++) {
        // create a feature list
        cliqueFeatures.add(Collections.EMPTY_SET);
      }
      CRFDatum datum = new CRFDatum(cliqueFeatures, ((FeatureLabel) labeledWordInfos.get(position)).answer());
      result.add(datum);
    }
    // now add the real datums
    for (int position = beginPosition; position <= endPosition; position++) {
      List cliqueFeatures = new ArrayList();
      for (int i = 0; i < windowSize; i++) {
        // create a feature list
        Collection features = new ArrayList();
        for (int j = 0; j < allData[position][i].length; j++) {
          features.add(featureIndex.get(allData[position][i][j]));
        }
        cliqueFeatures.add(features);
      }
      CRFDatum datum = new CRFDatum(cliqueFeatures, ((FeatureLabel) labeledWordInfos.get(position)).answer());
      result.add(datum);
    }
    return result;
  }

  /**
   * Adds the List of Lists of CRFDatums to the data and labels arrays, treating each datum as if
   * it were its own document.
   * Adds context labels in addition to the target label for each datum, meaning that for a particular
   * document, the number of labels will be windowSize-1 greater than the number of datums.
   *
   * @param processedData a List of Lists of CRFDatums
   * @param data
   * @param labels
   * @param offset
   */
  protected void addProcessedData(List processedData, int[][][][] data, int[][] labels, int offset) {
    for (int i = 0; i < processedData.size(); i++) {
      int dataIndex = i + offset;
      List document = (List) processedData.get(i);
      labels[dataIndex] = new int[document.size()];
      data[dataIndex] = new int[document.size()][][];
      for (int j = 0; j < document.size(); j++) {
        CRFDatum crfDatum = (CRFDatum) document.get(j);
        // add label, they are offset by extra context
        labels[dataIndex][j] = classIndex.indexOf(crfDatum.label());
        // add features
        List cliques = crfDatum.asFeatures();
        data[dataIndex][j] = new int[cliques.size()][];
        for (int k = 0; k < cliques.size(); k++) {
          Collection features = (Collection) cliques.get(k);

          // Debug only: Remove
          // if (j < windowSize) {
          //   System.err.println("addProcessedData: Features Size: " + features.size());
          // }

          data[dataIndex][j][k] = new int[features.size()];

          int m = 0;
          try {
            for (Iterator iterator = features.iterator(); iterator.hasNext();) {
              String feature = (String)iterator.next();
              //System.err.println("feature " + feature);
              //              if (featureIndex.indexOf(feature)) ;
              if (featureIndex == null) {
                System.out.println("Feature is NULL!");
              }
              data[dataIndex][j][k][m] = featureIndex.indexOf(feature);
              m++;
            }
          } catch (Exception e) {
            e.printStackTrace();
            System.err.printf("[index=%d, j=%d, k=%d, m=%d]\n", dataIndex, j, k, m);
            System.err.println("data.length                    " + data.length);
            System.err.println("data[dataIndex].length         " + data[dataIndex].length);
            System.err.println("data[dataIndex][j].length      " + data[dataIndex][j].length);
            System.err.println("data[dataIndex][j][k].length   " + data[dataIndex][j].length);
            System.err.println("data[dataIndex][j][k][m]       " + data[dataIndex][j][k][m]);
            System.exit(1);

          }
        }
      }
    }
  }

  protected void saveProcessedData(List datums, String filename) {
    System.err.print("Saving processsed data of size " + datums.size() + " to serialized file...");
    ObjectOutputStream oos = null;
    try {
      oos = new ObjectOutputStream(new FileOutputStream(filename));
      oos.writeObject(datums);
    } catch (IOException e) {
      // do nothing
    } finally {
      if (oos != null) {
        try {
          oos.close();
        } catch (IOException e) {
        }
      }
    }
    System.err.println("done.");
  }

  protected List loadProcessedData(String filename) {
    System.err.print("Loading processed data from serialized file...");
    ObjectInputStream ois = null;
    List result = Collections.EMPTY_LIST;
    try {
      ois = new ObjectInputStream(new FileInputStream(filename));
      result = (List) ois.readObject();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (ois != null) {
        try {
          ois.close();
        } catch (IOException e) {
        }
      }
    }
    System.err.println("done. Got " + result.size() + " datums.");
    return result;
  }

  public void serializeClassifier(String serializePath) {
    System.err.print("Serializing classifier to " + serializePath + "...");

    try {
      ObjectOutputStream oos;
      if (serializePath.endsWith(".gz")) {
        oos = new ObjectOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(serializePath))));
      } else {
        oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(serializePath)));
      }

      oos.writeObject(labelIndices);
      oos.writeObject(classIndex);
      oos.writeObject(featureIndex);
      oos.writeObject(flags);
      oos.writeObject(featureFactory);
      oos.writeInt(windowSize);
      oos.writeObject(weights);
      //oos.writeObject(WordShapeClassifier.getKnownLowerCaseWords());
      if (readerAndWriter instanceof TrueCasingDocumentReaderAndWriter) {
        oos.writeObject(TrueCasingDocumentReaderAndWriter.knownWords);
      }

      oos.writeObject(knownLCWords);

      oos.close();
      System.err.println("done.");

    } catch (Exception e) {
      System.err.println("Failed");
      e.printStackTrace();
      // don't actually exit in case they're testing too
      //System.exit(1);
    }
  }

  /**
   * Loads a classifier from the specified InputStream.
   * This version works quietly (unless VERBOSE is true).
   * If props is non-null then any properties it specifies override
   * those in the serialized file.  However, only some properties are
   * sensible to change (you shouldn't change how features are defined).
   */
  public void loadClassifier(InputStream in, Properties props) throws ClassCastException, IOException, ClassNotFoundException {
    ObjectInputStream ois = new ObjectInputStream(in);

    labelIndices = (Index[]) ois.readObject();
    classIndex = (Index<String>) ois.readObject();
    featureIndex = (Index) ois.readObject();
    flags = (SeqClassifierFlags) ois.readObject();
    featureFactory = (alteredu.stanford.nlp.sequences.FeatureFactory) ois.readObject();

    if (props != null) {
      flags.setProperties(props, false);
    }
    reinit();

    windowSize = ois.readInt();
    weights = (double[][]) ois.readObject();

    if (readerAndWriter instanceof TrueCasingDocumentReaderAndWriter) {
      TrueCasingDocumentReaderAndWriter.knownWords = (Set)ois.readObject();
    }

    //WordShapeClassifier.setKnownLowerCaseWords((Set) ois.readObject());
    knownLCWords = (Set<String>) ois.readObject();

    ois.close();
    if (VERBOSE) {
      System.err.println("windowSize=" + windowSize);
      System.err.println("flags=\n" + flags);
    }
  }

  /**
   * This is used to load the default supplied classifier stored within
   * the jar file.  **THIS FUNCTION
   * WILL ONLY WORK IF RUN INSIDE A JAR FILE**
   */
  public void loadDefaultClassifier() {
    loadJarClassifier(DEFAULT_CLASSIFIER, null);
  }


  /**
   * Used to get the default supplied classifier.  **THIS FUNCTION
   * WILL ONLY WORK IF RUN INSIDE A JAR FILE**
   */
  public static CRFClassifier getDefaultClassifier() {
    CRFClassifier crf = new CRFClassifier();
    crf.loadDefaultClassifier();
    return crf;
  }

  /**
   * Used to load a classifier stored as a resource inside a jar file.
   * **THIS FUNCTION WILL ONLY WORK IF RUN INSIDE A JAR FILE**
   */
  public static CRFClassifier getJarClassifier(String resourceName, Properties props) {
    CRFClassifier crf = new CRFClassifier();
    crf.loadJarClassifier(resourceName, props);
    return crf;
  }

  public static CRFClassifier getClassifierNoExceptions(File file) {
    CRFClassifier crf = new CRFClassifier();
    crf.loadClassifierNoExceptions(file);
    return crf;
  }

  public static CRFClassifier getClassifier(File file) throws IOException, ClassCastException, ClassNotFoundException {
    CRFClassifier crf = new CRFClassifier();
    crf.loadClassifier(file);
    return crf;
  }

  public static CRFClassifier getClassifierNoExceptions(String loadPath) {
    CRFClassifier crf = new CRFClassifier();
    crf.loadClassifierNoExceptions(loadPath);
    return crf;
  }

  public static CRFClassifier getClassifier(String loadPath) throws IOException, ClassCastException, ClassNotFoundException {
    CRFClassifier crf = new CRFClassifier();
    crf.loadClassifier(loadPath);
    return crf;
  }

  public static CRFClassifier getClassifierNoExceptions(InputStream in) {
    CRFClassifier crf = new CRFClassifier();
    crf.loadClassifierNoExceptions(new BufferedInputStream(in));
    return crf;
  }

  public static CRFClassifier getClassifier(InputStream in) throws IOException, ClassCastException, ClassNotFoundException {
    CRFClassifier crf = new CRFClassifier();
    crf.loadClassifier(new BufferedInputStream(in));
    return crf;
  }

  /** The main method. See the class documentation. */
  public static void main(String[] args) throws Exception {
    Properties props = StringUtils.argsToProperties(args);
    CRFClassifier crf = new CRFClassifier(props);
    String testFile = crf.flags.testFile;
    String textFile = crf.flags.textFile;
    String loadPath = crf.flags.loadClassifier;
    String serializeTo = crf.flags.serializeTo;

    if (crf.flags.trainFile != null) {
      crf.train();
    } else if (loadPath != null) {
      crf.loadClassifierNoExceptions(loadPath, props);
    } else if (crf.flags.loadJarClassifier != null) {
      crf.loadJarClassifier(crf.flags.loadJarClassifier, props);
    } else {
      crf.loadDefaultClassifier();
    }

    System.err.println("Using " + crf.flags.featureFactory);
    System.err.println("Using " + StringUtils.getShortClassName(crf.readerAndWriter));

    if (serializeTo != null) {
      crf.serializeClassifier(serializeTo);
    }

    if (testFile != null) {
      if (crf.flags.printFirstOrderProbs) {
        crf.printFirstOrderProbs(testFile);
      } else if (crf.flags.printProbs) {
        crf.printProbs(testFile);
      } else {
        crf.testAndWriteAnswers(testFile);
      }
    }

    if (textFile != null) {
      DocumentReaderAndWriter oldRW = crf.readerAndWriter;
      crf.readerAndWriter = new PlainTextDocumentReaderAndWriter();
      crf.testAndWriteAnswers(textFile);
      crf.readerAndWriter = oldRW;
    }
  } // end main

}
