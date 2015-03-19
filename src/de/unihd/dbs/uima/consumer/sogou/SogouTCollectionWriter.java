package de.unihd.dbs.uima.consumer.sogou;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import de.unihd.dbs.heideltime.standalone.components.impl.NERTimeResultFormatter;
import de.unihd.dbs.uima.types.heideltime.SourceDocInfo;

public class SogouTCollectionWriter extends CasConsumer_ImplBase {
	
	public static final String PARAM_OUTPUTDIR = "OutputDir";

	private File outputDir;
	BufferedWriter bf;
 
	private int docCount;
	
	private static final int docPerFile = 5000;
	private static final DecimalFormat df = new DecimalFormat("00000000");
	
	/**
	 * initialize
	 */
	public void initialize() throws ResourceInitializationException {
    
		docCount = 0;
		outputDir = new File((String) getConfigParameterValue(PARAM_OUTPUTDIR));
		if (!outputDir.exists()) {
			outputDir.mkdirs();
		} 
		
	}
	
	public void initialize(String dir) throws ResourceInitializationException {
	    
		docCount = 0;
		outputDir = new File(dir);
		if (!outputDir.exists()) {
			outputDir.mkdirs();
		} 
		
	}

	@Override
	public void processCas(CAS aCAS) throws ResourceProcessException {
		// TODO Auto-generated method stub
		JCas jcas;
		
		try {
			jcas = aCAS.getJCas();
		} catch (CASException e) {
			throw new ResourceProcessException(e);
		}

		printDocuments(jcas);

	}
	
	public void printDocuments(JCas jcas){
		
		if(0 == docCount % docPerFile){			
			try {
				if(docCount > 0){
					bf.flush();
					bf.close();
					bf=null;
				}
				
				int k = docCount/docPerFile;
				String suffix = df.format(k);
				
				String outFileName = "SogouCA_TemTagged_"+suffix+".xml";
				
				File outFile = new File(outputDir, outFileName);
				bf = new BufferedWriter(new FileWriter(outFile));
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}
		
		try {			
			// retrieve the filename of the input file from the CAS
		    FSIterator it = jcas.getAnnotationIndex(SourceDocInfo.type).iterator();
		    
		    //meta-info
		    if (it.hasNext()) {
		      SourceDocInfo docInfo = (SourceDocInfo) it.next();
		      
		      	bf.write("<doc id="+docInfo.getId()+">");
				bf.newLine();
				bf.write("<meta-info>");
				bf.newLine();
				bf.write("<tag name=\"host\">"+docInfo.getHost()+"</tag>");
				bf.newLine();
				bf.write("<tag name=\"date\">"+docInfo.getDate()+"</tag>");
				bf.newLine();
				bf.write("<tag name=\"url\">"+docInfo.getUri()+"</tag>");
				bf.newLine();
				bf.write("<tag name=\"title\">"+docInfo.getTitle()+"</tag>");
				bf.newLine();
				bf.write("<tag name=\"source-encoding\">UTF-8</tag>");
				bf.newLine();
				bf.write("</meta-info>");
				bf.newLine();				
		    }
			
		    //tagged text		    
			bf.write(NERTimeResultFormatter.temFormat(jcas));			
			bf.write("</doc>");
			bf.newLine();			
		    
		    docCount++;
		    
		    //System.out.println("in:\t"+docCount);
		      
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	@Override
	public void collectionProcessComplete(ProcessTrace arg0) throws IOException {
		try {
			if(null != bf){
				bf.flush();
				bf.close();
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
}
