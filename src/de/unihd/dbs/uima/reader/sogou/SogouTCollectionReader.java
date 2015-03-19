package de.unihd.dbs.uima.reader.sogou;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.FileUtils;
import org.apache.uima.util.Progress;
import org.archive.util.io.IOText;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import de.unihd.dbs.heideltime.standalone.components.JCasFactory;
import de.unihd.dbs.uima.types.heideltime.Dct;
import de.unihd.dbs.uima.types.heideltime.SourceDocInfo;

public class SogouTCollectionReader extends CollectionReader_ImplBase {
	
	public static final String PARAM_INPUTDIR = "InputDirectory";
	
	/**
	 * List containing all filenames of "documents"
	 */
	private ArrayList<File> fileSetList;

	private List docListOfCurrentFile = null;
	//cursor w.r.t. the list of big-files
	private int fileCursorOfDirecory;
	//cursor w.r.t. the list of documents in a big-file
	private int docCursorOfCurrentFile;
	//total number of documents in a file
	private int docNumOfCurrentFile;
	
	private static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy.MM.dd'T'HH:mm");
	
	
	public void initialize() throws ResourceInitializationException {
		
		File directory = new File(((String) getConfigParameterValue(PARAM_INPUTDIR)).trim());
		fileCursorOfDirecory = 0;
		
		// if input directory does not exist or is not a directory, throw exception
		if (!directory.exists() || !directory.isDirectory()) {
			throw new ResourceInitializationException(ResourceConfigurationException.DIRECTORY_NOT_FOUND,
					new Object[] { PARAM_INPUTDIR, this.getMetaData().getName(), directory.getPath() });
		}

		// get list of big-files (without sub-directories) in the specified directory
		fileSetList = new ArrayList<File>();
		File[] files = directory.listFiles();
		for (int i = 0; i < files.length; i++) {
			if (!files[i].isDirectory()) {
				fileSetList.add(files[i]);
			}
		}
		
		//load the documents of the first file
		if(fileCursorOfDirecory < fileSetList.size()){
			docListOfCurrentFile = loadDocuments();
			docCursorOfCurrentFile = 0;
			docNumOfCurrentFile = docListOfCurrentFile.size();
		}		
	}
	
	public void initialize(String dir) throws ResourceInitializationException {
		
		File directory = new File(dir);
		fileCursorOfDirecory = 0;
		
		// if input directory does not exist or is not a directory, throw exception
		if (!directory.exists() || !directory.isDirectory()) {
			throw new ResourceInitializationException(ResourceConfigurationException.DIRECTORY_NOT_FOUND,
					new Object[] { PARAM_INPUTDIR, this.getMetaData().getName(), directory.getPath() });
		}

		// get list of big-files (without sub-directories) in the specified directory
		fileSetList = new ArrayList<File>();
		File[] files = directory.listFiles();
		for (int i = 0; i < files.length; i++) {
			if (!files[i].isDirectory()) {
				fileSetList.add(files[i]);
			}
		}
		
		//load the documents of the first file
		if(fileCursorOfDirecory < fileSetList.size()){
			docListOfCurrentFile = loadDocuments();
			docCursorOfCurrentFile = 0;
			docNumOfCurrentFile = docListOfCurrentFile.size();
		}		
	}

	@Override
	public void getNext(CAS aCAS) throws IOException, CollectionException {
		JCas jcas;
		try {
			jcas = aCAS.getJCas();
		} catch (CASException e) {
			throw new CollectionException(e);
		}
		
		Element docElement = (Element)docListOfCurrentFile.get(docCursorOfCurrentFile++);
		
		Element metaElement = docElement.getChild("meta-info");		
		String text = docElement.getChildText("text");
		//
		jcas.setDocumentText(text);
		
		//meta-info
		TreeMap<String, String> metaMap = new TreeMap<>();
		metaMap.put("id", docElement.getAttributeValue("id"));
		
		List tagList = metaElement.getChildren("tag");
		for(int j=0; j<tagList.size(); j++){
			Element tagElement = (Element)tagList.get(j);
			String tagName = tagElement.getAttributeValue("name");
			String tagText = tagElement.getText();
			
			metaMap.put(tagName, tagText);
		}
		
	    SourceDocInfo srcDocInfo = new SourceDocInfo(jcas);	    
		srcDocInfo.setId(metaMap.get("id"));
		srcDocInfo.setHost(metaMap.get("host"));
		srcDocInfo.setDate(metaMap.get("date"));
		srcDocInfo.setUri(metaMap.get("url"));
		srcDocInfo.setTitle(metaMap.get("title"));
	    srcDocInfo.addToIndexes();
	}
	
	public JCas getNext(JCasFactory jcasFactory) throws IOException, CollectionException {
		JCas jcas = null;
		try {
			jcas = jcasFactory.createJCas();
		} catch (Exception e) {
			System.err.println("createJCas() error!");
		}
		
		Element docElement = (Element)docListOfCurrentFile.get(docCursorOfCurrentFile++);
		
		Element metaElement = docElement.getChild("meta-info");		
		String text = docElement.getChildText("text");
		//
		jcas.setDocumentText(text);
		
		//meta-info
		TreeMap<String, String> metaMap = new TreeMap<>();
		metaMap.put("id", docElement.getAttributeValue("id"));
		
		List tagList = metaElement.getChildren("tag");
		for(int j=0; j<tagList.size(); j++){
			Element tagElement = (Element)tagList.get(j);
			String tagName = tagElement.getAttributeValue("name");
			String tagText = tagElement.getText();
			
			metaMap.put(tagName, tagText);
		}
		
	    SourceDocInfo srcDocInfo = new SourceDocInfo(jcas);	    
		srcDocInfo.setId(metaMap.get("id"));
		srcDocInfo.setHost(metaMap.get("host"));
		srcDocInfo.setDate(metaMap.get("date"));
		srcDocInfo.setUri(metaMap.get("url"));
		srcDocInfo.setTitle(metaMap.get("title"));
	    srcDocInfo.addToIndexes();
	    
	    //Document creation time provided & Translate it to expected string format
	    String [] dateArray = metaMap.get("date").split("-");
	    if(null!=dateArray && dateArray.length==3){
	    	try {
	    		Calendar c = Calendar.getInstance();
	    		c.set(Integer.parseInt(dateArray[0]), Integer.parseInt(dateArray[1])-1, Integer.parseInt(dateArray[2]), 0, 0); 
								
				//System.out.println(c.getTime());				
				String formattedDCT = dateFormatter.format(c.getTime());
				// Create dct object for jcas
				Dct dct = new Dct(jcas);
				dct.setValue(formattedDCT);
				dct.addToIndexes();
			} catch (Exception e) {
				// TODO: handle exception
				System.err.println("!!!Wrong publication date provided!");
			}
	    }		
	    
	    return jcas;
	}

	@Override
	public boolean hasNext() throws IOException, CollectionException {
		
		if(fileCursorOfDirecory < fileSetList.size()){			 
			
			if(docCursorOfCurrentFile == docNumOfCurrentFile){
				
				do {
					docListOfCurrentFile = loadDocuments();
					
					docCursorOfCurrentFile = 0;
					docNumOfCurrentFile = docListOfCurrentFile.size();
					
				}while((fileCursorOfDirecory<fileSetList.size()) && (docCursorOfCurrentFile==docNumOfCurrentFile));
			}
			
			if(docCursorOfCurrentFile < docNumOfCurrentFile){
				return true;
			}else{
				return false;
			}
			
		}else if(0 == fileCursorOfDirecory){
			return false;
		}else if(docCursorOfCurrentFile < docNumOfCurrentFile){
			return true;
		}else{
			return false;
		}
		
	}

	@Override
	public Progress[] getProgress() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub

	}
	
	private List loadDocuments(){
		// open input stream to file
	    File file = (File) fileSetList.get(fileCursorOfDirecory++);
		//logger.log(Level.INFO, "getNext(CAS) - Reading file " + file.getName());
	    
	    Pattern p = Pattern.compile("[^\\u0009\\u000A\\u000D\\u0020-\\uD7FF\uE000-\\uFFFD\\u10000-\\u10FFF]+"); 
	    
	    List docList = null;	    

	    try {
	    	//build a standard pseudo-xml file
		    StringBuffer buffer = new StringBuffer();
		    buffer.append("<add>");
		    
		    //String text = FileUtils.file2String(file);
		    System.out.println("Loading:\t"+file.getAbsolutePath());
		    ArrayList<String> lineList = IOText.getLinesAsAList(file.getAbsolutePath(), "utf-8");		    
		    
		    for(String line: lineList){
		    	
		    	/*
		    	if(line.indexOf("id") >= 0){
		    		int equalSign = line.indexOf("=");
		    		int bigSign = line.indexOf(">");
		    		line = line.substring(0, equalSign+1)+"\""+line.substring(equalSign+1, bigSign)+"\""+">";
		    	}
		    	*/
		    	
		    	///*
		    	line = p.matcher(line).replaceAll("");		    
			    if(line.indexOf("&") >= 0){
			    	line = line.replaceAll("&", "&amp;");
		    	}
			    //*/
		    	buffer.append(line);
		    }		    
		    
		    buffer.append("</add>"); 
	    	
	    	SAXBuilder saxBuilder = new SAXBuilder();      
	        Document xmlDocSet = saxBuilder.build(new InputStreamReader(new ByteArrayInputStream(buffer.toString().getBytes("utf-8"))));
	        
	        Element rootElement = xmlDocSet.getRootElement(); 
	        docList = rootElement.getChildren("doc");
	        
	        return docList;
	        
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			docList = null;
		}
	    
	    return docList;
	}

}
