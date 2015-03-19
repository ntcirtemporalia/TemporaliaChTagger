package org.archive.sogou;

import java.io.PrintStream;

import de.unihd.dbs.sogou.TemSogouTagger;


/**
 * 
 * This script assembles two off-the-shelf open-source tools, Stanford CoreNLP and HeidelTime.
 * 
 * Stanford CoreNLP is used to perform Named Entity recognition, for detailed information, please refer to http://nlp.stanford.edu/software/corenlp.shtml
 * HeidelTime is used to perform Temporal expression annotation, for detailed information, please refer to https://code.google.com/p/heideltime/
 * 
 * All rights reserved. This program and the accompanying materials are made available under the terms of the GNU General Public License, with exceptions for 3rd party software & data.
 * 
 * **/

public class TemChTagger {
	
	//case-1: 	-p oriFile outputDir	
	//e.g., -p collectionTest/news_tensite_xml.smarty.dat collectionTest/NoTagVersion/
	
	/**
	* This setting performs pre-process required for Temporalia-Style tagging
	* 
	* @param oriFile the original raw data file, e.g., news_tensite_xml.smarty.dat or news_tensite_xml.dat
	* @param outputDir the output directory
	* 
	* **/
	
	
	//case-2:		-t NoTagFileDir	TagFileDir	
	//e.g., -t collectionTest/NoTagVersion/ collectionTest/TagVersion/
	/**
	* This setting performs Temporalia-Style tagging given the files generated as above
	* 
	* @param NoTagFileDir the directory of the non-tagged files generated as case-1
	* @param TagFileDir:	the output directory, i.e., the directory to store the tagged files
	*
	* **/
	
	public static void main(String [] args){
		
		
		if(args.length != 3){
			
			System.err.println("Parameter setting error!");
			
		}else{
			//e.g., calling
			//-p collectionTest/news_tensite_xml.smarty.dat collectionTest/NoTagVersion/
			
			if(args[0].endsWith("p")){
				
				Preprocesor.toTemporaliaStyle(args[1], args[2]);
				
			}else if(args[0].endsWith("t")){
				//e.g., calling -t collectionTest/NoTagVersion/ collectionTest/TagVersion/
				
				try {

					PrintStream logPrinter = new PrintStream("tag_log.txt");
					System.setErr(logPrinter);
					
					try {
						TemSogouTagger temSogouTagger = new TemSogouTagger(args[1], args[2]);
						temSogouTagger.run();
					} catch (Exception ee) {
						// TODO: handle exception
						ee.printStackTrace();
					}					
					
					logPrinter.flush();
					logPrinter.close();
					
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
			}else {
				System.err.println("Parameter setting error!");
			}
			
		}
		
	}

}
