package org.archive.util.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;

import org.archive.util.tuple.StrStr;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class XmlWriter {
	//
	private ContentHandler contentHandler;
	private  OutputStream outputStream;
	//private AttributesImpl att;
	
	XmlWriter(ContentHandler contentHandler, OutputStream outputStream){
		this.contentHandler = contentHandler;
		this.outputStream = outputStream;
		//this.att = null;		
	}
	
	private ContentHandler getContentHandler(){
		return this.contentHandler;
	}
	private OutputStream getOutputStream(){
		return this.outputStream;
	}	
	//
	public void startDocument(String rootName) throws SAXException{
		getContentHandler().startDocument();
		getContentHandler().startElement("", "", rootName, null);
	}
	public void startDocument(String rootName, Vector<StrStr> attList) throws SAXException{
		AttributesImpl attribute = new AttributesImpl();
		for(StrStr kv: attList){
			attribute.addAttribute("", "", kv.first, "", kv.second);
		}		
		getContentHandler().startDocument();		
		getContentHandler().startElement("", "", rootName, attribute);
	}
	public void endDocument(String rootName)throws SAXException{		
		getContentHandler().endElement("", "", rootName);
		getContentHandler().endDocument();
	}
	//
	public void startElement(String elementName)throws SAXException{
		getContentHandler().startElement("", "", elementName, null);
	}
	public void startElement(String elementName, Vector<StrStr> attList)throws SAXException{
		AttributesImpl attribute = new AttributesImpl();
		for(StrStr kv: attList){
			attribute.addAttribute("", "", kv.first, "", kv.second);
		}		
		//
		getContentHandler().startElement("", "", elementName, attribute);
	}
	public void startElement(String elementName, AttributesImpl att)throws SAXException{
		getContentHandler().startElement("", "", elementName, att);
	}
	public void endElement(String elementName)throws SAXException{
		getContentHandler().endElement("", "", elementName);
	}
	//
	public void writeElement(String elementName, String elementContent)throws SAXException{
		getContentHandler().startElement("", "", elementName, null);
		getContentHandler().characters(elementContent.toCharArray(), 0, elementContent.length());
		getContentHandler().endElement("", "", elementName);
	}
	public void writeElement(String elementName, Vector<StrStr> attList)throws SAXException{
		AttributesImpl attribute = new AttributesImpl();
		for(StrStr kv: attList){
			attribute.addAttribute("", "", kv.first, "", kv.second);
		}	
		getContentHandler().startElement("", "", elementName, attribute);		
		getContentHandler().endElement("", "", elementName);
	}
	public void writeElement(String elementName, Vector<StrStr> attList, String elementContent)throws SAXException{
		AttributesImpl attribute = new AttributesImpl();
		for(StrStr kv: attList){
			attribute.addAttribute("", "", kv.first, "", kv.second);
		}	
		getContentHandler().startElement("", "", elementName, attribute);
		getContentHandler().characters(elementContent.toCharArray(), 0, elementContent.length());
		getContentHandler().endElement("", "", elementName);
	}
	//
	public void flush() throws IOException{
		getOutputStream().flush();
	}
	public void close() throws IOException{
		getOutputStream().close();
	}
}
