package de.florianlaws.uima;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Properties;

import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.annotator.AnnotatorConfigurationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorContext;
import org.apache.uima.analysis_engine.annotator.AnnotatorContextException;
import org.apache.uima.analysis_engine.annotator.AnnotatorInitializationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorProcessException;
import org.apache.uima.analysis_engine.annotator.JTextAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;


import alteredu.stanford.nlp.ie.crf.CRFClassifier;
import alteredu.stanford.nlp.ling.FeatureLabel;

public class StanfordNERAnnotator extends JTextAnnotator_ImplBase {
	
	public static final String CLASSIFIER_FILE_PARAM = "ClassifierFile";
	
	public static final String COMPONENT_ID = "Stanford NER Detector";
	
	private CRFClassifier crf;
	
	public void initialize(AnnotatorContext context) 
		throws AnnotatorInitializationException, AnnotatorConfigurationException { 
		super.initialize(context);
		reconfigure();
	}
	
	public void reconfigure() throws AnnotatorConfigurationException, AnnotatorInitializationException {
		String classifierFile;
		try {
			classifierFile = (String) getContext().getConfigParameterValue(CLASSIFIER_FILE_PARAM);
			
		} catch (AnnotatorContextException e) {
			throw new AnnotatorConfigurationException(e);
		}
		
		try {
			Properties props = new Properties();
			props.setProperty("loadClassifier",classifierFile);
			crf = new CRFClassifier(props);
			crf.loadClassifier(crf.flags.loadClassifier);
		} catch (ClassCastException e) {
			throw new AnnotatorConfigurationException(e);			
		} catch (IOException e) {
			throw new AnnotatorConfigurationException(e);
		} catch (ClassNotFoundException e) {
			throw new AnnotatorConfigurationException(e);			
		}
	}
	
	

	public void process(JCas jcas, ResultSpecification aResultSpec)
		throws AnnotatorProcessException {
		
		System.err.println("getting doc text...");
		String docText = jcas.getDocumentText();
		
		System.err.println("analyzing...");
		List<List<FeatureLabel>> documents = 
			crf.testReader(new StringReader(docText));
		
		System.err.println("collecting annotations...");
		for (List<FeatureLabel> doc : documents) {
			String prevTag = "O";
			int entityBegin = 0;
			int entityEnd = 0;
			for  (FeatureLabel wi : doc) {
				
				
				
				String tag = wi.answer();
				if (!tag.equals(prevTag)) {
					if (!prevTag.equals("O") && !tag.equals("O")) {
						// an entity directly next to an other entity
						// push out finished entity
						createAnnotation(jcas,prevTag,entityBegin,entityEnd);
						
						// begin new entity
						entityBegin = (Integer) wi.get("startPosition");
						entityEnd = (Integer) wi.get("endPosition");
					} else if (!prevTag.equals("O")) {
						// change from entity to "other": entity finished
						// push out finished entity
						createAnnotation(jcas,prevTag,entityBegin,entityEnd);
					} else if (!tag.equals("O")) {
						// change from "other" to an entity: beginning of new entity
						entityBegin = (Integer) wi.get("startPosition");
						entityEnd = (Integer) wi.get("endPosition");						
					}
				} else if (!tag.equals("O")) {
					// continuing entity: advance end position
					entityEnd = (Integer) wi.get("endPosition");
				}
				prevTag = tag;
			}
		}
		System.err.println("StanfordNERAnnotator done.");
		
	}

	protected void createAnnotation(JCas jcas, String tag, 
			int entityBegin, int entityEnd) {

		de.florianlaws.uima.types.Annotation entity;
		
	
		if (tag.equals("PERSON")) {
			entity = new de.florianlaws.uima.types.stanford.Person(jcas,entityBegin, entityEnd);
			entity.setComponentId(COMPONENT_ID);
		} else if (tag.equals("ORGANIZATION")) {
			entity = new de.florianlaws.uima.types.stanford.Organization(jcas,entityBegin, entityEnd);
			entity.setComponentId(COMPONENT_ID);
		} else if (tag.equals("LOCATION")) {
			entity = new de.florianlaws.uima.types.stanford.Location(jcas,entityBegin, entityEnd);
			entity.setComponentId(COMPONENT_ID);
		} else if (tag.equals("MISC")) {
			entity = new de.florianlaws.uima.types.stanford.Misc(jcas,entityBegin, entityEnd);
			entity.setComponentId(COMPONENT_ID);
		} else {
			entity = new de.florianlaws.uima.types.Annotation(jcas,entityBegin, entityEnd);
			entity.setComponentId(COMPONENT_ID);
		}
		
		System.out.printf("%s %d..%d %s",tag,entityBegin,entityEnd,
				entity.getCoveredText());
		
		
		entity.addToIndexes();
	}

}

