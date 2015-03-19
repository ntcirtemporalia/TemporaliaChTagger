// NERFeatureFactory -- features for a probabilistic Named Entity Recognizer
// Copyright (c) 2002-2006 Leland Stanford Junior University
// Additional features (c) 2003 The University of Edinburgh
//
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

import alteredu.stanford.nlp.process.WordShapeClassifier;
import alteredu.stanford.nlp.util.PaddedList;
import alteredu.stanford.nlp.util.StringUtils;
import alteredu.stanford.nlp.util.Timing;
import alteredu.stanford.nlp.ling.FeatureLabel;
import alteredu.stanford.nlp.sequences.SeqClassifierFlags;
import alteredu.stanford.nlp.sequences.FeatureFactory;
import alteredu.stanford.nlp.sequences.CoNLLDocumentReaderAndWriter;
import alteredu.stanford.nlp.sequences.Clique;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Features for Named Entity Recognition.  The code here creates the features
 * by processing Lists of FeatureLabels.
 * Look at {@link SeqClassifierFlags} to see where the flags are set for
 * what options to use for what flags.
 * <p>
 * To add a new feature extractor, you should do the following:
 * <ol>
 * <li>Add a variable (boolean, int, String, etc. as appropriate) to
 *     SeqClassifierFlags to mark if the new extractor is turned on or 
 *     its value, etc. Add it at the <i>bottom</i> of the list of variables
 *     currently in the class (this avoids problems with older serialized
 *     files breaking). Make the default value of the variable false/null/0 
 *     (this is again for backwards compatibility).</li>
 * <li>Add a clause to the big if/then/else of setProperties(Properties) in
 *     SeqClassifierFlags.  Unless it is a macro option, make the option name
 *     the same as the variable name used in step 1.</li>
 * <li>Add code to NERFeatureFactory for this feature. First decide which 
 *     classes (hidden states) are involved in the feature.  If only the 
 *     current class, you add the feature extractor to the
 *     <code>featuresC</code> code, if both the current and previous class,
 *     then <code>featuresCpC</code>, etc.</li>
 * </ol>
 * <p> Parameters can be defined using a Properties file
 * (specified on the command-line with <code>-prop</code> <i>propFile</i>),
 * or directly on the command line. The following properties are recognized:
 * </p>
 * <table border="1">
 * <tr><td><b>Property Name</b></td><td><b>Type</b></td><td><b>Default Value</b></td><td><b>Description</b></td></tr>
 * <tr><td> loadClassifier </td><td>String</td><td>n/a</td><td>Path to serialized classifier to load</td></tr>
 * <tr><td> loadAuxClassifier </td><td>String</td><td>n/a</td><td>Path to auxiliary classifier to load.</td></tr>
 * <tr><td> serializeTo</td><td>String</td><td>n/a</td><td>Path to serialize classifier to</td></tr>
 * <tr><td> trainFile</td><td>String</td><td>n/a</td><td>Path of file to use as training data</td></tr>
 * <tr><td> testFile</td><td>String</td><td>n/a</td><td>Path of file to use as training data</td></tr>
 * <p/>
 * <tr><td> useWord</td><td>boolean</td><td>true</td><td>Gives you feature for w</td></tr>
 * <tr><td> useBinnedLength</td><td>String</td><td>null</td><td>If non-null, treat as a sequence of comma separated integer bounds, where items above the previous bound up to the next bound are binned Len-<i>range</i></td></tr>
 * <tr><td> useNGrams</td><td>boolean</td><td>false</td><td>Make features from letter n-grams</td></tr>
 * <tr><td> lowercaseNGrams</td><td>boolean</td><td>false</td><td>Make features from letter n-grams only lowercase</td></tr>
 * <tr><td> dehyphenateNGrams</td><td>boolean</td><td>false</td><td>Remove hyphens before making features from letter n-grams</td></tr>
 * <tr><td> conjoinShapeNGrams</td><td>boolean</td><td>false</td><td>Conjoin word shape and n-gram features</td></tr>
 * <tr><td> usePrev</td><td>boolean</td><td>false</td><td>Gives you feature for (pw,c), and together with other options enables other previous features, such as (pt,c) [with useTags)</td></tr>
 * <tr><td> useNext</td><td>boolean</td><td>false</td><td>Gives you feature for (nw,c), and together with other options enables other next features, such as (nt,c) [with useTags)</td></tr>
 * <tr><td> useTags</td><td>boolean</td><td>false</td><td>Gives you features for (t,c), (pt,c) [if usePrev], (nt,c) [if useNext]</td></tr>
 * <tr><td> useWordPairs</td><td>boolean</td><td>false</td><td>Gives you
 * features for (pw, w, c) and (w, nw, c)</td></tr>
 * <tr><td> useGazettes</td><td>boolean</td><td>false</td><td></td></tr>
 * <tr><td> wordShape</td><td>String</td><td>none</td><td>Either "none" for no wordShape use, or the name of a word shape function recognized by {@link WordShapeClassifier#lookupShaper(String)}</td></tr>
 * <tr><td> useSequences</td><td>boolean</td><td>true</td><td></td></tr>
 * <tr><td> usePrevSequences</td><td>boolean</td><td>false</td><td></td></tr>
 * <tr><td> useNextSequences</td><td>boolean</td><td>false</td><td></td></tr>
 * <tr><td> useLongSequences</td><td>boolean</td><td>false</td><td>Use plain higher-order state sequences out to minimum of length or maxLeft</td></tr>
 * <tr><td> useBoundarySequences</td><td>boolean</td><td>false</td><td>Use extra second order class sequence features when previous is CoNLL boundary, so entity knows it can span boundary.</td></tr>
 * <tr><td> useTaggySequences</td><td>boolean</td><td>false</td><td>Use first, second, and third order class and tag sequence interaction features</td></tr>
 * <tr><td> useExtraTaggySequences</td><td>boolean</td><td>false</td><td>Add in sequences of tags with just current class features</td></tr>
 * <tr><td> useTaggySequencesShapeInteraction</td><td>boolean</td><td>false</td><td>Add in terms that join sequences of 2 or 3 tags with the current shape</td></tr>
 * <tr><td> strictlyFirstOrder</td><td>boolean</td><td>false</td><td>As an override to whatever other options are in effect, deletes all features other than C and CpC clique features when building the classifier</td></tr>
 * <tr><td> entitySubclassification</td><td>String</td><td>"IO"</td><td>If
 * set, convert the labeling of classes (but not  the background) into 
 * one of several alternate encodings (IO, IOB1, IOB2, IOE1, IOE2, SBIEO, with 
 * a S(ingle), B(eginning),
 * E(nding), I(nside) 4-way classification for each class.  By default, we 
 * either do no re-encoding, or the CoNLLDocumentIteratorFactory does a 
 * lossy encoding as IO.  Note that this is all CoNLL-specific, and depends on
 * their way of prefix encoding classes, and is only implemented by 
 * the CoNLLDocumentIteratorFactory. </td></tr>
 * <tr><td> useGazettePhrases</td><td>boolean</td><td>false</td><td></td></tr>
 * <p/>
 * <tr><td> useSum</td><td>boolean</td><td>false</td><td></td></tr>
 * <tr><td> tolerance</td><td>double</td><td>1e-4</td><td>Convergence tolerance in optimization</td></tr>
 * <tr><td> printFeatures</td><td>String</td><td>null</td><td>print out the features of the classifier to a file based on this name (suffixed "-1" and "-2")</td></tr>
 * <p/>
 * <tr><td> useSymTags</td><td>boolean</td><td>false</td><td>Gives you
 * features (pt, t, nt, c), (t, nt, c), (pt, t, c)</td></tr>
 * <tr><td> useSymWordPairs</td><td>boolean</td><td>false</td><td>Gives you
 * features (pw, nw, c)</td></tr>
 * <p/>
 * <tr><td> printClassifier</td><td>String</td><td>null</td><td>Style in which to print the classifier. One of: HighWeight, HighMagnitude, Collection, AllWeights, WeightHistogram</td></tr>
 * <tr><td> printClassifierParam</td><td>int</td><td>100</td><td>A parameter
 * to the printing style, which may give, for example the number of parameters
 * to print</td></tr>
 * <tr><td> intern</td><td>boolean</td><td>false</td><td>If true,
 * (String) intern read in data and classes and feature (pre-)names such
 * as substring features</td></tr>
 * <tr><td> intern2</td><td>boolean</td><td>false</td><td>If true, intern all (final) feature names (if only current word and ngram features are used, these will already have been interned by intern, and this is an unnecessary no-op)</td></tr>
 * <tr><td> cacheNGrams</td><td>boolean</td><td>false</td><td>If true,
 * record the NGram features that correspond to a String (under the current
 * option settings) and reuse rather than recalculating if the String is seen
 * again.</td></tr>
 * <tr><td> selfTest</td><td>boolean</td><td>false</td><td></td></tr>
 * <p/>
 * <tr><td> sloppyGazette</td><td>boolean</td><td>false</td><td></td></tr>
 * <tr><td> cleanGazette</td><td>boolean</td><td>false</td><td></td></tr>
 * <p/>
 * <tr><td> noMidNGrams</td><td>boolean</td><td>false</td><td>Do not include character n-gram features for n-grams that contain neither the beginning or end of the word</td></tr>
 * <tr><td> maxNGramLeng</td><td>int</td><td>-1</td><td>If this number is
 * positive, n-grams above this size will not be used in the model</td></tr>
 * <tr><td> useReverse</td><td>boolean</td><td>false</td><td></td></tr>
 * <tr><td> retainEntitySubclassification</td><td>boolean</td><td>false</td><td>If true, rather than undoing a recoding of entity tag subtypes (such as BIO variants), just leave them in the output.</td></tr>
 * <tr><td> useLemmas</td><td>boolean</td><td>false</td><td>Include the lemma of a word as a feature.</td></tr>
 * <tr><td> usePrevNextLemmas</td><td>boolean</td><td>false</td><td>Include the previous/next lemma of a word as a feature.</td></tr>
 * <tr><td> useLemmaAsWord</td><td>boolean</td><td>false</td><td>Include the lemma of a word as a feature.</td></tr>
 * <tr><td> normalizeTerms</td><td>boolean</td><td>false</td><td>If this is true, some words are normalized: day and month names are lowercased (as for normalizeTimex) and some British spellings are mapped to American English spellings (e.g., -our/-or, etc.).</td></tr>
 * <tr><td> normalizeTimex</td><td>boolean</td><td>false</td><td>If this is true, capitalization of day and month names is normalized to lowercase</td></tr>
 * <tr><td> useNB</td><td>boolean</td><td>false</td><td></td></tr>
 * <tr><td> useTypeSeqs</td><td>boolean</td><td>false</td><td>Use basic zeroeth order word shape features.</td></tr>
 * <tr><td> useTypeSeqs2</td><td>boolean</td><td>false</td><td>Add additional first and second order word shape features</td></tr>
 * <tr><td> useTypeSeqs3</td><td>boolean</td><td>false</td><td>Adds one more first order shape sequence</td></tr>
 * <tr><td> useDisjunctive</td><td>boolean</td><td>false</td><td>Include in features giving disjunctions of words anywhere in the left or right disjunctionWidth words (preserving direction but not position)</td></tr>
 * <tr><td> disjunctionWidth</td><td>int</td><td>4</td><td>The number of words on each side of the current word that are included in the disjunction features</td></tr>
 * <tr><td> useDisjunctiveShapeInteraction</td><td>boolean</td><td>false</td><td>Include in features giving disjunctions of words anywhere in the left or right disjunctionWidth words (preserving direction but not position) interacting with the word shape of the current word</td></tr>
 * <tr><td> useWideDisjunctive</td><td>boolean</td><td>false</td><td>Include in features giving disjunctions of words anywhere in the left or right wideDisjunctionWidth words (preserving direction but not position)</td></tr>
 * <tr><td> wideDisjunctionWidth</td><td>int</td><td>4</td><td>The number of words on each side of the current word that are included in the disjunction features</td></tr>
 * <tr><td> usePosition</td><td>boolean</td><td>false</td><td>Use combination of position in sentence and class as a feature</td></tr>
 * <tr><td> useBeginSent</td><td>boolean</td><td>false</td><td>Use combination of initial position in sentence and class (and word shape) as a feature.  (Doesn't seem to help.)</td></tr>
 * <tr><td> useDisjShape</td><td>boolean</td><td>false</td><td>Include features giving disjunctions of word shapes anywhere in the left or right disjunctionWidth words (preserving direction but not position)</td></tr>
 * <tr><td> useClassFeature</td><td>boolean</td><td>false</td><td>Include a feature for the class (as a class marginal)</td></tr>
 * <tr><td> useShapeConjunctions</td><td>boolean</td><td>false</td><td>Conjoin shape with tag or position</td></tr>
 * <tr><td> useWordTag</td><td>boolean</td><td>false</td><td>Include word and tag pair features</td></tr>
 * <tr><td> useLastRealWord</td><td>boolean</td><td>false</td><td>Iff the prev word is of length 3 or less, add an extra feature that combines the word two back and the current word's shape. <i>Weird!</i></td></tr>
 * <tr><td> useNextRealWord</td><td>boolean</td><td>false</td><td>Iff the next word is of length 3 or less, add an extra feature that combines the word after next and the current word's shape. <i>Weird!</i></td></tr>
 * <tr><td> useTitle</td><td>boolean</td><td>false</td><td>Match a word against a list of name titles (Mr, Mrs, etc.)</td></tr>
 * <tr><td> useOccurrencePatterns</td><td>boolean</td><td>false</td><td>This is a very engineered feature designed to capture multiple references to names.  If the current word isn't capitalized, followed by a non-capitalized word, and preceded by a word with alphabetic characters, it returns NO-OCCURRENCE-PATTERN.  Otherwise, if the previous word is a capitalized NNP, then if in the next 150 words you find this PW-W sequence, you get XY-NEXT-OCCURRENCE-XY, else if you find W you get XY-NEXT-OCCURRENCE-Y.  Similarly for backwards and XY-PREV-OCCURRENCE-XY and XY-PREV-OCCURRENCE-Y.  Else (if the previous word isn't a capitalized NNP), under analogous rules you get one or more of X-NEXT-OCCURRENCE-YX, X-NEXT-OCCURRENCE-XY, X-NEXT-OCCURRENCE-X, X-PREV-OCCURRENCE-YX, X-PREV-OCCURRENCE-XY, X-PREV-OCCURRENCE-X.</td></tr>
 * <tr><td> useTypeySequences</td><td>boolean</td><td>false</td><td>Some first order word shape patterns.</td></tr>
 * <tr><td> justify</td><td>boolean</td><td>false</td><td>Print out all
 * feature/class pairs and their weight, and then for each input data
 * point, print justification (weights) for active features</td></tr>
 * <tr><td> normalize</td><td>boolean</td><td>false</td><td>For the CMMClassifier (only) if this is true then the Scorer normalizes scores as probabilities.</td></tr>
 * <tr><td> useHuber</td><td>boolean</td><td>false</td><td>Use a Huber loss prior rather than the default quadratic loss.</td></tr>
 * <tr><td> useQuartic</td><td>boolean</td><td>false</td><td>Use a Quartic prior rather than the default quadratic loss.</td></tr>
 * <tr><td> sigma</td><td>double</td><td>1.0</td><td></td></tr>
 * <tr><td> epsilon</td><td>double</td><td>0.01</td><td>Used only as a parameter in the Huber loss: this is the distance from 0 at which the loss changes from quadratic to linear</td></tr>
 * <tr><td> beamSize</td><td>int</td><td>30</td><td></td></tr>
 * <tr><td> maxLeft</td><td>int</td><td>2</td><td>The number of things to the left that have to be cached to run the Viterbi algorithm: the maximum context of class features used.</td></tr>
 * <tr><td> dontExtendTaggy</td><td>boolean</td><td>false</td><td>Don't extend the range of useTaggySequences when maxLeft is increased.</td></tr>
 * <tr><td> numFolds </td><td>int</td><td>1</td><td>The number of folds to use for cross-validation.</td></tr>
 * <tr><td> startFold </td><td>int</td><td>1</td><td>The starting fold to run.</td></tr>
 * <tr><td> numFoldsToRun </td><td>int</td><td>1</td><td>The number of folds to run.</td></tr>
 * <tr><td> mergeTags </td><td>boolean</td><td>false</td><td>Whether to merge B- and I- tags.</td></tr>
 * <tr><td> splitDocuments</td><td>boolean</td><td>true</td><td>Whether or not to split the data into seperate documents for training/testing</td></tr>
 * </table>
 * <p/>
 * You always get the current word as a feature (w,c).
 * <p/>
 * Note: flags/properties overwrite left to right.  That is, the parameter
 * setting specified <i>last</i> is the one used.
 * <p/>
 * <pre>
 * DOCUMENTATION ON FEATURE TEMPLATES
 * <p/>
 * w = word
 * t = tag
 * p = position (word index in sentence)
 * c = class
 * p = paren
 * g = gazette
 * a = abbrev
 * s = shape
 * r = regent (dependency governor)
 * h = head word of phrase
 * n(w) = ngrams from w
 * g(w) = gazette entries containing w
 * l(w) = length of w
 * o(...) = occurrence patterns of words
 * <p/>
 * useReverse reverses meaning of prev, next everywhere below (on in macro)
 * <p/>
 * "Prolog" booleans: , = AND and ; = OR
 * <p/>
 * Mac: Y = turned on in -macro,
 *      + = additional positive things relative to -macro for CoNLL NERFeatureFactory
 *          (perhaps none...)
 *      - = Known negative for CoNLL NERFeatureFactory relative to -macro
 * <p/>
 * Bio: + = additional things that are positive for BioCreative
 *      - = things negative relative to -macro
 * <p/>
 * HighMagnitude: There are no (0) to a few (+) to many (+++) high weight
 * features of this template. (? = not used in goodCoNLL, but usually = 0)
 * <p/>
 * Feature              Mac Bio CRFFlags                   HighMagnitude
 * ---------------------------------------------------------------------
 * w,c                    Y     useWord                    0 (useWord is almost useless with unlimited ngram features, but helps a fraction in goodCoNLL, if only because of prior fiddling
 * p,c                          usePosition                ?
 * p=0,c                        useBeginSent               ?
 * p=0,s,c                      useBeginSent               ?
 * t,c                    Y     useTags                    ++
 * pw,c                   Y     usePrev                    +
 * pt,c                   Y     usePrev,useTags            0
 * nw,c                   Y     useNext                    ++
 * nt,c                   Y     useNext,useTags            0
 * pw,w,c                 Y     useWordPairs               +
 * w,nw,c                 Y     useWordPairs               +
 * pt,t,nt,c                    useSymTags                 ?
 * t,nt,c                       useSymTags                 ?
 * pt,t,c                       useSymTags                 ?
 * pw,nw,c                      useSymWordPairs            ?
 * <p/>
 * pc,c                   Y     usePrev,useSequences,usePrevSequences   +++
 * pc,w,c                 Y     usePrev,useSequences,usePrevSequences   0
 * nc,c                         useNext,useSequences,useNextSequences   ?
 * w,nc,c                       useNext,useSequences,useNextSequences   ?
 * pc,nc,c                      useNext,usePrev,useSequences,usePrevSequences,useNextSequences  ?
 * w,pc,nc,c                    useNext,usePrev,useSequences,usePrevSequences,useNextSequences   ?
 * <p/>
 * (pw;p2w;p3w;p4w),c        +  useDisjunctive  (out to disjunctionWidth now)   +++
 * (nw;n2w;n3w;n4w),c        +  useDisjunctive  (out to disjunctionWidth now)   ++++
 * (pw;p2w;p3w;p4w),s,c      +  useDisjunctiveShapeInteraction          ?
 * (nw;n2w;n3w;n4w),s,c      +  useDisjunctiveShapeInteraction          ?
 * (pw;p2w;p3w;p4w),c        +  useWideDisjunctive (to wideDisjunctionWidth)   ?
 * (nw;n2w;n3w;n4w),c        +  useWideDisjunctive (to wideDisjunctionWidth)   ?
 * (ps;p2s;p3s;p4s),c           useDisjShape  (out to disjunctionWidth now)   ?
 * (ns;n2s;n3s;n4s),c           useDisjShape  (out to disjunctionWidth now)   ?
 * <p/>
 * pt,pc,t,c              Y     useTaggySequences                        +
 * p2t,p2c,pt,pc,t,c      Y     useTaggySequences,maxLeft&gt;=2          +
 * p3t,p3c,p2t,p2c,pt,pc,t,c Y  useTaggySequences,maxLeft&gt;=3,!dontExtendTaggy   ?
 * p2c,pc,c               Y     useLongSequences                         ++
 * p3c,p2c,pc,c           Y     useLongSequences,maxLeft&gt;=3           ?
 * p4c,p3c,p2c,pc,c       Y     useLongSequences,maxLeft&gt;=4           ?
 * p2c,pc,c,pw=BOUNDARY         useBoundarySequences                     0 (OK, but!)
 * <p/>
 * p2t,pt,t,c             -     useExtraTaggySequences                   ?
 * p3t,p2t,pt,t,c         -     useExtraTaggySequences                   ?
 * <p/>
 * p2t,pt,t,s,p2c,pc,c    -     useTaggySequencesShapeInteraction        ?
 * p3t,p2t,pt,t,s,p3c,p2c,pc,c  useTaggySequencesShapeInteraction        ?
 * <p/>
 * s,pc,c                 Y     useTypeySequences                        ++
 * ns,pc,c                Y     useTypeySequences  // error for ps? not? 0
 * ps,pc,s,c              Y     useTypeySequences                        0
 * // p2s,p2c,ps,pc,s,c      Y     useTypeySequences,maxLeft&gt;=2 // duplicated a useTypeSeqs2 feature
 * <p/>
 * n(w),c                 Y     useNGrams (noMidNGrams, MaxNGramLeng, lowercaseNGrams, dehyphenateNGrams)   +++
 * n(w),s,c                     useNGrams,conjoinShapeNGrams             ?
 * <p/>
 * g,c                        + useGazFeatures   // test refining this?   ?
 * pg,pc,c                    + useGazFeatures                           ?
 * ng,c                       + useGazFeatures                           ?
 * // pg,g,c                    useGazFeatures                           ?
 * // pg,g,ng,c                 useGazFeatures                           ?
 * // p2g,p2c,pg,pc,g,c         useGazFeatures                           ?
 * g,w,c                        useMoreGazFeatures                       ?
 * pg,pc,g,c                    useMoreGazFeatures                       ?
 * g,ng,c                       useMoreGazFeatures                       ?
 * <p/>
 * g(w),c                       useGazette,sloppyGazette (contains same word)   ?
 * g(w),[pw,nw,...],c           useGazette,cleanGazette (entire entry matches)   ?
 * <p/>
 * s,c                    Y     wordShape &gt;= 0                       +++
 * ps,c                   Y     wordShape &gt;= 0,useTypeSeqs           +
 * ns,c                   Y     wordShape &gt;= 0,useTypeSeqs           +
 * pw,s,c                 Y     wordShape &gt;= 0,useTypeSeqs           +
 * s,nw,c                 Y     wordShape &gt;= 0,useTypeSeqs           +
 * ps,s,c                 Y     wordShape &gt;= 0,useTypeSeqs           0
 * s,ns,c                 Y     wordShape &gt;= 0,useTypeSeqs           ++
 * ps,s,ns,c              Y     wordShape &gt;= 0,useTypeSeqs           ++
 * pc,ps,s,c              Y     wordShape &gt;= 0,useTypeSeqs,useTypeSeqs2   0
 * p2c,p2s,pc,ps,s,c      Y     wordShape &gt;= 0,useTypeSeqs,useTypeSeqs2,maxLeft&gt;=2   +++
 * pc,ps,s,ns,c                 wordShape &gt;= 0,useTypeSeqs,useTypeSeqs3   ?
 * <p/>
 * p2w,s,c if l(pw) &lt;= 3 Y     useLastRealWord // weird features, but work   0
 * n2w,s,c if l(nw) &lt;= 3 Y     useNextRealWord                        ++
 * o(pw,w,nw),c           Y     useOccurrencePatterns // don't fully grok but has to do with capitalized name patterns   ++
 * <p/>
 * a,c                          useAbbr;useMinimalAbbr
 * pa,a,c                       useAbbr
 * a,na,c                       useAbbr
 * pa,a,na,c                    useAbbr
 * pa,pc,a,c                    useAbbr;useMinimalAbbr
 * p2a,p2c,pa,pc,a              useAbbr
 * w,a,c                        useMinimalAbbr
 * p2a,p2c,a,c                  useMinimalAbbr
 * <p/>
 * RESTR. w,(pw,pc;p2w,p2c;p3w,p3c;p4w,p4c)   + useParenMatching,maxLeft&gt;=n
 * <p/>
 * c                          - useClassFeature                    
 * <p/>
  * p,s,c                      - useShapeConjunctions
 * t,s,c                      - useShapeConjunctions
 * <p/>
 * w,t,c                      + useWordTag                      ?
 * w,pt,c                     + useWordTag                      ?
 * w,nt,c                     + useWordTag                      ?
 * <p/>
 * r,c                          useNPGovernor (only for baseNP words)
 * r,t,c                        useNPGovernor (only for baseNP words)
 * h,c                          useNPHead (only for baseNP words)
 * h,t,c                        useNPHead (only for baseNP words)
 * <p/>
 * </pre>
 *
 * @author Dan Klein
 * @author Jenny Finkel
 * @author Christopher Manning
 * @author Shipra Dingare
 * @author Huy Nguyen
 */
public class NERFeatureFactory extends FeatureFactory {

  private static final long serialVersionUID = -2329726064739185544L;

  public NERFeatureFactory() {
    super();
  }

  public void init(SeqClassifierFlags flags) {
    super.init(flags);
    initGazette();
    if (flags.useDistSim) {
      initLexicon();
    }
  }


  /**
   * Extracts all the features from the input data at a certain index.
   *
   * @param cInfo The complete data set as a List of WordInfo
   * @param loc  The index at which to extract features.
   */
  public Collection getCliqueFeatures(PaddedList<FeatureLabel> cInfo, int loc, Clique clique) {
    Collection features = new HashSet();

    if (clique == cliqueC) {
      // TODO: It'd usefully improve performance to make this one ""
      addAllInterningAndSuffixing(features, featuresC(cInfo, loc), "C");
    } else if (clique == cliqueCpC) {
      addAllInterningAndSuffixing(features, featuresCpC(cInfo, loc), "CpC");
      addAllInterningAndSuffixing(features, featuresCnC(cInfo, loc-1), "CnC");
    } else if (clique == cliqueCp2C) {
      addAllInterningAndSuffixing(features, featuresCp2C(cInfo, loc), "Cp2C");
    } else if (clique == cliqueCp3C) {
      addAllInterningAndSuffixing(features, featuresCp3C(cInfo, loc), "Cp3C");
    } else if (clique == cliqueCp4C) {
      addAllInterningAndSuffixing(features, featuresCp4C(cInfo, loc), "Cp4C");
    } else if (clique == cliqueCp5C) {
      addAllInterningAndSuffixing(features, featuresCp5C(cInfo, loc), "Cp5C");
    } else if (clique == cliqueCpCp2C) {
      addAllInterningAndSuffixing(features, featuresCpCp2C(cInfo, loc), "CpCp2C");
      addAllInterningAndSuffixing(features, featuresCpCnC(cInfo, loc-1), "CpCnC");
    } else if (clique == cliqueCpCp2Cp3C) {
      addAllInterningAndSuffixing(features, featuresCpCp2Cp3C(cInfo, loc), "CpCp2Cp3C");
    } else if (clique == cliqueCpCp2Cp3Cp4C) {
      addAllInterningAndSuffixing(features, featuresCpCp2Cp3Cp4C(cInfo, loc), "CpCp2Cp3Cp4C");
    }

    return features;
  }


  private void addAllInterningAndSuffixing(Collection accumulator, Collection<String> addend, String suffix) {
    boolean nonNullSuffix = ! "".equals(suffix);
    for (String feat : addend) {
      if (nonNullSuffix) {    
        StringBuilder protoFeat = new StringBuilder(feat);
        protoFeat.append('|');
        protoFeat.append(suffix);
        feat = protoFeat.toString();
      }
      if (flags.intern2) {
        feat = feat.intern();
      }
      accumulator.add(feat);
    }
  }

  // TODO: when breaking serialization, it seems like it would be better to
  // move the lexicon into (Abstract)SequenceClassifier and to do this
  // annotation as part of the ObjectBankWrapper.  But note that it is 
  // serialized in this object currently and it would then need to be 
  // serialized elsewhere or loaded each time
  private Map<String,String> lexicon; 
  
  private void initLexicon() {
    if (flags.distSimLexicon == null) { 
      return;
    }
    if (lexicon != null) {
      return;
    }
    Timing.startDoing("Loading distsim lexicon from " + flags.distSimLexicon);
    lexicon = new HashMap<String, String>();
    String[] words = StringUtils.slurpFileNoExceptions(flags.distSimLexicon).split("\n");
    for (String word : words) {
      String[] bits = word.split("\\s+");
      lexicon.put(bits[0].toLowerCase(), bits[1]);
    }
    Timing.endDoing();
  }


  // TODO: Delete me when breaking serialization.  This is used to record
  // whether you have already calculated distsim info for this list.  It's 
  // kind of ugly.
  private PaddedList<FeatureLabel> cache = null;  
 
  private void distSimAnnotate(PaddedList<FeatureLabel> info) {
    if (info.sameInnerList(cache)) { return; }
    for (FeatureLabel fl : info) {
      String distSim = lexicon.get(fl.word().toLowerCase());
      if (distSim == null) { distSim = "null"; }
      fl.set("distSim", distSim);
    }
    cache = info;
  }


  private Map<String,Collection<String>> wordToSubstrings = new HashMap<String,Collection<String>>();

  public void clearSubstringList() {
    wordToSubstrings = new HashMap<String,Collection<String>>();
  }

  private static String dehyphenate(String str) {
    // don't take out leading or ending ones, just internal
    // and remember padded with < > characters
    String retStr = str;
    int leng = str.length();
    int hyphen = 2;
    do {
      hyphen = retStr.indexOf('-', hyphen);
      if (hyphen >= 0 && hyphen < leng - 2) {
        retStr = retStr.substring(0, hyphen) + retStr.substring(hyphen + 1);
      } else {
        hyphen = -1;
      }
    } while (hyphen >= 0);
    return retStr;
  }

  private static String greekify(String str) {
    // don't take out leading or ending ones, just internal
    // and remember padded with < > characters

    String pattern = "(alpha)|(beta)|(gamma)|(delta)|(epsilon)|(zeta)|(kappa)|(lambda)|(rho)|(sigma)|(tau)|(upsilon)|(omega)";

    Pattern p = Pattern.compile(pattern);
    Matcher m = p.matcher(str);
    return m.replaceAll("~");
  }

  /** end methods that do transformations **/

  /*
   * static booleans that check strings for certain qualities *
   */

  // cdm: this could be improved to handle more name types, such as
  // O'Reilly, DeGuzman, etc. (need a little classifier?!?)
  private static boolean isNameCase(String str) {
    if (str.length() < 2) {
      return false;
    }
    if (!(Character.isUpperCase(str.charAt(0)) || Character.isTitleCase(str.charAt(0)))) {
      return false;
    }
    for (int i = 1; i < str.length(); i++) {
      if (Character.isUpperCase(str.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  private static boolean noUpperCase(String str) {
    if (str.length() < 1) {
      return false;
    }
    for (int i = 0; i < str.length(); i++) {
      if (Character.isUpperCase(str.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  private static boolean hasLetter(String str) {
    if (str.length() < 1) {
      return false;
    }
    for (int i = 0; i < str.length(); i++) {
      if (Character.isLetter(str.charAt(i))) {
        return true;
      }
    }
    return false;
  }
  
  private static final Pattern ordinalPattern = Pattern.compile("(?:(?:first|second|third|fourth|fifth|"+
                                                          "sixth|seventh|eighth|ninth|tenth|"+
                                                          "eleventh|twelfth|thirteenth|"+
                                                          "fourteenth|fifteenth|sixteenth|"+
                                                          "seventeenth|eighteenth|nineteenth|"+
                                                          "twenty|twentieth|thirty|thirtieth|"+
                                                          "fourty|fourtieth|fifty|fiftieth|"+
                                                          "sixty|sixtieth|seventy|seventieth|"+
                                                          "eighty|eightieth|ninety|ninetieth|"+
                                                          "one|two|three|four|five|six|seven|"+
                                                          "eight|nine|hundred|hundredth)-?)+|[0-9]+(?:st|nd|rd|th)", Pattern.CASE_INSENSITIVE);
  
  
  private static final Pattern numberPattern = Pattern.compile("[0-9]+");
  private static final Pattern ordinalEndPattern = Pattern.compile("(?:st|nd|rd|th)", Pattern.CASE_INSENSITIVE);
  
  private static boolean isOrdinal(List<FeatureLabel> wordInfos, int pos) {
    FeatureLabel c = wordInfos.get(pos);
    Matcher m = ordinalPattern.matcher(c.word());
    if (m.matches()) { return true; }
    m = numberPattern.matcher(c.word());
    if (m.matches()) {
      if (pos+1 < wordInfos.size()) {
        FeatureLabel n = wordInfos.get(pos+1);
        m = ordinalEndPattern.matcher(n.word());
        if (m.matches()) { return true; }
      }
      return false;
    }
    
    m = ordinalEndPattern.matcher(c.word());
    if (m.matches()) {
      if (pos > 0) {
        FeatureLabel p = wordInfos.get(pos-1);
        m = numberPattern.matcher(p.word());
        if (m.matches()) { return true; }
      }
    }
    if (c.word().equals("-")) {
      if (pos+1 < wordInfos.size() && pos > 0) {
        FeatureLabel p = wordInfos.get(pos-1);
        FeatureLabel n = wordInfos.get(pos+1);
        m = ordinalPattern.matcher(p.word());
        if (m.matches()) {
          m = ordinalPattern.matcher(n.word());
          if (m.matches()) {
            return true;
          }
        }
      }
    }
    return false;
  }
  
  /* end static booleans that check strings for certain qualities */

  /**
   * Gazette Stuff.
   */

  static class GazetteInfo {
    String feature = "";
    int loc = 0;
    String[] words = StringUtils.EMPTY_STRING_ARRAY;
  } // end class GazetteInfo

  private Map<String,Collection<String>> wordToGazetteEntries = new HashMap<String,Collection<String>>();
  private Map<String,Collection<GazetteInfo>> wordToGazetteInfos = new HashMap<String,Collection<GazetteInfo>>();

  private void readGazette(BufferedReader in) throws IOException {
    Pattern p = Pattern.compile("^(\\S+)\\s+(.+)$");
    String line;
    while ((line = in.readLine()) != null) {
      Matcher m = p.matcher(line);
      if (m.matches()) {
        String type = intern(m.group(1));
        String phrase = m.group(2);
        String[] words = phrase.split(" ");
        for (int i = 0; i < words.length; i++) {
          String word = intern(words[i]);
          if (flags.sloppyGazette) {
            Collection<String> entries = wordToGazetteEntries.get(word);
            if (entries == null) {
              entries = new HashSet<String>();
              wordToGazetteEntries.put(word, entries);
            }
            String feature = intern(type + "-GAZ" + words.length);
            entries.add(feature);
          }
          if (flags.cleanGazette) {
            Collection<GazetteInfo> infos = wordToGazetteInfos.get(word);
            if (infos == null) {
              infos = new HashSet<GazetteInfo>();
              wordToGazetteInfos.put(word, infos);
            }
            GazetteInfo info = new GazetteInfo();
            info.loc = i;
            info.words = words;
            info.feature = intern(type + "-GAZ" + words.length);
            infos.add(info);
          }
        }
      }
    }
  }

  private HashSet<String> lastNames; // = null;
  private HashSet<String> maleNames; // = null;
  private HashSet<String> femaleNames; // = null;

  private Pattern titlePattern = Pattern.compile("(Mr|Ms|Mrs|Dr|Miss|Sen|Judge|Sir)\\.?");
  
  protected Collection<String> featuresC(PaddedList<FeatureLabel> cInfo, int loc) {
    FeatureLabel c = cInfo.get(loc);
    FeatureLabel n = cInfo.get(loc + 1);
    FeatureLabel n2 = cInfo.get(loc + 2);
    FeatureLabel p = cInfo.get(loc - 1);
    FeatureLabel p2 = cInfo.get(loc - 2);
    FeatureLabel p3 = cInfo.get(loc - 3);

    String cWord = c.word();

    Collection<String> featuresC = new ArrayList<String>();

    if (flags.useDistSim) {
      distSimAnnotate(cInfo);
    }
    
    if (flags.useDistSim && flags.useMoreTags) {
      featuresC.add(p.get("distSim") + "-" + cWord + "-PDISTSIM-CWORD");
    }

    if (flags.useDistSim) {
      featuresC.add(c.get("distSim") + "-DISTSIM");
    }
    

    if (flags.useTitle) {
      Matcher m = titlePattern.matcher(cWord);
      if (m.matches()) {
        featuresC.add("IS_TITLE");
      }
    }

    
    if (flags.useInternal && flags.useExternal ) {
    
      if (flags.useWord) {
        featuresC.add(cWord + "-WORD");
      }

      if (flags.useUnknown) { // for true casing
        featuresC.add(c.get("unknown")+"-UNKNOWN");
        featuresC.add(p.get("unknown")+"-PUNKNOWN");
        featuresC.add(n.get("unknown")+"-NUNKNOWN");
      }

      if (flags.useLemmas) {
        String lem = c.getString("lemma");
        if (! "".equals(lem)) {
          featuresC.add(lem + "-LEM");
        }
      }
      if (flags.usePrevNextLemmas) {
        String plem = p.getString("lemma");
        String nlem = n.getString("lemma");
        if (! "".equals(plem)) {
          featuresC.add(plem + "-PLEM");
        }
        if (! "".equals(nlem)) {
          featuresC.add(nlem + "-NLEM");
        }
      }

      if (flags.checkNameList) {
        try {
          if (lastNames == null) {
            lastNames = new HashSet<String>();
            String[] names = StringUtils.slurpFile(flags.lastNameList).split("\n");
            for (String line : names) {
              String[] cols = line.split("\\s+");
              lastNames.add(cols[0]);
            }
          }
          if (maleNames == null) {
            maleNames = new HashSet<String>();
            String[] names = StringUtils.slurpFile(flags.maleNameList).split("\n");
            for (String line : names) {
              String[] cols = line.split("\\s+");
              maleNames.add(cols[0]);
            }
          }
          if (femaleNames == null) {
            femaleNames = new HashSet<String>();
            String[] names = StringUtils.slurpFile(flags.femaleNameList).split("\n");
            for (String line : names) {
              String[] cols = line.split("\\s+");
              femaleNames.add(cols[0]);
            }
          }
        
          String name = cWord.toUpperCase();
          if (lastNames.contains(name)) {
            featuresC.add("LAST_NAME");
          }

          if (maleNames.contains(name)) {
            featuresC.add("MALE_NAME");
          }

          if (femaleNames.contains(name)) {
            featuresC.add("FEMALE_NAME");
          }

        } catch (Exception e) {
          e.printStackTrace();
          throw new RuntimeException();
        }
      }

      if (flags.binnedLengths != null) {
        int len = cWord.length();
        String featureName = null;
        for (int i = 0; i <= flags.binnedLengths.length; i++) {
          if (i == flags.binnedLengths.length) {
            featureName = "Len-" + flags.binnedLengths[flags.binnedLengths.length - 1] + "-Inf";
          } else if (len <= flags.binnedLengths[i]) {
            featureName = "Len-" + ((i == 0) ? 1 : flags.binnedLengths[i - 1]) + "-" + flags.binnedLengths[i];
            break;
          }
        }
        featuresC.add(featureName);
      }

      if (flags.useABGENE) {
        featuresC.add(c.get("abgene") + "-ABGENE");
        featuresC.add(p.get("abgene") + "-PABGENE");
        featuresC.add(n.get("abgene") + "-NABGENE");
      }

      if (flags.useABSTRFreqDict) {
        featuresC.add(c.get("abstr") + "-ABSTRACT" + c.get("freq") + "-FREQ" + c.get("tag") + "-TAG");
        featuresC.add(c.get("abstr") + "-ABSTRACT" + c.get("dict") + "-DICT" + c.get("tag") + "-TAG");
        featuresC.add(c.get("abstr") + "-ABSTRACT" + c.get("dict") + "-DICT" + c.get("freq") + "-FREQ" + c.get("tag") + "-TAG");
      }

      if (flags.useABSTR) {
        featuresC.add(c.get("abstr") + "-ABSTRACT");
        featuresC.add(p.get("abstr") + "-PABSTRACT");
        featuresC.add(n.get("abstr") + "-NABSTRACT");
      }

      if (flags.useGENIA) {
        featuresC.add(c.get("genia") + "-GENIA");
        featuresC.add(p.get("genia") + "-PGENIA");
        featuresC.add(n.get("genia") + "-NGENIA");
      }
      if (flags.useWEBFreqDict) {
        featuresC.add(c.get("web") + "-WEB" + c.get("freq") + "-FREQ" + c.get("tag") + "-TAG");
        featuresC.add(c.get("web") + "-WEB" + c.get("dict") + "-DICT" + c.get("tag") + "-TAG");
        featuresC.add(c.get("web") + "-WEB" + c.get("dict") + "-DICT" + c.get("freq") + "-FREQ" + c.get("tag") + "-TAG");
      }

      if (flags.useWEB) {
        featuresC.add(c.get("web") + "-WEB");
        featuresC.add(p.get("web") + "-PWEB");
        featuresC.add(n.get("web") + "-NWEB");
      }

      if (flags.useIsURL) {
        featuresC.add(c.get("isURL") + "-ISURL");
      }
      if (flags.useEntityRule) {
        featuresC.add(c.get("entityRule")+"-ENTITYRULE");
      }
      if (flags.useEntityTypes) {
        featuresC.add(c.get("entityType") + "-ENTITYTYPE");
      }
      if (flags.useIsDateRange) {
        featuresC.add(c.get("isDateRange") + "-ISDATERANGE");
      }

      if (flags.useABSTRFreq) {
        featuresC.add(c.get("abstr") + "-ABSTRACT" + c.get("freq") + "-FREQ");
      }

      if (flags.useFREQ) {
        featuresC.add(c.get("freq") + "-FREQ");
      }

      if (flags.useMoreTags) {
        featuresC.add(p.get("tag") + "-" + cWord + "-PTAG-CWORD");
      }

      if (flags.usePosition) {
        featuresC.add(c.get("position") + "-POSITION");
      }
      if (flags.useBeginSent) {
        if ("0".equals(c.get("position"))) {
          featuresC.add("BEGIN-SENT");
          featuresC.add(c.get("shape") + "-BEGIN-SENT");
        } else {
          featuresC.add("IN-SENT");
          featuresC.add(c.get("shape") + "-IN-SENT");
        }
      }
      if (flags.useTags) {
        featuresC.add(c.get("tag") + "-TAG");
      }
    
      if (flags.useOrdinal) {
        if (isOrdinal(cInfo, loc)) {
          featuresC.add("C_ORDINAL");
          if (isOrdinal(cInfo, loc-1)) {
            //System.err.print(p.word()+" ");
            featuresC.add("PC_ORDINAL");
          }
          //System.err.println(c.word());
        }
        if (isOrdinal(cInfo, loc-1)) {
          featuresC.add("P_ORDINAL");
        }
      }

      if (flags.usePrev) {
        featuresC.add(p.word() + "-PW");
        if (flags.useTags) {
          featuresC.add(p.get("tag") + "-PTAG");
        }
        if (flags.useDistSim) {
          featuresC.add(p.get("distSim") + "-PDISTSIM");
        }
        if (flags.useIsURL) {
          featuresC.add(p.get("isURL") + "-PISURL");
        }
        if (flags.useEntityTypes) {
          featuresC.add(p.get("entityType") + "-PENTITYTYPE");
        }
      }

      if (flags.useNext) {
        featuresC.add(n.word() + "-NW");
        if (flags.useTags) {
          featuresC.add(n.get("tag") + "-NTAG");
        }
        if (flags.useDistSim) {
          featuresC.add(p.get("distSim") + "-NDISTSIM");
        }        
        if (flags.useIsURL) {
          featuresC.add(n.get("isURL") + "-NISURL");
        }
        if (flags.useEntityTypes) {
          featuresC.add(n.get("entityType") + "-NENTITYTYPE");
        }
      }
      /*here, entityTypes refers to the type in the PASCAL IE challenge:
       * i.e. certain words are tagged "Date" or "Location" */

      if (flags.useEitherSideWord) {
        featuresC.add(p.word() + "-EW");
        featuresC.add(n.word() + "-EW");
      }

      if (flags.useWordPairs) {
        featuresC.add(cWord + "-" + p.word() + "-W-PW");
        featuresC.add(cWord + "-" + n.word() + "-W-NW");
      }

      if (flags.useSymTags) {
        if (flags.useTags) {
          featuresC.add(p.get("tag") + "-" + c.get("tag") + "-" + n.get("tag") + "-PCNTAGS");
          featuresC.add(c.get("tag") + "-" + n.get("tag") + "-CNTAGS");
          featuresC.add(p.get("tag") + "-" + c.get("tag") + "-PCTAGS");
        }
        if (flags.useDistSim) {
          featuresC.add(p.get("distSim") + "-" + c.get("distSim") + "-" + n.get("distSim") + "-PCNDISTSIM");
          featuresC.add(c.get("distSim") + "-" + n.get("distSim") + "-CNDISTSIM");
          featuresC.add(p.get("distSim") + "-" + c.get("distSim") + "-PCDISTSIM");
        }

      }

      if (flags.useSymWordPairs) {
        featuresC.add(p.word() + "-" + n.word() + "-SWORDS");
      }

      if (flags.useGazFeatures) {
        if (!c.get("gaz").equals(flags.dropGaz)) {
          featuresC.add(c.get("gaz") + "-GAZ");
        }
        if (!n.get("gaz").equals(flags.dropGaz)) {
          featuresC.add(n.get("gaz") + "-NGAZ");
        }
        if (!p.get("gaz").equals(flags.dropGaz)) {
          featuresC.add(p.get("gaz") + "-PGAZ");
        }
      }

      if (flags.useMoreGazFeatures) {
        if (!c.get("gaz").equals(flags.dropGaz)) {
          featuresC.add(c.get("gaz") + "-" + cWord + "-CG-CW-GAZ");
          if (!n.get("gaz").equals(flags.dropGaz)) {
            featuresC.add(c.get("gaz") + "-" + n.get("gaz") + "-CNGAZ");
          }
          if (!p.get("gaz").equals(flags.dropGaz)) {
            featuresC.add(p.get("gaz") + "-" + c.get("gaz") + "-PCGAZ");
          }
        }
      }

      if (flags.useAbbr || flags.useMinimalAbbr) {
        featuresC.add(c.get("abbr") + "-ABBR");
      }

      if (flags.useAbbr1 || flags.useMinimalAbbr1) {
        if (!c.get("abbr").equals("XX")) {
          featuresC.add(c.get("abbr") + "-ABBR");
        }
      }

      if (flags.useAbbr) {
        featuresC.add(p.get("abbr") + "-" + c.get("abbr") + "-PCABBR");
        featuresC.add(c.get("abbr") + "-" + n.get("abbr") + "-CNABBR");
        featuresC.add(p.get("abbr") + "-" + c.get("abbr") + "-" + n.get("abbr") + "-PCNABBR");
      }

      if (flags.useAbbr1) {
        if (!c.get("abbr").equals("XX")) {
          featuresC.add(p.get("abbr") + "-" + c.get("abbr") + "-PCABBR");
          featuresC.add(c.get("abbr") + "-" + n.get("abbr") + "-CNABBR");
          featuresC.add(p.get("abbr") + "-" + c.get("abbr") + "-" + n.get("abbr") + "-PCNABBR");
        }
      }

      if (flags.useChunks) {
        featuresC.add(p.get("chunk") + "-" + c.get("chunk") + "-PCCHUNK");
        featuresC.add(c.get("chunk") + "-" + n.get("chunk") + "-CNCHUNK");
        featuresC.add(p.get("chunk") + "-" + c.get("chunk") + "-" + n.get("chunk") + "-PCNCHUNK");
      }

      if (flags.useMinimalAbbr) {
        featuresC.add(cWord + "-" + c.get("abbr") + "-CWABB");
      }

      if (flags.useMinimalAbbr1) {
        if (!c.get("abbr").equals("XX")) {
          featuresC.add(cWord + "-" + c.get("abbr") + "-CWABB");
        }
      }

      String prevVB = "", nextVB = "";
      if (flags.usePrevVB) {
        FeatureLabel wi;
        for (int j = loc - 1; ; j--) {
          wi = cInfo.get(j);
          if (wi == cInfo.getPad()) {
            prevVB = "X";
            featuresC.add("X-PVB");
            break;
          } else if (((String)wi.get("tag")).startsWith("VB")) {
            featuresC.add(wi.word() + "-PVB");
            prevVB = wi.word();
            break;
          }
        }
      }

      if (flags.useNextVB) {
        FeatureLabel wi;
        for (int j = loc + 1; ; j++) {
          wi = cInfo.get(j);
          if (wi == cInfo.getPad()) {
            featuresC.add("X-NVB");
            nextVB = "X";
            break;
          } else if (((String)wi.get("tag")).startsWith("VB")) {
            featuresC.add(wi.word() + "-NVB");
            nextVB = wi.word();
            break;
          }
        }
      }

      if (flags.useVB) {
        featuresC.add(prevVB + "-" + nextVB + "-PNVB");
      }

      if (flags.useShapeConjunctions) {
        featuresC.add(c.get("position") + c.shape() + "-POS-SH");
        if (flags.useTags) {
          featuresC.add(c.tag() + c.shape() + "-TAG-SH");
        }
        if (flags.useDistSim) {
          featuresC.add(c.get("distSim") + c.shape() + "-DISTSIM-SH");
        }

      }

      if (flags.useWordTag) {
        featuresC.add(c.word() + "-" + c.get("tag") + "-W-T");
        featuresC.add(c.word() + "-" + p.get("tag") + "-W-PT");
        featuresC.add(c.word() + "-" + n.get("tag") + "-W-NT");
      }

      if (flags.useNPHead) {
        featuresC.add(c.get("head") + "-HW");
        if (flags.useTags) {
          featuresC.add(c.get("head") + "-" + c.get("tag") + "-HW-T");
        }
        if (flags.useDistSim) {
          featuresC.add(c.get("head") + "-" + c.get("distSim") + "-HW-DISTSIM");
        }
      }

      if (flags.useNPGovernor) {
        featuresC.add(c.get("governor") + "-GW");
        if (flags.useTags) {
          featuresC.add(c.get("governor") + "-" + c.get("tag") + "-GW-T");
        }
        if (flags.useDistSim) {
          featuresC.add(c.get("governor") + "-" + c.get("distSim") + "-DISTSIM-T1");
        }
      }

      if (flags.useHeadGov) {
        featuresC.add(c.get("head") + "-" + c.get("governor") + "-HW_GW");
      }

      if (flags.useClassFeature) {
        featuresC.add("###");
      }

      if (flags.useFirstWord) {
        String firstWord = cInfo.get(0).word();
        featuresC.add(firstWord);
      }

      if (flags.useNGrams) {
        Collection<String> subs = wordToSubstrings.get(cWord);
        if (subs == null) {
          subs = new ArrayList<String>();
          String word = "<" + cWord + ">";
          if (flags.lowercaseNGrams) {
            word = word.toLowerCase();
          }
          if (flags.dehyphenateNGrams) {
            word = dehyphenate(word);
          }
          if (flags.greekifyNGrams) {
            word = greekify(word);
          }
          for (int i = 0; i < word.length(); i++) {
            for (int j = i + 2; j <= word.length(); j++) {
              if (flags.noMidNGrams && i != 0 && j != word.length()) {
                continue;
              }
              if (flags.maxNGramLeng >= 0 && j - i > flags.maxNGramLeng) {
                continue;
              }
              subs.add(intern("#" + word.substring(i, j) + "#"));
            }
          }
          if (flags.cacheNGrams) {
            wordToSubstrings.put(cWord, subs);
          }
        }
        featuresC.addAll(subs);
        if (flags.conjoinShapeNGrams) {
          String shape = (String) c.get("shape");
          for (String str : subs) {
            String feat = str + "-" + shape + "-CNGram-CS";
            featuresC.add(feat);
          }
        }
      }

      if (flags.useGazettes) {
        if (flags.sloppyGazette) {
          Collection<String> entries = wordToGazetteEntries.get(cWord);
          if (entries != null) {
            featuresC.addAll(entries);
          }
        }
        if (flags.cleanGazette) {
          Collection<GazetteInfo> infos = wordToGazetteInfos.get(cWord);
          if (infos != null) {
            for (GazetteInfo gInfo : infos) {
              boolean ok = true;
              for (int gLoc = 0; gLoc < gInfo.words.length; gLoc++) {
                ok &= gInfo.words[gLoc].equals(cInfo.get(loc + gLoc - gInfo.loc).word());
              }
              if (ok) {
                featuresC.add(gInfo.feature);
              }
            }
          }
        }
      }

      if ((flags.wordShape > WordShapeClassifier.NOWORDSHAPE) || (flags.useShapeStrings)) {
        featuresC.add(c.get("shape") + "-TYPE");
        if (flags.useTypeSeqs) {
          String cType = (String) c.get("shape");
          String pType = (String) p.get("shape");
          String nType = (String) n.get("shape");
          featuresC.add(pType + "-PTYPE");
          featuresC.add(nType + "-NTYPE");
          featuresC.add(p.word() + "..." + cType + "-PW_CTYPE");
          featuresC.add(cType + "..." + n.word() + "-NW_CTYPE");
          featuresC.add(pType + "..." + cType + "-PCTYPE");
          featuresC.add(cType + "..." + nType + "-CNTYPE");
          featuresC.add(pType + "..." + cType + "..." + nType + "-PCNTYPE");
        }
      }

      if (flags.useLastRealWord) {
        if (p.word().length() <= 3) {
          // extending this to check for 2 short words doesn't seem to help....
          featuresC.add(p2.word() + "..." + c.get("shape") + "-PPW_CTYPE");
        }
      }

      if (flags.useNextRealWord) {
        if (n.word().length() <= 3) {
          // extending this to check for 2 short words doesn't seem to help....
          featuresC.add(n2.word() + "..." + c.get("shape") + "-NNW_CTYPE");
        }
      }

      if (flags.useOccurrencePatterns) {
        featuresC.addAll(occurrencePatterns(cInfo, loc));
      }

      if (flags.useDisjunctive) {
        for (int i = 1; i <= flags.disjunctionWidth; i++) {
          FeatureLabel dn = cInfo.get(loc + i);
          FeatureLabel dp = cInfo.get(loc - i);
          featuresC.add(dn.word() + "-DISJN");
          if (flags.useDisjunctiveShapeInteraction) {
            featuresC.add(dn.word() + "-" + c.get("shape") + "-DISJN-CS");
          }
          featuresC.add(dp.word() + "-DISJP");
          if (flags.useDisjunctiveShapeInteraction) {
            featuresC.add(dp.word() + "-" + c.get("shape") + "-DISJP-CS");
          }
        }
      }

      if (flags.useWideDisjunctive) {
        for (int i = 1; i <= flags.wideDisjunctionWidth; i++) {
          featuresC.add(cInfo.get(loc + i).word() + "-DISJWN");
          featuresC.add(cInfo.get(loc - i).word() + "-DISJWP");
        }
      }

      if (flags.useEitherSideDisjunctive) {
        for (int i = 1; i <= flags.disjunctionWidth; i++) {
          featuresC.add(cInfo.get(loc + i).word() + "-DISJWE");
          featuresC.add(cInfo.get(loc - i).word() + "-DISJWE");
        }
      }

      if (flags.useDisjShape) {
        for (int i = 1; i <= flags.disjunctionWidth; i++) {
          featuresC.add(cInfo.get(loc + i).get("shape") + "-NDISJSHAPE");
          // featuresC.add(cInfo.get(loc - i).get("shape") + "-PDISJSHAPE");
          featuresC.add(c.get("shape") + "-" + cInfo.get(loc + i).get("shape") + "-CNDISJSHAPE");
          // featuresC.add(c.get("shape") + "-" + cInfo.get(loc - i).get("shape") + "-CPDISJSHAPE");
        }
      }

      if (flags.useExtraTaggySequences) {
        if (flags.useTags) {
          featuresC.add(p2.get("tag") + "-" + p.get("tag") + "-" + c.get("tag") + "-TTS");
          featuresC.add(p3.get("tag") + "-" + p2.get("tag") + "-" + p.get("tag") + "-" + c.get("tag") + "-TTTS");
        }
        if (flags.useDistSim) {
          featuresC.add(p2.get("distSim") + "-" + p.get("distSim") + "-" + c.get("distSim") + "-DISTSIM_TTS1");
          featuresC.add(p3.get("distSim") + "-" + p2.get("distSim") + "-" + p.get("distSim") + "-" + c.get("distSim") + "-DISTSIM_TTTS1");
        }
      }

      if (flags.useMUCFeatures) {
        featuresC.add(c.get("section")+"-SECTION");
        featuresC.add(c.get("wordPos")+"-WORD_POSITION");
        featuresC.add(c.get("sentPos")+"-SENT_POSITION");
        featuresC.add(c.get("paraPos")+"-PARA_POSITION");
        featuresC.add(c.get("wordPos")+"-"+c.get("shape")+"-WORD_POSITION_SHAPE");
      }
    } else if (flags.useInternal) {
      
      if (flags.useWord) {
        featuresC.add(cWord + "-WORD");
      }

      if (flags.useNGrams) {
        Collection<String> subs = wordToSubstrings.get(cWord);
        if (subs == null) {
          subs = new ArrayList<String>();
          String word = "<" + cWord + ">";
          if (flags.lowercaseNGrams) {
            word = word.toLowerCase();
          }
          if (flags.dehyphenateNGrams) {
            word = dehyphenate(word);
          }
          if (flags.greekifyNGrams) {
            word = greekify(word);
          }
          for (int i = 0; i < word.length(); i++) {
            for (int j = i + 2; j <= word.length(); j++) {
              if (flags.noMidNGrams && i != 0 && j != word.length()) {
                continue;
              }
              if (flags.maxNGramLeng >= 0 && j - i > flags.maxNGramLeng) {
                continue;
              }
              //subs.add(intern("#" + word.substring(i, j) + "#"));
              subs.add(intern("#" + word.substring(i, j) + "#"));
            }
          }
          if (flags.cacheNGrams) {
            wordToSubstrings.put(cWord, subs);
          }
        }
        featuresC.addAll(subs);
        if (flags.conjoinShapeNGrams) {
          String shape = (String) c.get("shape");
          for (String str : subs) {
            String feat = str + "-" + shape + "-CNGram-CS";
            featuresC.add(feat);
          }
        }
      }

      if ((flags.wordShape > WordShapeClassifier.NOWORDSHAPE) || (flags.useShapeStrings)) {
        featuresC.add(c.get("shape") + "-TYPE");
      }

      if (flags.useOccurrencePatterns) {
        featuresC.addAll(occurrencePatterns(cInfo, loc));
      }
      
    } else if (flags.useExternal) {

      if (flags.usePrev) {
        featuresC.add(p.word() + "-PW");
      }

      if (flags.useNext) {
        featuresC.add(n.word() + "-NW");
      }
     
      if (flags.useWordPairs) {
        featuresC.add(cWord + "-" + p.word() + "-W-PW");
        featuresC.add(cWord + "-" + n.word() + "-W-NW");
      }

      if (flags.useSymWordPairs) {
        featuresC.add(p.word() + "-" + n.word() + "-SWORDS");
      }

      if ((flags.wordShape > WordShapeClassifier.NOWORDSHAPE) || (flags.useShapeStrings)) {
        if (flags.useTypeSeqs) {
          String cType = (String) c.get("shape");
          String pType = (String) p.get("shape");
          String nType = (String) n.get("shape");
          featuresC.add(pType + "-PTYPE");
          featuresC.add(nType + "-NTYPE");
          featuresC.add(p.word() + "..." + cType + "-PW_CTYPE");
          featuresC.add(cType + "..." + n.word() + "-NW_CTYPE");
          if (flags.maxLeft > 0) featuresC.add(pType + "..." + cType + "-PCTYPE"); // this one just isn't useful, at least given c,pc,s,ps.  Might be useful 0th-order
          featuresC.add(cType + "..." + nType + "-CNTYPE");
          featuresC.add(pType + "..." + cType + "..." + nType + "-PCNTYPE");
        }
      }

      if (flags.useLastRealWord) {
        if (p.word().length() <= 3) {
          featuresC.add(p2.word() + "..." + c.get("shape") + "-PPW_CTYPE");
        }
      }

      if (flags.useNextRealWord) {
        if (n.word().length() <= 3) {
          featuresC.add(n2.word() + "..." + c.get("shape") + "-NNW_CTYPE");
        }
      }

      if (flags.useDisjunctive) {
        for (int i = 1; i <= flags.disjunctionWidth; i++) {
          FeatureLabel dn = cInfo.get(loc + i);
          FeatureLabel dp = cInfo.get(loc - i);
          featuresC.add(dn.word() + "-DISJN");
          if (flags.useDisjunctiveShapeInteraction) {
            featuresC.add(dn.word() + "-" + c.get("shape") + "-DISJN-CS");
          }
          featuresC.add(dp.word() + "-DISJP");
          if (flags.useDisjunctiveShapeInteraction) {
            featuresC.add(dp.word() + "-" + c.get("shape") + "-DISJP-CS");
          }
        }
      }

      if (flags.useWideDisjunctive) {
        for (int i = 1; i <= flags.wideDisjunctionWidth; i++) {
          featuresC.add(cInfo.get(loc + i).word() + "-DISJWN");
          featuresC.add(cInfo.get(loc - i).word() + "-DISJWP");
        }
      }

      if (flags.useDisjShape) {
        for (int i = 1; i <= flags.disjunctionWidth; i++) {
          featuresC.add(cInfo.get(loc + i).get("shape") + "-NDISJSHAPE");
          // featuresC.add(cInfo.get(loc - i).get("shape") + "-PDISJSHAPE");
          featuresC.add(c.get("shape") + "-" + cInfo.get(loc + i).get("shape") + "-CNDISJSHAPE");
          // featuresC.add(c.get("shape") + "-" + cInfo.get(loc - i).get("shape") + "-CPDISJSHAPE");
        }
      }     
      
    }
    
    // Stuff to add binary features from the additional columns
    if (flags.twoStage) {
      featuresC.add(c.get("bin1") + "-BIN1");
      featuresC.add(c.get("bin2") + "-BIN2");
      featuresC.add(c.get("bin3") + "-BIN3");
      featuresC.add(c.get("bin4") + "-BIN4");
      featuresC.add(c.get("bin5") + "-BIN5");
      featuresC.add(c.get("bin6") + "-BIN6");
    }
    
    return featuresC;
  }


  protected Collection<String> featuresCpC(PaddedList<FeatureLabel> cInfo, int loc) {
    FeatureLabel c = cInfo.get(loc);
    FeatureLabel n = cInfo.get(loc + 1);
    FeatureLabel p = cInfo.get(loc - 1);

    String cWord = c.word();
    Collection<String> featuresCpC = new ArrayList<String>();

    if (flags.useInternal && flags.useExternal ) {

      if (flags.useOrdinal) {
        if (isOrdinal(cInfo, loc)) {
          featuresCpC.add("C_ORDINAL");
          if (isOrdinal(cInfo, loc-1)) {
            featuresCpC.add("PC_ORDINAL");
          }
        }
        if (isOrdinal(cInfo, loc-1)) {
          featuresCpC.add("P_ORDINAL");
        }
      }

      if (flags.useAbbr || flags.useMinimalAbbr) {
        featuresCpC.add(p.get("abbr") + "-" + c.get("abbr") + "-PABBRANS");
      }

      if (flags.useAbbr1 || flags.useMinimalAbbr1) {
        if (!c.get("abbr").equals("XX")) {
          featuresCpC.add(p.get("abbr") + "-" + c.get("abbr") + "-PABBRANS");
        }
      }

      if (flags.useChunkySequences) {
        featuresCpC.add(p.get("chunk") + "-" + c.get("chunk") + "-" + n.get("chunk") + "-PCNCHUNK");
      }

      if (flags.usePrev) {
        if (flags.useSequences && flags.usePrevSequences) {
          featuresCpC.add("PSEQ");
          featuresCpC.add(cWord + "-PSEQW");
        }
      }

      if( ((flags.wordShape > WordShapeClassifier.NOWORDSHAPE) || 
           flags.useShapeStrings)
          && flags.useTypeSeqs && (flags.useTypeSeqs2 || flags.useTypeSeqs3)) {
        String pType = (String) p.get("shape");
        String cType = (String) c.get("shape");
        if (flags.useTypeSeqs3) {
          featuresCpC.add(pType + "-" + cType + "-" + n.get("shape") + "-PCNSHAPES");
        }
        if (flags.useTypeSeqs2) {
          featuresCpC.add(pType + "-" + cType + "-TYPES");
        }
      }

      if (flags.useTypeySequences) {
        featuresCpC.add(c.get("shape") + "-TPS2");
        featuresCpC.add(n.get("shape") + "-TNS1");
        // featuresCpC.add(p.get("shape") + "-" + c.get("shape") + "-TPS"); // duplicates -TYPES, so now omitted; you may need to slighly increase sigma to duplicate previous results, however.
      }

      if (flags.useTaggySequences) {
        if (flags.useTags) {
          featuresCpC.add(p.get("tag") + "-" + c.get("tag") + "-TS");
        }
        if (flags.useDistSim) {
          featuresCpC.add(p.get("distSim") + "-" + c.get("distSim") + "-DISTSIM_TS1");
        }
      }

      if (flags.useParenMatching) {
        if (flags.useReverse) {
          if (cWord.equals("(") || cWord.equals("[") || cWord.equals("-LRB-")) {
            if (p.word().equals(")") || p.word().equals("]") || p.word().equals("-RRB-")) {
              featuresCpC.add("PAREN-MATCH");
            }
          }
        } else {
          if (cWord.equals(")") || cWord.equals("]") || cWord.equals("-RRB-")) {
            if (p.word().equals("(") || p.word().equals("[") || p.word().equals("-LRB-")) {
              featuresCpC.add("PAREN-MATCH");
            }
          }
        }
      }
      if (flags.useEntityTypeSequences) {
        featuresCpC.add(p.get("entityType") + "-" + c.get("entityType") + "-ETSEQ");
      }
      if (flags.useURLSequences) {
        featuresCpC.add(p.get("isURL") + "-" + c.get("isURL") + "-URLSEQ");
      }
    } else if (flags.useInternal) {
      
      if (flags.useSequences && flags.usePrevSequences) {
        featuresCpC.add("PSEQ");
        featuresCpC.add(cWord + "-PSEQW");
      }
      
      if (flags.useTypeySequences) {
        featuresCpC.add(c.get("shape") + "-TPS2");
      }

    } else if (flags.useExternal) {
      
      if( ((flags.wordShape > WordShapeClassifier.NOWORDSHAPE) || 
           flags.useShapeStrings)
          && flags.useTypeSeqs && (flags.useTypeSeqs2 || flags.useTypeSeqs3)) {
        String pType = (String) p.get("shape");
        String cType = (String) c.get("shape");
        if (flags.useTypeSeqs3) {
          featuresCpC.add(pType + "-" + cType + "-" + n.get("shape") + "-PCNSHAPES");
        }
        if (flags.useTypeSeqs2) {
          featuresCpC.add(pType + "-" + cType + "-TYPES");
        }
      }

      if (flags.useTypeySequences) {
        featuresCpC.add(n.get("shape") + "-TNS1");
        featuresCpC.add(p.get("shape") + "-" + c.get("shape") + "-TPS");
      }
    }

    return featuresCpC;
  }

  protected Collection<String> featuresCp2C(PaddedList<FeatureLabel> cInfo, int loc) {
    FeatureLabel c = cInfo.get(loc);
    FeatureLabel p = cInfo.get(loc - 1);
    FeatureLabel p2 = cInfo.get(loc - 2);

    String cWord = c.word();
    Collection<String> featuresCp2C = new ArrayList<String>();
    
    if (flags.useMoreAbbr) {
      featuresCp2C.add(p2.get("abbr") + "-" + c.get("abbr") + "-P2ABBRANS");
    }

    if (flags.useMinimalAbbr) {
      featuresCp2C.add(p2.get("abbr") + "-" + c.get("abbr") + "-P2AP2CABB");
    }

    if (flags.useMinimalAbbr1) {
      if (!c.get("abbr").equals("XX")) {
        featuresCp2C.add(p2.get("abbr") + "-" + c.get("abbr") + "-P2AP2CABB");
      }
    }

    if (flags.useParenMatching) {
      if (flags.useReverse) {
        if (cWord.equals("(") || cWord.equals("[") || cWord.equals("-LRB-")) {
          if ((p2.word().equals(")") || p2.word().equals("]") || p2.word().equals("-RRB-")) && ! (p.word().equals(")") || p.word().equals("]") || p.word().equals("-RRB-"))) {
            featuresCp2C.add("PAREN-MATCH");
          }
        }
      } else {
        if (cWord.equals(")") || cWord.equals("]") || cWord.equals("-RRB-")) {
          if ((p2.word().equals("(") || p2.word().equals("[") || p2.word().equals("-LRB-")) && ! (p.word().equals("(") || p.word().equals("[") || p.word().equals("-LRB-"))) {
            featuresCp2C.add("PAREN-MATCH");
          }
        }
      }
    }
    
    return featuresCp2C;
  }

  protected Collection<String> featuresCp3C(PaddedList<FeatureLabel> cInfo, int loc) {
    FeatureLabel c = cInfo.get(loc);
    FeatureLabel p = cInfo.get(loc - 1);
    FeatureLabel p2 = cInfo.get(loc - 2);
    FeatureLabel p3 = cInfo.get(loc - 3);

    String cWord = c.word();
    Collection<String> featuresCp3C = new ArrayList<String>();

    if (flags.useParenMatching) {
      if (flags.useReverse) {
        if (cWord.equals("(") || cWord.equals("[")) {
          if ((flags.maxLeft >= 3) && (p3.word().equals(")") || p3.word().equals("]")) && !(p2.word().equals(")") || p2.word().equals("]") || p.word().equals(")") || p.word().equals("]"))) {
            featuresCp3C.add("PAREN-MATCH");
          }
        }
      } else {
        if (cWord.equals(")") || cWord.equals("]")) {
          if ((flags.maxLeft >= 3) && (p3.word().equals("(") || p3.word().equals("[")) && !(p2.word().equals("(") || p2.word().equals("[") || p.word().equals("(") || p.word().equals("["))) {
            featuresCp3C.add("PAREN-MATCH");
          }
        }
      }
    }

    return featuresCp3C;
  }

  protected Collection<String> featuresCp4C(PaddedList<FeatureLabel> cInfo, int loc) {
    FeatureLabel c = cInfo.get(loc);
    FeatureLabel p = cInfo.get(loc - 1);
    FeatureLabel p2 = cInfo.get(loc - 2);
    FeatureLabel p3 = cInfo.get(loc - 3);
    FeatureLabel p4 = cInfo.get(loc - 4);

    String cWord = c.word();
    Collection<String> featuresCp4C = new ArrayList<String>();

    if (flags.useParenMatching) {
      if (flags.useReverse) {
        if (cWord.equals("(") || cWord.equals("[")) {
          if ((flags.maxLeft >= 4) && (p4.word().equals(")") || p4.word().equals("]")) && !(p3.word().equals(")") || p3.word().equals("]") || p2.word().equals(")") || p2.word().equals("]") || p.word().equals(")") || p.word().equals("]"))) {
            featuresCp4C.add("PAREN-MATCH");
          }
        }
      } else {
        if (cWord.equals(")") || cWord.equals("]")) {
          if ((flags.maxLeft >= 4) && (p4.word().equals("(") || p4.word().equals("[")) && !(p3.word().equals("(") || p3.word().equals("[") || p2.word().equals("(") || p2.word().equals("[") || p.word().equals("(") || p.word().equals("["))) {
            featuresCp4C.add("PAREN-MATCH");
          }
        }
      }
    }

    return featuresCp4C;
  }

  protected Collection<String> featuresCp5C(PaddedList<FeatureLabel> cInfo, int loc) {
    FeatureLabel c = cInfo.get(loc);
    FeatureLabel p = cInfo.get(loc - 1);
    FeatureLabel p2 = cInfo.get(loc - 2);
    FeatureLabel p3 = cInfo.get(loc - 3);
    FeatureLabel p4 = cInfo.get(loc - 4);
    FeatureLabel p5 = cInfo.get(loc - 5);

    String cWord = c.word();
    Collection<String> featuresCp5C = new ArrayList<String>();

    if (flags.useParenMatching) {
      if (flags.useReverse) {
        if (cWord.equals("(") || cWord.equals("[")) {
          if ((flags.maxLeft >= 5) && (p5.word().equals(")") || p5.word().equals("]")) && !(p4.word().equals(")") || p4.word().equals("]") || p3.word().equals(")") || p3.word().equals("]") || p2.word().equals(")") || p2.word().equals("]") || p.word().equals(")") || p.word().equals("]"))) {
            featuresCp5C.add("PAREN-MATCH");
          }
        }
      } else {
        if (cWord.equals(")") || cWord.equals("]")) {
          if ((flags.maxLeft >= 5) && (p5.word().equals("(") || p5.word().equals("[")) && !(p4.word().equals("(") || p4.word().equals("[") || p3.word().equals("(") || p3.word().equals("[") || p2.word().equals("(") || p2.word().equals("[") || p.word().equals("(") || p.word().equals("["))) {
            featuresCp5C.add("PAREN-MATCH");
          }
        }
      }
    }
    return featuresCp5C;
  }


  protected Collection<String> featuresCpCp2C(PaddedList<FeatureLabel> cInfo, int loc) {
    FeatureLabel c = cInfo.get(loc);
    FeatureLabel p = cInfo.get(loc - 1);
    FeatureLabel p2 = cInfo.get(loc - 2);

    Collection<String> featuresCpCp2C = new ArrayList<String>();

    if (flags.useInternal && flags.useExternal) {

      if (false && flags.useTypeySequences && flags.maxLeft >= 2) {  // this feature duplicates -TYPETYPES one below, so don't include it (hurts to duplicate)!!!
        featuresCpCp2C.add(p2.get("shape") + "-" + p.get("shape") + "-" + c.get("shape") + "-TTPS");
      }

      if (flags.useAbbr) {
        featuresCpCp2C.add(p2.get("abbr") + "-" + p.get("abbr") + "-" + c.get("abbr") + "-2PABBRANS");
      }

      if (flags.useChunks) {
        featuresCpCp2C.add(p2.get("chunk") + "-" + p.get("chunk") + "-" + c.get("chunk") + "-2PCHUNKS");
      }

      if (flags.useLongSequences) {
        featuresCpCp2C.add("PPSEQ");
      }
      if (flags.useBoundarySequences && p.word().equals(CoNLLDocumentReaderAndWriter.BOUNDARY)) {
        featuresCpCp2C.add("BNDRY-SPAN-PPSEQ");
      }
      // This more complex consistency checker didn't help!
      // if (flags.useBoundarySequences) {
      //   String pw = p.word();
      //   // try enforce consistency over "and" and "," as well as boundary now
      //   if (pw.equals(CoNLLDocumentIteratorFactory.BOUNDARY) ||
      //       pw.equalsIgnoreCase("and") || pw.equalsIgnoreCase("or") ||
      //       pw.equals(",")) {
      //   }
      // }

      if (flags.useTaggySequences) {
        if (flags.useTags) {
          featuresCpCp2C.add(p2.get("tag") + "-" + p.get("tag") + "-" + c.get("tag") + "-TTS");
          if (flags.useTaggySequencesShapeInteraction) {
            featuresCpCp2C.add(p2.get("tag") + "-" + p.get("tag") + "-" + c.get("tag") + "-" + c.get("shape") + "-TTS-CS");
          }
        }
        if (flags.useDistSim) {
          featuresCpCp2C.add(p2.get("distSim") + "-" + p.get("distSim") + "-" + c.get("distSim") + "-DISTSIM_TTS1");
          if (flags.useTaggySequencesShapeInteraction) {
            featuresCpCp2C.add(p2.get("distSim") + "-" + p.get("distSim") + "-" + c.get("distSim") + "-" + c.get("shape") + "-DISTSIM_TTS1-CS");
          }
        }
      }

      if (((flags.wordShape > WordShapeClassifier.NOWORDSHAPE) ||
           flags.useShapeStrings)
          && flags.useTypeSeqs && flags.useTypeSeqs2 && flags.maxLeft >= 2) {
        String cType = (String) c.get("shape");
        String pType = (String) p.get("shape");
        String p2Type = (String) p2.get("shape");
        featuresCpCp2C.add(p2Type + "-" + pType + "-" + cType + "-TYPETYPES");
      }
    } else if (flags.useInternal) {

      if (flags.useLongSequences) {
        featuresCpCp2C.add("PPSEQ");
      }      
    } else if (flags.useExternal) {
      
      if (flags.useLongSequences) {
        featuresCpCp2C.add("PPSEQ");
      }
      
      if (((flags.wordShape > WordShapeClassifier.NOWORDSHAPE) ||
           flags.useShapeStrings)
          && flags.useTypeSeqs && flags.useTypeSeqs2 && flags.maxLeft >= 2) {
        String cType = (String) c.get("shape");
        String pType = (String) p.get("shape");
        String p2Type = (String) p2.get("shape");
        featuresCpCp2C.add(p2Type + "-" + pType + "-" + cType + "-TYPETYPES");
      }
    }
    
    return featuresCpCp2C;
  }


  protected Collection<String> featuresCpCp2Cp3C(PaddedList<FeatureLabel> cInfo, int loc) {
    FeatureLabel c = cInfo.get(loc);
    FeatureLabel p = cInfo.get(loc - 1);
    FeatureLabel p2 = cInfo.get(loc - 2);
    FeatureLabel p3 = cInfo.get(loc - 3);

    Collection<String> featuresCpCp2Cp3C = new ArrayList<String>();

    if (flags.useTaggySequences) {
      if (flags.useTags) {
        if (flags.maxLeft >= 3 && !flags.dontExtendTaggy) {
          featuresCpCp2Cp3C.add(p3.get("tag") + "-" + p2.get("tag") + "-" + p.get("tag") + "-" + c.get("tag") + "-TTTS");
          if (flags.useTaggySequencesShapeInteraction) {
            featuresCpCp2Cp3C.add(p3.get("tag") + "-" + p2.get("tag") + "-" + p.get("tag") + "-" + c.get("tag") + "-" + c.get("shape") + "-TTTS-CS");
          }
        }
      }
      if (flags.useDistSim) {
        if (flags.maxLeft >= 3 && !flags.dontExtendTaggy) {
          featuresCpCp2Cp3C.add(p3.get("distSim") + "-" + p2.get("distSim") + "-" + p.get("distSim") + "-" + c.get("distSim") + "-DISTSIM_TTTS1");
          if (flags.useTaggySequencesShapeInteraction) {
            featuresCpCp2Cp3C.add(p3.get("distSim") + "-" + p2.get("distSim") + "-" + p.get("distSim") + "-" + c.get("distSim") + "-" + c.get("shape") + "-DISTSIM_TTTS1-CS");
          }
        }
      }
    }

    if (flags.maxLeft >= 3) {
      if (flags.useLongSequences) {
        featuresCpCp2Cp3C.add("PPPSEQ");
      }
      if (flags.useBoundarySequences && p.word().equals(CoNLLDocumentReaderAndWriter.BOUNDARY)) {
        featuresCpCp2Cp3C.add("BNDRY-SPAN-PPPSEQ");
      }
    }

    return featuresCpCp2Cp3C;
  }

  protected Collection<String> featuresCpCp2Cp3Cp4C(PaddedList<FeatureLabel> cInfo, int loc) {
    Collection<String> featuresCpCp2Cp3Cp4C = new ArrayList<String>();

    FeatureLabel p = cInfo.get(loc - 1);

    if (flags.maxLeft >= 4) {
      if (flags.useLongSequences) {
        featuresCpCp2Cp3Cp4C.add("PPPPSEQ");
      }
      if (flags.useBoundarySequences && p.word().equals(CoNLLDocumentReaderAndWriter.BOUNDARY)) {
        featuresCpCp2Cp3Cp4C.add("BNDRY-SPAN-PPPPSEQ");
      }
    }

    return featuresCpCp2Cp3Cp4C;
  }


  protected Collection<String> featuresCnC(PaddedList<FeatureLabel> cInfo, int loc) {
    FeatureLabel c = cInfo.get(loc);

    Collection<String> featuresCnC = new ArrayList<String>();

    if (flags.useNext) {
      if (flags.useSequences && flags.useNextSequences) {
        featuresCnC.add("NSEQ");
        featuresCnC.add(c.word() + "-NSEQW");
      }
    }

    return featuresCnC;
  }


  protected Collection<String> featuresCpCnC(PaddedList<FeatureLabel> cInfo, int loc) {
    FeatureLabel c = cInfo.get(loc);

    Collection<String> featuresCpCnC = new ArrayList<String>();

    if (flags.useNext && flags.usePrev) {
      if (flags.useSequences && flags.usePrevSequences && flags.useNextSequences) {
        featuresCpCnC.add("PNSEQ");
        featuresCpCnC.add(c.word() + "-PNSEQW");
      }
    }

    return featuresCpCnC;
  }


  int reverse(int i) {
    return (flags.useReverse ? -1 * i : i);
  }

  private Collection<String> occurrencePatterns(PaddedList<FeatureLabel> cInfo, int loc) {
    // features on last Cap
    String word = cInfo.get(loc).word();
    String nWord = cInfo.get(loc + reverse(1)).word();
    FeatureLabel p = cInfo.get(loc - reverse(1));
    String pWord = p.word();
    // System.err.println(word+" "+nWord);
    if (!(isNameCase(word) && noUpperCase(nWord) && hasLetter(nWord) && hasLetter(pWord) && p != cInfo.getPad())) {
      return Collections.singletonList("NO-OCCURRENCE-PATTERN");
    }
    // System.err.println("LOOKING");
    Set<String> l = new HashSet<String>();
    if (isNameCase(pWord) && cInfo.get(loc - reverse(1)).get("tag").equals("NNP")) {
      for (int jump = 3; jump < 150; jump++) {
        if (cInfo.get(loc + reverse(jump)).word().equals(word)) {
          if (cInfo.get(loc + reverse(jump - 1)).word().equals(pWord)) {
            l.add("XY-NEXT-OCCURRENCE-XY");
          } else {
            l.add("XY-NEXT-OCCURRENCE-Y");
          }
        }
      }
      for (int jump = -3; jump > -150; jump--) {
        if (cInfo.get(loc + reverse(jump)).word().equals(word)) {
          if (cInfo.get(loc + reverse(jump - 1)).word().equals(pWord)) {
            l.add("XY-PREV-OCCURRENCE-XY");
          } else {
            l.add("XY-PREV-OCCURRENCE-Y");
          }
        }
      }
    } else {
      for (int jump = 3; jump < 150; jump++) {
        if (cInfo.get(loc + reverse(jump)).word().equals(word)) {
          if (isNameCase(cInfo.get(loc + reverse(jump - 1)).word()) && (cInfo.get(loc + reverse(jump - 1))).get("tag").equals("NNP")) {
            l.add("X-NEXT-OCCURRENCE-YX");
            // System.err.println(cInfo.get(loc+reverse(jump-1)).word());
          } else if (isNameCase((cInfo.get(loc + reverse(jump + 1))).word()) && (cInfo.get(loc + reverse(jump + 1))).get("tag").equals("NNP")) {
            // System.err.println(cInfo.get(loc+reverse(jump+1)).word());
            l.add("X-NEXT-OCCURRENCE-XY");
          } else {
            l.add("X-NEXT-OCCURRENCE-X");
          }
        }
      }
      for (int jump = -3; jump > -150; jump--) {
        if (cInfo.get(loc + jump).word().equals(word)) {
          if (isNameCase(cInfo.get(loc + reverse(jump + 1)).word()) && (cInfo.get(loc + reverse(jump + 1))).get("tag").equals("NNP")) {
            l.add("X-PREV-OCCURRENCE-YX");
            // System.err.println(cInfo.get(loc+reverse(jump+1)).word());
          } else if (isNameCase(cInfo.get(loc + reverse(jump - 1)).word()) && cInfo.get(loc + reverse(jump - 1)).get("tag").equals("NNP")) {
            l.add("X-PREV-OCCURRENCE-XY");
            // System.err.println(cInfo.get(loc+reverse(jump-1)).word());
          } else {
            l.add("X-PREV-OCCURRENCE-X");
          }
        }
      }
    }
    /*
    if (!l.isEmpty()) {
      System.err.println(pWord+" "+word+" "+nWord+" "+l);
    }
    */
    return l;
  }

  String intern(String s) {
    if (flags.intern) {
      return s.intern();
    } else {
      return s;
    }
  }

  public void initGazette() {
    try {
      // read in gazettes
      if (flags.gazettes == null) { flags.gazettes = new ArrayList<String>(); }
      List<String> gazettes = flags.gazettes;
      for (String gazetteFile : gazettes) {
        BufferedReader r = new BufferedReader(new FileReader(gazetteFile));
        readGazette(r);
        r.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

} // end class NERFeatureFactory
