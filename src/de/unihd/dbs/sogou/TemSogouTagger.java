package de.unihd.dbs.sogou;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.apache.uima.UIMAFramework;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.XMLInputSource;

import de.unihd.dbs.heideltime.standalone.Config;
import de.unihd.dbs.heideltime.standalone.DocumentType;
import de.unihd.dbs.heideltime.standalone.components.JCasFactory;
import de.unihd.dbs.heideltime.standalone.components.impl.JCasFactoryImpl;
import de.unihd.dbs.heideltime.standalone.components.impl.UimaContextImpl;
import de.unihd.dbs.heideltime.standalone.exceptions.DocumentCreationTimeMissingException;
import de.unihd.dbs.uima.annotator.heideltime.HeidelTime;
import de.unihd.dbs.uima.annotator.heideltime.resources.Language;
import de.unihd.dbs.uima.annotator.stanfordtagger.StanfordCoreNLPWrapper;
import de.unihd.dbs.uima.consumer.sogou.SogouTCollectionWriter;
import de.unihd.dbs.uima.reader.sogou.SogouTCollectionReader;
import de.unihd.dbs.uima.types.heideltime.Dct;

public class TemSogouTagger {
	
	private Properties props;
	
	/**
	 * HeidelTime instance
	 */
	private HeidelTime heidelTime;
	/**
	 * Type system description
	 */
	private JCasFactory jcasFactory;
	
	private StanfordCoreNLPWrapper stanfordCoreNLPWrapper;
	//
	//private StanfordPOSTaggerWrapper stanfordPOSTaggerWrapper;
	
	/**
	 * Used document type
	 */
	private DocumentType documentType;
	
	SogouTCollectionReader sogouReader;
	SogouTCollectionWriter sogouWriter;
	
	
	public TemSogouTagger(String inDir, String outDir){
		
		readConfigFile("conf/config.props");
		
		//1
		try {
			
			heidelTime = new HeidelTime();
			heidelTime.initialize(new UimaContextImpl(Language.CHINESE, DocumentType.NEWS));
			
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("HeidelTime could not be initialized !");			
		}
		//2
		// Initialize JCas factory -------------
		try {
			TypeSystemDescription[] descriptions = new TypeSystemDescription[] {
					UIMAFramework
							.getXMLParser()
							.parseTypeSystemDescription(
									new XMLInputSource(new File(Config.get(Config.TYPESYSTEMHOME))
											)),
					UIMAFramework
							.getXMLParser()
							.parseTypeSystemDescription(
									new XMLInputSource(new File(Config.get(Config.TYPESYSTEMHOME_DKPRO)))),
					UIMAFramework
					.getXMLParser()
					.parseTypeSystemDescription(
							new XMLInputSource(new File("desc/type/NERDATypes.xml")))};
			
			jcasFactory = new JCasFactoryImpl(descriptions);
			
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("JCas factory could not be initialized !");
		}
		//3
		try {
			stanfordCoreNLPWrapper = new StanfordCoreNLPWrapper();
			stanfordCoreNLPWrapper.initialize();
			
			//
			/*
			stanfordPOSTaggerWrapper = new StanfordPOSTaggerWrapper();
			props.put(PartOfSpeechTagger.STANFORDPOSTAGGER_ANNOTATE_TOKENS, true);
			props.put(PartOfSpeechTagger.STANFORDPOSTAGGER_ANNOTATE_SENTENCES, true);
			props.put(PartOfSpeechTagger.STANFORDPOSTAGGER_ANNOTATE_POS, true);
			props.put(PartOfSpeechTagger.STANFORDPOSTAGGER_MODEL_PATH, Config.get(Config.STANFORDPOSTAGGER_MODEL_PATH));
			props.put(PartOfSpeechTagger.STANFORDPOSTAGGER_CONFIG_PATH, Config.get(Config.STANFORDPOSTAGGER_CONFIG_PATH));
			stanfordPOSTaggerWrapper.initialize(props);
			*/
			
			
			sogouReader = new SogouTCollectionReader();
			sogouReader.initialize(inDir);
			
			sogouWriter = new SogouTCollectionWriter();
			sogouWriter.initialize(outDir);
		} catch (Exception e) {
			System.err.println("IO error!");
		}		
	}
	//
	public void readConfigFile(String configPath) {
		InputStream configStream = null;
		try {
			//logger.log(Level.INFO, "trying to read in file "+configPath);
			configStream = new FileInputStream(configPath);
			
			props = new Properties();
			props.load(configStream);

			Config.setProps(props);
			
			configStream.close();
		} catch (FileNotFoundException e) {
			//logger.log(Level.WARNING, "couldn't open configuration file \""+configPath+"\". quitting.");
			System.exit(-1);
		} catch (IOException e) {
			//logger.log(Level.WARNING, "couldn't close config file handle");
			e.printStackTrace();
		}
	}
	//
	public void run(){
		//int i=1;
		try {
			while(sogouReader.hasNext()){
				
				JCas jcas = sogouReader.getNext(jcasFactory);
				
				process(jcas);
				//System.out.println((i++));
				sogouWriter.printDocuments(jcas);
			}
			
			sogouWriter.collectionProcessComplete(null);
			
			//System.out.println(stanfordCoreNLPWrapper.nerTagsHashSet);
			
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Running error!");
		}
		
	}

	//
	private void process(JCas jcas){
		try {
			
			stanfordCoreNLPWrapper.process(jcas);
			
			//stanfordPOSTaggerWrapper.process(jcas);
			
			heidelTime.process(jcas);
			
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Process ereor!");
		}		
	}
	
	/**
	 * Provides jcas object with document creation time if
	 * <code>documentCreationTime</code> is not null.
	 * 
	 * @param jcas
	 * @param documentCreationTime
	 * @throws DocumentCreationTimeMissingException
	 *             If document creation time is missing when processing a
	 *             document of type {@link DocumentType#NEWS}.
	 */
	private void provideDocumentCreationTime(JCas jcas, Date documentCreationTime)
			throws DocumentCreationTimeMissingException {
		
		if (documentCreationTime == null) {
			// Document creation time is missing
			if (documentType == DocumentType.NEWS) {
				// But should be provided in case of news-document
				throw new DocumentCreationTimeMissingException();
			}
			if (documentType == DocumentType.COLLOQUIAL) {
				// But should be provided in case of colloquial-document
				throw new DocumentCreationTimeMissingException();
			}
		} else {
			// Document creation time provided
			// Translate it to expected string format
			SimpleDateFormat dateFormatter = new SimpleDateFormat(
					"yyyy.MM.dd'T'HH:mm");
			String formattedDCT = dateFormatter.format(documentCreationTime);

			// Create dct object for jcas
			Dct dct = new Dct(jcas);
			dct.setValue(formattedDCT);

			dct.addToIndexes();
		}
	}
	
	
	//
	public static void main(String []args){
		//1
		String inDir = "/Users/dryuhaitao/WorkBench/JavaBench/HeidelTimeKit/collectionTest/input";
		String outDir = "/Users/dryuhaitao/WorkBench/JavaBench/HeidelTimeKit/collectionTest/output";
		TemSogouTagger temSogouTagger = new TemSogouTagger(inDir, outDir);
		temSogouTagger.run();
		
	}
}
