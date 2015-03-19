package de.unihd.dbs.uima.annotator.stanfordtagger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;


import org.archive.util.tuple.Pair;

import de.unihd.dbs.uima.types.heideltime.Sentence;
import de.unihd.dbs.uima.types.heideltime.Token;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.util.CoreMap;

public class StanfordCoreNLPWrapper extends JCasAnnotator_ImplBase {
	
	private Class<?> component = this.getClass();
	//used for splitting a document into sentences
	private static final String SPLITTER = "[.]|[!?]+|[。]|[！？]+";
	//find the splitting points and keep the offset
	private static Pattern sPattern = Pattern.compile(SPLITTER);
	
	// definitions of what names these parameters have in the wrapper's descriptor file
	public static final String PARAM_MODEL_PATH = "model_path";
	public static final String PARAM_CONFIG_PATH = "config_path";
	public static final String PARAM_ANNOTATE_TOKENS = "annotate_tokens";
	public static final String PARAM_ANNOTATE_SENTENCES = "annotate_sentences";
	public static final String PARAM_ANNOTATE_PARTOFSPEECH = "annotate_partofspeech";
	
	// switches for annotation parameters
	private String model_path;
	private String config_path;
	private Boolean annotate_tokens = true;
	private Boolean annotate_sentences = true;
	private Boolean annotate_partofspeech = true;
	private Boolean annotate_ners = true;
	
	//use pipeline or per calling
	private boolean StanfordCoreNLP_PIP_USE = false;
	
	/**
	 * StanfordCoreNLP 
	 * not used due to inconsistent results of pos and ner, i.e., the untokenizable tokens are not deleted consistently
	 * **/
	public static Properties pipeStProps;
	public static StanfordCoreNLP pipeStPipeline;
	
	/**
	 * usage of Stanford Parser by calling necessary components, and self-splitting
	 * **/
	private CRFClassifier stSegmenter;
	// Maximum Entropy Tagger from the Stanford POS Tagger
	private MaxentTagger stPosTagger;
	// CRF classifier for NER tagging
	private CRFClassifier stNerTagger;
	
	/**
	 * Tags of considered named entity
	 * ? Location, Person, Organization, Misc
	 * **/
	//PER, PERSON
	public static final String NER_PER = "PERSON";
	//GPE (geo-political entities), LOC
	public static final String NER_GPE = "GPE";
	//
	public static final String NER_LOC = "LOCATION";
	//
	public static final String NER_ORG = "ORGANIZATION";
	//Miscellaneous names include date, time, percentage and monetary expressions
	public static final String NER_MISC = "MISC";
		
	/**
	 * initialization method where we fill configuration values and check some prerequisites
	 */	
	
	public void initialize(UimaContext aContext) {
		
		if(StanfordCoreNLP_PIP_USE){
			initialize_Pip();
		}else{
			initialize_perComponent();
		}
	}
	
	public void initialize() {
		
		if(StanfordCoreNLP_PIP_USE){
			initialize_Pip();
		}else{
			initialize_perComponent();
		}
	}
	
	public void initialize_Pip() {

		pipeStProps = loadProperties("conf/StanfordCoreNLP-chinese.properties");
		pipeStPipeline = new StanfordCoreNLP(pipeStProps);
		System.out.println("Finished corenlp setting!");
		
	}
	
	public void initialize_perComponent() {
		try {
			//1
			Properties segProps = new Properties();
			segProps.setProperty("segment", "edu.stanford.nlp.pipeline.ChineseSegmenterAnnotator");
			segProps.setProperty("model", "edu/stanford/nlp/models/segmenter/chinese/ctb.gz");
	        // Lines below are needed because CTBSegDocumentIteratorFactory accesses it
			segProps.setProperty("sighanCorporaDict", "edu/stanford/nlp/models/segmenter/chinese");
			segProps.setProperty("serDictionary", "edu/stanford/nlp/models/segmenter/chinese/dict-chris6.ser.gz");
	        //props.setProperty("testFile", sampleData);
			segProps.setProperty("inputEncoding", "UTF-8");
			segProps.setProperty("sighanPostProcessing", "true");
			segProps.setProperty("untokenizable", "allKeep");
	        
	        stSegmenter = new CRFClassifier(segProps);
	        stSegmenter.loadClassifierNoExceptions("edu/stanford/nlp/models/segmenter/chinese/ctb.gz", segProps);
	        
	        //2
	        stPosTagger = new MaxentTagger("edu/stanford/nlp/models/pos-tagger/chinese-distsim/chinese-distsim.tagger");

	        //3
	        stNerTagger = CRFClassifier.getClassifier("edu/stanford/nlp/models/ner/chinese.misc.distsim.crf.ser.gz");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Finished perComponent setting!");
		
	}
	
	private static Properties loadProperties(String name){
	    
	    Properties proTarget = null;

	    // Returns null on lookup failures
	    System.err.println("Searching for property file: " + name);
	    
	    InputStream in = null;
	    try {
	      in = new FileInputStream(name);
	      if (in != null) {
	        InputStreamReader reader = new InputStreamReader(in, "utf-8");
	        proTarget = new Properties ();
	        proTarget.load(reader); // Can throw IOException
	      }
	    } catch (IOException e) {
	    	System.err.println("loading property file error!");
	    	e.printStackTrace();
	    	proTarget = null;
	    } finally {
	      IOUtils.closeIgnoringExceptions(in);
	    }

	    return proTarget;
	  }
	
	/**
	 * Method that gets called to process the documents' cas objects
	 */
	public void process_Pip(JCas jcas) {
		// grab the document text
		String oriDoc = jcas.getDocumentText();
		// create an empty Annotation just with the given text
		Annotation stDoc = new Annotation(oriDoc);
		// run all Annotators on this text
		pipeStPipeline.annotate(stDoc);
		
		//keep it let's see what is needed (i.e., the offsets) when output!
		List<CoreMap> taggedSenList = stDoc.get(CoreAnnotations.SentencesAnnotation.class);
		
		Integer docOffset = 0; // a cursor of sorts to keep up with the position in the document text
		
		for(CoreMap taggedSen: taggedSenList) {
			
			// create a sentence object. gets added to index or discarded depending on configuration
			Sentence uimaSentence = new Sentence(jcas);
			uimaSentence.setBegin(docOffset);
			
			Integer wordCount = 0;
			CoreLabel lastTaggedToken = null;
			
			String preNerTag = "O";
			int entityBegin = 0;
			int entityEnd = 0;
			
			// traversing the words in the current sentence
			for (CoreLabel taggedToken: taggedSen.get(CoreAnnotations.TokensAnnotation.class)) {
				wordCount++;
				lastTaggedToken = taggedToken;
				
				Token uimaToken = new Token(jcas);
				uimaToken.setBegin(taggedToken.beginPosition());
				uimaToken.setEnd(taggedToken.endPosition());
				
				if(annotate_partofspeech){
					uimaToken.setPos(taggedToken.get(CoreAnnotations.PartOfSpeechAnnotation.class));
				}
				
				if(annotate_tokens){
					uimaToken.addToIndexes();
				}	
				
				if(annotate_ners){
					// this is the NER label of the token
					String curNerTag = taggedToken.get(CoreAnnotations.NamedEntityTagAnnotation.class);
					//--				
					if (!curNerTag.equals(preNerTag)) {
						if (!preNerTag.equals("O") && !curNerTag.equals("O")) {
							// an entity directly next to an other entity
							// push out finished entity
							createAnnotation(jcas,preNerTag,entityBegin,entityEnd);
							
							// begin new entity
							entityBegin = (Integer) taggedToken.beginPosition();
							entityEnd = (Integer) taggedToken.endPosition();
						} else if (!preNerTag.equals("O")) {
							// change from entity to "other": entity finished
							// push out finished entity
							createAnnotation(jcas,preNerTag,entityBegin,entityEnd);
						} else if (!curNerTag.equals("O")) {
							// change from "other" to an entity: beginning of new entity
							entityBegin = (Integer) taggedToken.beginPosition();
							entityEnd = (Integer) taggedToken.endPosition();						
						}
					} else if (!curNerTag.equals("O")) {
						// continuing entity: advance end position
						entityEnd = (Integer) taggedToken.endPosition();
					}
					
					preNerTag = curNerTag;
					//--
				}				
			}
			
			if(annotate_sentences){
				if(0 == wordCount){
					uimaSentence.setEnd(docOffset);
				}else{
					uimaSentence.setEnd(lastTaggedToken.endPosition());
				}
				
				uimaSentence.addToIndexes();
			}
			
			docOffset += lastTaggedToken.endPosition();
		}
	}
		
	private ArrayList<Pair<String, String>> toNerTokenList(List<List<CoreLabel>> nerTokenList){
		ArrayList<Pair<String, String>> nerWordTagList = new ArrayList<Pair<String,String>>();
				
		for (List<CoreLabel> sentence : nerTokenList) {
            for (CoreLabel word : sentence) {
            	//word - tag
            	nerWordTagList.add(new Pair<String, String>(word.word(), word.get(CoreAnnotations.AnswerAnnotation.class)));
            }
        }
		
		return nerWordTagList;
	}
	//
	/**
	 * keep the same number of tokens by adding "O" to ner tagged list
	 * **/
	private static int tagWindow = 5;
	private ArrayList<String> toNerTagList(List<String> segList, ArrayList<Pair<String, String>> nerWordTagList){
		
		ArrayList<String> nerTagList = new ArrayList<String>();
		
		for(int i=0; i<segList.size(); i++){
			
			if(i<nerWordTagList.size() && segList.get(i).equals(nerWordTagList.get(i).first)){
				nerTagList.add(nerWordTagList.get(i).second);
				continue;
			}
			
			boolean match = false;
			for(int win=1; win<=tagWindow; win++){
				int befPoint = i-win;
				if(0<=befPoint && befPoint<nerWordTagList.size() && segList.get(i).equals(nerWordTagList.get(befPoint).first)){
					nerTagList.add(nerWordTagList.get(befPoint).second);
					match = true;
					break;
				}
				
				int aftPoint = i+win;
				if(0<=aftPoint && aftPoint<nerWordTagList.size() && segList.get(i).equals(nerWordTagList.get(aftPoint).first)){
					nerTagList.add(nerWordTagList.get(aftPoint).second);
					match = true;
					break;
				}
			}
			
			if(!match){
				nerTagList.add("O");
			}
		}		
		
		return nerTagList;
	}

	//public static HashSet<String> nerTagsHashSet = new HashSet<String>();
	
	/**
	 * splitting the document, tagging segment, pos, ner respectively per sentence!
	 * **/
	public void process_perComponent(JCas jcas) {
		// grab the document text
		String oriDoc = jcas.getDocumentText();
		
		Matcher mat = sPattern.matcher(oriDoc);
		
		int senBegin = 0;
		while(mat.find()) {
	
			//String sp = mat.group();
			//int spBegin = mat.start();
			
			//end-offset of a sentence
			int spEnd = mat.end();
			
			//current raw sentence
			String newSentence = oriDoc.substring(senBegin, spEnd);
			
			//sentence-leveling tagging
			if(annotate_sentences){
				// create a sentence object. gets added to index or discarded depending on configuration
				Sentence uimaSentence = new Sentence(jcas);
				uimaSentence.setBegin(senBegin);
				uimaSentence.setEnd(spEnd);
				
				uimaSentence.addToIndexes();
			}			
			
			//processing each sentence by calling each component
			List<String> segList = null;
			String [] posToken = null;
			ArrayList<String> nerTagList = null;
			
			try {
				//perform segmentation
				segList = stSegmenter.segmentString(newSentence);				
		        StringBuffer segBuffer = new StringBuffer();
		        for(int i=0; i<segList.size(); i++){
		        	String w = (String)segList.get(i);
		        	segBuffer.append(w+" ");
		        }		    
		        //token string separated by blank
		        String segmentedSen = segBuffer.toString().trim();
		        
		        //perform pos tagging given segmented sentence
		        String posStr = stPosTagger.tagTokenizedString(new String(segmentedSen.getBytes(), "UTF-8"));
		        posToken = posStr.split(" ");
		        
		        //perform ner tagging
		        List<List<CoreLabel>> nerResult = stNerTagger.classify(segmentedSen);
		        nerTagList = toNerTagList(segList, toNerTokenList(nerResult));
		        
			} catch (Exception e) {
				// TODO: handle exception
				System.err.println("Bug sentence!");
				/*				
				System.out.println("sen:\t"+newSentence);
				System.out.println("---");
				System.out.println("segList:\t"+segList.size()+":"+segList);
				e.printStackTrace();
				System.out.println("---");
				*/
				senBegin = spEnd;
				continue;
			}
			
	        //check the in consistence
	        if(posToken.length >0 && segList.size() == posToken.length && posToken.length == nerTagList.size()){
	        	
				String preNerTag = "O";				
				String lastToken = null;
				
				int offsetInSec = 0;
				int entityBegin = senBegin;
				int entityEnd = senBegin;
				
				// traversing the words in the current sentence				
				for (int i=0; i<posToken.length; i++) {
					
					String curNerTag = nerTagList.get(i);
					lastToken = segList.get(i);					
					
					int tokenBegin = senBegin+offsetInSec;
					int tokenEnd = tokenBegin+lastToken.length();
					offsetInSec += lastToken.length();
										
					Token uimaToken = new Token(jcas);
					uimaToken.setBegin(tokenBegin);
					uimaToken.setEnd(tokenEnd);
					
					if(annotate_partofspeech){
						uimaToken.setPos(posToken[i].substring(posToken[i].lastIndexOf("#")+1));
					}
					
					if(annotate_tokens){
						uimaToken.addToIndexes();
					}	
					
					if(annotate_ners){
						
						/*
						if(!nerTagsHashSet.contains(curNerTag)){
							nerTagsHashSet.add(curNerTag);
						}
						*/
						
						// this is the NER label of the token										
						if (!curNerTag.equals(preNerTag)) {
							if (!preNerTag.equals("O") && !curNerTag.equals("O")) {
								// an entity directly next to an other entity
								// push out finished entity
								createAnnotation(jcas,preNerTag,entityBegin,entityEnd);
								
								// begin new entity
								entityBegin = tokenBegin;
								entityEnd = tokenEnd;
								
							} else if (!preNerTag.equals("O")) {
								// change from entity to "other": entity finished
								// push out finished entity
								createAnnotation(jcas,preNerTag,entityBegin,entityEnd);
							} else if (!curNerTag.equals("O")) {
								// change from "other" to an entity: beginning of new entity
								//entityBegin = (Integer) nerTaggedToken.beginPosition();
								entityBegin = tokenBegin;
								//entityEnd = (Integer) nerTaggedToken.endPosition();
								entityEnd = tokenEnd;
							}
						} else if (!curNerTag.equals("O")) {
							// continuing entity: advance end position
							entityEnd = tokenEnd;
						}
						
						preNerTag = curNerTag;
						
					}							
				}			
				
	        }else {
	        	
				System.err.println("unequal size error!");
				// >0 && segList.size() == posToken.length && posToken.length == nerWordList.size()
				/*
				System.out.println("posToken:\t"+posToken.length);				
				System.out.println("segList:\t"+segList.size());
				System.out.println(segList);
				System.out.println("nerTagList:\t"+nerTagList.size());
				System.out.println(nerTagList);
				*/
				
			}
	        
			senBegin = spEnd;
						
        }
		
		//last sentence
		if(senBegin < oriDoc.length()){
			
			//System.out.println("last sentence!");
			
			int spEnd = oriDoc.length();
			String lastSentence = oriDoc.substring(senBegin, spEnd);
			
			//sentence-leveling tagging
			if(annotate_sentences){
				// create a sentence object. gets added to index or discarded depending on configuration
				Sentence uimaSentence = new Sentence(jcas);
				uimaSentence.setBegin(senBegin);
				uimaSentence.setEnd(spEnd);
				
				uimaSentence.addToIndexes();
			}			
			
			//processing each sentence by calling each component
			List<String> segList = null;
			String [] posToken = null;
			ArrayList<String> nerTagList = null;
			
			try {
				//perform segmentation
				segList = stSegmenter.segmentString(lastSentence);				
		        StringBuffer segBuffer = new StringBuffer();
		        for(int i=0; i<segList.size(); i++){
		        	String w = (String)segList.get(i);
		        	segBuffer.append(w+" ");
		        }		    
		        //token string separated by blank
		        String segmentedSen = segBuffer.toString().trim();
		        
		        //perform pos tagging given segmented sentence
		        String posStr = stPosTagger.tagTokenizedString(new String(segmentedSen.getBytes(), "UTF-8"));
		        posToken = posStr.split(" ");
		        
		        //perform ner tagging
		        List<List<CoreLabel>> nerResult = stNerTagger.classify(segmentedSen);
		        nerTagList = toNerTagList(segList, toNerTokenList(nerResult));
		        
			} catch (Exception e) {
				// TODO: handle exception
				System.err.println("Bug sentence!");
				/*
				System.out.println("sen:\t"+lastSentence);
				System.out.println("---");
				System.out.println("segList:\t"+segList.size()+":"+segList);
				e.printStackTrace();
				System.out.println("---");
				*/
				senBegin = spEnd;
			}
			
	        //check the in consistence
	        if(posToken.length >0 && segList.size() == posToken.length && posToken.length == nerTagList.size()){
	        	
				String preNerTag = "O";				
				String lastToken = null;
				
				int offsetInSec = 0;
				int entityBegin = senBegin;
				int entityEnd = senBegin;
				
				// traversing the words in the current sentence				
				for (int i=0; i<posToken.length; i++) {
					
					String curNerTag = nerTagList.get(i);
					lastToken = segList.get(i);					
					
					int tokenBegin = senBegin+offsetInSec;
					int tokenEnd = tokenBegin+lastToken.length();
					offsetInSec += lastToken.length();
										
					Token uimaToken = new Token(jcas);
					uimaToken.setBegin(tokenBegin);
					uimaToken.setEnd(tokenEnd);
					
					if(annotate_partofspeech){
						uimaToken.setPos(posToken[i].substring(posToken[i].lastIndexOf("#")+1));
					}
					
					if(annotate_tokens){
						uimaToken.addToIndexes();
					}	
					
					if(annotate_ners){
						// this is the NER label of the token										
						if (!curNerTag.equals(preNerTag)) {
							if (!preNerTag.equals("O") && !curNerTag.equals("O")) {
								// an entity directly next to an other entity
								// push out finished entity
								createAnnotation(jcas,preNerTag,entityBegin,entityEnd);
								
								// begin new entity
								entityBegin = tokenBegin;
								entityEnd = tokenEnd;
								
							} else if (!preNerTag.equals("O")) {
								// change from entity to "other": entity finished
								// push out finished entity
								createAnnotation(jcas,preNerTag,entityBegin,entityEnd);
							} else if (!curNerTag.equals("O")) {
								// change from "other" to an entity: beginning of new entity
								//entityBegin = (Integer) nerTaggedToken.beginPosition();
								entityBegin = tokenBegin;
								//entityEnd = (Integer) nerTaggedToken.endPosition();
								entityEnd = tokenEnd;
							}
						} else if (!curNerTag.equals("O")) {
							// continuing entity: advance end position
							entityEnd = tokenEnd;
						}
						
						preNerTag = curNerTag;
						
					}							
				}			
				
	        }else {
	        	
				System.err.println("unequal size error!");
				// >0 && segList.size() == posToken.length && posToken.length == nerWordList.size()
				/*
				System.out.println("posToken:\t"+posToken.length);				
				System.out.println("segList:\t"+segList.size());
				System.out.println(segList);
				System.out.println("nerTagList:\t"+nerTagList.size());
				System.out.println(nerTagList);
				*/
				
			}
		}

	}
	
	public void process(JCas jcas) throws AnalysisEngineProcessException{
		if(StanfordCoreNLP_PIP_USE){
			process_Pip(jcas);
		} else{
			process_perComponent(jcas);
		}
	}
	
	/**
	 * should be adjusted according to requirement of ner type
	 * **/
	protected void createAnnotation(JCas jcas, String tag, int entityBegin, int entityEnd) {

		de.florianlaws.uima.types.Annotation nerAnnotation;
	
		if (tag.equals("PERSON") || tag.equals("PER")) {
			
			nerAnnotation = new de.florianlaws.uima.types.stanford.Person(jcas,entityBegin, entityEnd);
			nerAnnotation.setComponentId(NER_PER);			
			nerAnnotation.addToIndexes();
			
		} else if (tag.equals("ORG")) {
			
			nerAnnotation = new de.florianlaws.uima.types.stanford.Organization(jcas,entityBegin, entityEnd);
			nerAnnotation.setComponentId(NER_ORG);			
			nerAnnotation.addToIndexes();
			
		} else if (tag.equals("LOC")) {
			
			nerAnnotation = new de.florianlaws.uima.types.stanford.Location(jcas,entityBegin, entityEnd);
			nerAnnotation.setComponentId(NER_LOC);
			nerAnnotation.addToIndexes();
			
		} else if(tag.equals("GPE")){
			nerAnnotation = new de.florianlaws.uima.types.stanford.GPE(jcas,entityBegin, entityEnd);
			nerAnnotation.setComponentId(NER_GPE);
			nerAnnotation.addToIndexes();
			
		} else if (tag.equals("MISC")) {
			
			nerAnnotation = new de.florianlaws.uima.types.stanford.Misc(jcas,entityBegin, entityEnd);
			nerAnnotation.setComponentId(NER_MISC);
			nerAnnotation.addToIndexes();
			
		}		
	}

}
