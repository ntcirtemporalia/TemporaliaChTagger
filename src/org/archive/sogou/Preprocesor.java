package org.archive.sogou;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.archive.util.io.IOText;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;


/**
 * Function:
 * 
 * Given the raw data of SogouCA, get the corresponding Temporalia-Styple (Non-Tagged) version below 
 * <doc id=***>
 * <meta-info>
 * 	<tag name="host">***</tag>
 *  <tag name="date">****-**-**</tag>
 *  <tag name="url">***</tag>
 *  <tag name="title">***</tag>
 *  <tag name="source-encoding">UTF-8</tag>
 * </meta-info>
 * <text>***</text>
 * </doc> 
 * 
 * **/


public class Preprocesor {
	
	//for filtering the noisy characters
	private static Pattern noisyCharacters = Pattern.compile("[^\\u0009\\u000A\\u000D\\u0020-\\uD7FF\uE000-\\uFFFD\\u10000-\\u10FFF]+"); 
	
	private static int totalDoc;
	private static int acceptedDoc;
	private static int refusedDoc;
	private static int zeroContentDoc;
	
	private static final int docPerFile = 5000;
	private static final DecimalFormat df = new DecimalFormat("00000000");
	
	/**
	 * For test usage
	 * **/
	private static  void testPolish(String originalFile, String SogouCA_NoTagFile, String leftFile, String urlFile, String refusedUrlFile){
		
		try {
			//original file
			ArrayList<String> lineList = IOText.getLinesAsAList(originalFile, "gbk");
			
			//file with publication date			
			BufferedWriter utf8Writer = IOText.getBufferedWriter(SogouCA_NoTagFile, "utf-8");
			
			//file without publication date, or zero-content
			BufferedWriter leftWriter = IOText.getBufferedWriter(leftFile, "utf-8");
			
			//file to store the accepted urls in the original file
			BufferedWriter acceptedUrlWriter = IOText.getBufferedWriter(urlFile, "utf-8");
			
			//file to store the refused url
			BufferedWriter refusedUrlWriter = IOText.getBufferedWriter(refusedUrlFile, "utf-8");
			
			StringBuffer docBuffer = new StringBuffer();
			
			for(String line: lineList){
				
				//filter unnormal characters
				line = noisyCharacters.matcher(line).replaceAll("");
				//due to different inner way of expression
				if(line.indexOf("&") >= 0){
		    		line = line.replaceAll("&", "&amp;");
		    	}				
				
				if(line.indexOf("</doc>") >= 0){
					
					docBuffer.append(line);
					
					//process
					totalDoc++;
					testToTemporaliaFormat(utf8Writer, docBuffer.toString(), leftWriter, acceptedUrlWriter, refusedUrlWriter);
					
					//new
					docBuffer.delete(0, docBuffer.length());
					
				}else {
					docBuffer.append(line);
				}
			}
			
			//
			utf8Writer.flush();
			utf8Writer.close();
			
			leftWriter.flush();
			leftWriter.close();
			
			acceptedUrlWriter.flush();
			acceptedUrlWriter.close();
			
			refusedUrlWriter.flush();
			refusedUrlWriter.close();
			
			System.out.println("total doc:\t"+totalDoc);
			System.out.println("accepted:\t"+acceptedDoc);
			System.out.println("refused:\t"+refusedDoc);
			System.out.println("including zero-contentDoc:\t"+zeroContentDoc);
			System.out.println("check\t\t"+(totalDoc==(acceptedDoc+refusedDoc)?"ture":"false"));

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}	
		
	}
	
	/**
	 * get the Temporalia-Styple (Non-Tagged) version
	 * **/
	public static void toTemporaliaStyle(String originalFile, String outputDir){
		
		try {
			//original file
			ArrayList<String> lineList = IOText.getLinesAsAList(originalFile, "gbk");
			
			StringBuffer docBuffer = new StringBuffer();
			
			for(String line: lineList){
				
				//filter unnormal characters
				line = noisyCharacters.matcher(line).replaceAll("");
				//due to different inner way of expression
				if(line.indexOf("&") >= 0){
		    		line = line.replaceAll("&", "&amp;");
		    	}				
				
				if(line.indexOf("</doc>") >= 0){
					
					docBuffer.append(line);
					
					//process
					totalDoc++;
					toTemporaliaStyple(outputDir, docBuffer.toString());
					
					//new
					docBuffer.delete(0, docBuffer.length());
					
				}else {
					docBuffer.append(line);
				}
			}
			
			//
			utf8Writer.flush();
			utf8Writer.close();
			
			//
			System.out.println("Total number of documents in the input file:\t"+totalDoc);
			System.out.println("Total number of accepted documents:\t"+acceptedDoc);
	
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}	
		
	}
	
	/**
	 * for meta-info processing
	 * **/
	
	// patterns used for extracting date from url
	//	/12/0702/
	private static Pattern publicationDate_1 = Pattern.compile("/12/[0-9]{4}/"); 
	//	/20120311/	or /2012324/	or	/201233/
	private static Pattern publicationDate_4 = Pattern.compile("/[0-9]{6,8}/"); 
	//	/2012/03/11/
	private static Pattern publicationDate_2 = Pattern.compile("/[0-9]{4}/[0-9]{2}/[0-9]{2}/"); 
	//	/2012/0311/
	private static Pattern publicationDate_3 = Pattern.compile("/[0-9]{4}/[0-9]{4}/"); 
	//detail_2012_06/12/
	private static Pattern publicationDate_5 = Pattern.compile("detail_[0-9]{4}_[0-9]{2}/[0-9]{2}/"); 
	//20120611.html
	private static Pattern publicationDate_6 = Pattern.compile("2012[0-9]{4}."); 
	
	//convert an original doc to the non-tagged version for Temporalia-2
	//for test
	private static void testToTemporaliaFormat(BufferedWriter utf8Writer, String docString, BufferedWriter leftWriter,
			BufferedWriter acceptedUrlWriter, BufferedWriter refusedUrlWriter){
		
		try {			
			
			SAXBuilder saxBuilder = new SAXBuilder();      
	        Document xmlDocSet = saxBuilder.build(new InputStreamReader(new ByteArrayInputStream(docString.getBytes("utf-8"))));
	        
	        Element rootElement = xmlDocSet.getRootElement(); 
	        
	        String url = rootElement.getChildText("url");
	        
	        
			String docno = rootElement.getChildText("docno");
			String contenttitle = rootElement.getChildText("contenttitle");
			String content = rootElement.getChildText("content");
			
			/**
			 * String str="[\u3002\uff1b\uff0c\uff1a\u201c\u201d\uff08\uff09\u3001\uff1f\u300a\u300b]"
			 * 该表达式可以识别出： 。 ；  ， ： “ ”（ ） 、 ？ 《 》 这些标点符号。
			 * **/
			content = content.replaceAll("[^０１２３４５６７８９a-z0-9A-Z\u4E00-\u9FFF\u3002\uff1b\uff0c\uff1a\u201c\u201d\uff08\uff09\u3001\uff1f\u300a\u300b]+", "");
			
			//for host
			int index = url.indexOf("://");
			String host = url.substring(index+3);
			host = url.substring(0, index+3+host.indexOf("/"));
			//for publication date			
			Matcher matcher_1 = publicationDate_1.matcher(url);
			Matcher matcher_2 = publicationDate_2.matcher(url);
			Matcher matcher_3 = publicationDate_3.matcher(url);
			Matcher matcher_4 = publicationDate_4.matcher(url);
			Matcher matcher_5 = publicationDate_5.matcher(url);
			Matcher matcher_6 = publicationDate_6.matcher(url);
			String dateStr = null;
			
			if(matcher_1.find()){
				dateStr = matcher_1.group();
				dateStr = convertDate_1(dateStr);
			}else if(matcher_2.find()){
				dateStr = matcher_2.group();
				dateStr = convertDate_2(dateStr);
			}else if(matcher_3.find()){
				dateStr = matcher_3.group();
				dateStr = convertDate_3(dateStr);
			}else if(matcher_4.find()){
				dateStr = matcher_4.group();
				dateStr = convertDate_4(dateStr);
			}else if(matcher_5.find()){
				dateStr = matcher_5.group();
				dateStr = convertDate_5(dateStr);
			}else if(matcher_6.find()){
				dateStr = matcher_6.group();
				dateStr = convertDate_6(dateStr);
			}	
		
			if(null != dateStr && content.length()>0){
				
				acceptedDoc++;
				
				acceptedUrlWriter.write(url);
				acceptedUrlWriter.newLine();
				
				utf8Writer.write("<doc id="+docno+">");
				utf8Writer.newLine();
				utf8Writer.write("<meta-info>");
				utf8Writer.newLine();
				utf8Writer.write("<tag name=\"host\">"+host+"</tag>");
				utf8Writer.newLine();
				utf8Writer.write("<tag name=\"date\">"+dateStr+"</tag>");
				utf8Writer.newLine();
				utf8Writer.write("<tag name=\"url\">"+url+"</tag>");
				utf8Writer.newLine();
				utf8Writer.write("<tag name=\"title\">"+contenttitle+"</tag>");
				utf8Writer.newLine();
				utf8Writer.write("<tag name=\"source-encoding\">UTF-8</tag>");
				utf8Writer.newLine();
				utf8Writer.write("</meta-info>");
				utf8Writer.newLine();
				utf8Writer.write("<text>");
				utf8Writer.newLine();
				utf8Writer.write(content);
				utf8Writer.newLine();
				utf8Writer.write("</text>");
				utf8Writer.newLine();
				utf8Writer.write("</doc>");
				utf8Writer.newLine();
				
			}else {
				
				refusedDoc++;
				if(0 == content.length()){
					zeroContentDoc++;
				}else {
					refusedUrlWriter.write(url);
					refusedUrlWriter.newLine();
				}
				
				//due to no-date, zero-content
				leftWriter.write(docString);
				leftWriter.newLine();
				
			}
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
	}
	
	private static BufferedWriter utf8Writer = null;
	
	private static void toTemporaliaStyple(String outputDir, String docString){	
		
		try {
			
			if(0 == acceptedDoc % docPerFile){			
				try {
					if(acceptedDoc > 0){
						utf8Writer.flush();
						utf8Writer.close();
						utf8Writer = null;
					}
					
					int k = acceptedDoc/docPerFile;
					String suffix = df.format(k);
					
					String outFileName = "SogouCA_TemNoTag_"+suffix+".xml";
					
					File outFile = new File(outputDir, outFileName);
					utf8Writer = IOText.getBufferedWriter(outFile.getAbsolutePath(), "utf-8");
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
			}
			
			SAXBuilder saxBuilder = new SAXBuilder();      
	        Document xmlDocSet = saxBuilder.build(new InputStreamReader(new ByteArrayInputStream(docString.getBytes("utf-8"))));
	        
	        Element rootElement = xmlDocSet.getRootElement(); 
	        
	        String url = rootElement.getChildText("url");
	        
	        
			String docno = rootElement.getChildText("docno");
			String contenttitle = rootElement.getChildText("contenttitle");
			String content = rootElement.getChildText("content");
			
			/**
			 * String str="[\u3002\uff1b\uff0c\uff1a\u201c\u201d\uff08\uff09\u3001\uff1f\u300a\u300b]"
			 * for recognizing ： 。 ；  ， ： “ ”（ ） 、 ？ 《 》 
			 * **/
			content = content.replaceAll("[^０１２３４５６７８９a-z0-9A-Z\u4E00-\u9FFF\u3002\uff1b\uff0c\uff1a\u201c\u201d\uff08\uff09\u3001\uff1f\u300a\u300b]+", "");
			
			//for host
			int index = url.indexOf("://");
			String host = url.substring(index+3);
			host = url.substring(0, index+3+host.indexOf("/"));
			//for publication date			
			Matcher matcher_1 = publicationDate_1.matcher(url);
			Matcher matcher_2 = publicationDate_2.matcher(url);
			Matcher matcher_3 = publicationDate_3.matcher(url);
			Matcher matcher_4 = publicationDate_4.matcher(url);
			Matcher matcher_5 = publicationDate_5.matcher(url);
			Matcher matcher_6 = publicationDate_6.matcher(url);
			String dateStr = null;
			
			if(matcher_1.find()){
				dateStr = matcher_1.group();
				dateStr = convertDate_1(dateStr);
			}else if(matcher_2.find()){
				dateStr = matcher_2.group();
				dateStr = convertDate_2(dateStr);
			}else if(matcher_3.find()){
				dateStr = matcher_3.group();
				dateStr = convertDate_3(dateStr);
			}else if(matcher_4.find()){
				dateStr = matcher_4.group();
				dateStr = convertDate_4(dateStr);
			}else if(matcher_5.find()){
				dateStr = matcher_5.group();
				dateStr = convertDate_5(dateStr);
			}else if(matcher_6.find()){
				dateStr = matcher_6.group();
				dateStr = convertDate_6(dateStr);
			}	
		
			if(null != dateStr && content.length()>0){
				
				
				
				acceptedDoc++;			
				
				utf8Writer.write("<doc id=\""+docno+"\">");
				utf8Writer.newLine();
				utf8Writer.write("<meta-info>");
				utf8Writer.newLine();
				utf8Writer.write("<tag name=\"host\">"+host+"</tag>");
				utf8Writer.newLine();
				utf8Writer.write("<tag name=\"date\">"+dateStr+"</tag>");
				utf8Writer.newLine();
				utf8Writer.write("<tag name=\"url\">"+url+"</tag>");
				utf8Writer.newLine();
				utf8Writer.write("<tag name=\"title\">"+contenttitle+"</tag>");
				utf8Writer.newLine();
				utf8Writer.write("<tag name=\"source-encoding\">UTF-8</tag>");
				utf8Writer.newLine();
				utf8Writer.write("</meta-info>");
				utf8Writer.newLine();
				utf8Writer.write("<text>");
				utf8Writer.newLine();
				utf8Writer.write(content);
				utf8Writer.newLine();
				utf8Writer.write("</text>");
				utf8Writer.newLine();
				utf8Writer.write("</doc>");
				utf8Writer.newLine();
				
			}
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
	}
	
	//	//	/20120311/	or /2012324/	or	/201233/
	private static  String convertDate_4(String rawStr) {
		
		String dateStr = rawStr.substring(1, rawStr.length()-1);
		
		String yearStr = null;
		String monStr = null;
		String dayStr = null;
		
		if(dateStr.length() == 6){
			yearStr = dateStr.substring(0, 4);
			monStr = dateStr.substring(4, 5);
			dayStr = dateStr.substring(5, 6);
		}else if(dateStr.length() == 7){
			yearStr = dateStr.substring(0, 4);
			monStr = dateStr.substring(4, 5);
			dayStr = dateStr.substring(5, 7);
		}else if(dateStr.length() == 8){
			yearStr = dateStr.substring(0, 4);
			monStr = dateStr.substring(4, 6);
			dayStr = dateStr.substring(6, 8);
		}
		
		return yearStr+"-"+monStr+"-"+dayStr;
		
	}
	//	/12/0702/
	private static  String convertDate_1(String rawStr) {
		
		String yearStr = "2012";
		String monStr = rawStr.substring(4, 6);
		String dayStr = rawStr.substring(6, 8);
		
		return yearStr+"-"+monStr+"-"+dayStr;
		
	}
	//	/2012/06/11/
	private static  String convertDate_2(String rawStr) {
		
		String yearStr = rawStr.substring(1, 5);
		String monStr = rawStr.substring(6, 8);
		String dayStr = rawStr.substring(9, 11);
		
		return yearStr+"-"+monStr+"-"+dayStr;
		
	}
	//	/2012/0311/
	private static  String convertDate_3(String rawStr) {
		
		String yearStr = rawStr.substring(1, 5);
		String monStr = rawStr.substring(6, 8);
		String dayStr = rawStr.substring(8, 10);
		
		return yearStr+"-"+monStr+"-"+dayStr;
		
	}
	//	detail_2012_06/12/
	private static  String convertDate_5(String rawStr) {
		
		String yearStr = rawStr.substring(7, 11);
		String monStr = rawStr.substring(12, 14);
		String dayStr = rawStr.substring(15, 17);
		
		return yearStr+"-"+monStr+"-"+dayStr;
		
	}
	//	20120611.html
	private static  String convertDate_6(String rawStr) {
		String dateStr = rawStr.substring(0, rawStr.length()-1);
		
		String yearStr = dateStr.substring(0, 4);
		String monStr = dateStr.substring(4, 6);
		String dayStr = dateStr.substring(6, 8);
		
		return yearStr+"-"+monStr+"-"+dayStr;
		
	}
	
	//test
	private static void testUsage(){
		//1 SogouCAMini
		
		/*
		String oriFile = "collectionTest/news_tensite_xml.smarty.dat";
		
		String SogouCA_Mini_NoTagFile = "collectionTest/ToBeReleased/SogouCA_Mini_NoTag.txt";
		
		String SogouCA_Mini_LeftFile = "collectionTest/ToBeReleased/SogouCA_Mini_Left.txt";

		Preprocesor.polish(oriFile, SogouCA_Mini_NoTagFile, SogouCA_Mini_LeftFile);
		*/
		
		//2	SogouCA
		///*
		String oriFile = "../../Corpus/SogouCA/news_tensite_xml.dat";
		
		String SogouCA_NoTagFile = "collectionTest/ToBeReleased/SogouCA_NoTag.txt";
		
		String SogouCA_LeftFile = "collectionTest/ToBeReleased/SogouCA_Left.txt";
		
		String SogouCA_AcceptedUrlFile = "collectionTest/ToBeReleased/SogouCA_URL_Accepted.txt";
		
		String SogouCA_RefusedUrl_File = "collectionTest/ToBeReleased/SogouCA_URL_Refused.txt"; 
		
		Preprocesor.testPolish(oriFile, SogouCA_NoTagFile, SogouCA_LeftFile, SogouCA_AcceptedUrlFile, SogouCA_RefusedUrl_File);
		//*/
	}
	
	//real usage
	private static void realUsage(){
		
		//Case-1 SogouCAMini
		
		/*
		String oriFile = "collectionTest/news_tensite_xml.smarty.dat";
		
		String outputDir = "collectionTest/NoTagVersion/";
		
		Preprocesor.toTemporaliaStyle(oriFile, outputDir);
		*/
		
		//Case-2 SogouCA
		/*
		String oriFile = "../../Corpus/SogouCA/news_tensite_xml.dat";
		
		String outputDir = "collectionTest/NoTagVersion/";
		
		Preprocesor.toTemporaliaStyle(oriFile, outputDir);
		*/
	}
	
	
	/**
	 * This class performs pre-process required for Temporalia-Style tagging
	 * 
	 * @param oriFile the original raw data file, e.g., news_tensite_xml.smarty.dat or news_tensite_xml.dat
	 * @param outputDir the output directory
	 * 
	 * **/
	public static void main(String []args){
		//// Example usage
		
		//Case-1 SogouCAMini
		
		///*
		String oriFile = "collectionTest/news_tensite_xml.smarty.dat";
		
		String outputDir = "collectionTest/NoTagVersion/";
		
		Preprocesor.toTemporaliaStyle(oriFile, outputDir);
		
		//*/
		
		//Case-2 SogouCA, a larger dataset
		/*
		String oriFile = "../../Corpus/SogouCA/news_tensite_xml.dat";
		
		String outputDir = "collectionTest/NoTagVersion/";
		
		Preprocesor.toTemporaliaStyle(oriFile, outputDir);
		*/		
		
	}

}
