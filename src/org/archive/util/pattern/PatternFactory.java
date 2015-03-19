package org.archive.util.pattern;

import java.io.BufferedReader;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.archive.util.io.IOText;

/**
 * a set of patterns
 * **/
public class PatternFactory {
	//edition pattern
	//year problem, e.g., 2013
	//public static Pattern editionPattern = Pattern.compile("[a-z0-9A-Z]+[.。]+[a-z0-9A-Z]+|[0-9]{1,2}");
	public static Pattern editionPattern = Pattern.compile("[a-z0-9A-Z]+[.。]+[a-z0-9A-Z]+");
	//time pattern : [12]{1}[0-9]{3}
	public static Pattern timePattern = Pattern.compile("(\\d+[年月日]+|[12]{1}[0-9]{3})+");	
	//TV pattern
	public static Pattern tvPattern = Pattern.compile("[第]*[0-9一二三四五六七八九十]+[部集期号季]");
	//number and alphabet
	public static Pattern numAlphabetPattern = Pattern.compile("[a-z0-9A-Z_]+");
	//alphabet
	public static Pattern alphabetPattern = Pattern.compile("[a-zA-Z_]+");
	//number alphabet Chinese
	public static Pattern nacPattern = Pattern.compile("[a-z0-9A-Z_]+[版号级]");
	//place pattern
	public static Pattern placePattern = Pattern.compile("[\u4E00-\u9FFF]+[县省市区国]");
	//web net	
	public static String net_1 = "(http|www|WWW|ftp|site){1,}(://)?" +
	"(\\.(\\w+(-\\w+)*))*((:\\d+)?)(/(\\w+(-\\w+)*))*(\\.?(\\w)*)(\\?)?(((\\w*%)*(\\w*\\?)*(\\w*:)*(\\w*\\+)*(\\w*\\.)*(\\w*&)*(\\w*-)*(\\w*=)*(\\w*%)*(\\w*\\?)*(\\w*:)*(\\w*\\+)*(\\w*\\.)*(\\w*&)*(\\w*-)*(\\w*=)*)*(\\w*)*)(/)*";
	public static String netSum = "("+net_1+"|\\w+(.com)+)";
	//
	public static Pattern netPattern = Pattern.compile(netSum);
	//mail pattern
	public static Pattern mailPattern = Pattern.compile("([a-z0-9A-Z]+[-|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}");
	/**
	 * ^[\u2E80-\u9FFF]+$   匹配所有东亚区的语言    
	 * ^[\u4E00-\u9FFF]+$   匹配简体和繁体  
	 * ^[\u4E00-\u9FA5]+$   匹配简体  	
	 */
	public static Pattern nonSimpleChPattern = Pattern.compile("[^\u4E00-\u9FA5]+");
	//separator symbol
	public static Pattern separatorSymbolPattern = Pattern.compile("[^a-z0-9A-Z\u4E00-\u9FFF]+");
	//
	public static Pattern chPattern = Pattern.compile("[\u4E00-\u9FFF]+");
	//
	public static boolean mustBeModifier(String wStr){
		if(isTimeWord(wStr) || isPlaceWord(wStr)||
				isTvWord(wStr) || isNacWord(wStr)){
			//
			return true;
		}else{
			return false;
		}
	}
	//
	public static boolean isTimeWord(String wStr){
		Matcher matcher = timePattern.matcher(wStr);
		if(matcher.find()){
			String mStr = matcher.group();			
			//
			if(mStr.equals(wStr)){
				return true;
			}else{
				return false;
			}
		}else{
			return false;
		}
	}
	//
	private static boolean placedicIni = false;
	private static Hashtable<String, Boolean> placeDict = new Hashtable<String, Boolean>();
	private static void iniPlaceDict(){
		try{
			BufferedReader reader = IOText.getBufferedReader_UTF8("E:/Data_Log/Dictionary/TotalSourceDictionary/PlaceList/PlaceDict_A1.dic");			
			String line;			
			while(null != (line=reader.readLine())){
				if(!placeDict.containsKey(line)){
					placeDict.put(line, true);
				}				
			}
			reader.close();
			//
			placedicIni = true;
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	private static boolean inPlaceDict(String word){
		if(!placedicIni){
			iniPlaceDict();
		}
		//
		if(placeDict.containsKey(word)){
			return true;
		}else{
			return false;
		}
	}
	//
	public static boolean isPlaceWord(String wStr){
		//
		if(inPlaceDict(wStr)){
			return true;
		}
		//
		Matcher matcher = placePattern.matcher(wStr);
		if(matcher.find()){
			String mStr = matcher.group();			
			//
			if(mStr.equals(wStr)){
				return true;
			}else{
				return false;
			}
		}else{
			return false;
		}
	}
	//
	public static boolean isTvWord(String wStr){
		Matcher matcher = tvPattern.matcher(wStr);
		if(matcher.find()){
			String mStr = matcher.group();			
			//
			if(mStr.equals(wStr)){
				return true;
			}else{
				return false;
			}
		}else{
			return false;
		}
	}
	//
	public static boolean isNacWord(String wStr){
		Matcher matcher = nacPattern.matcher(wStr);
		if(matcher.find()){
			String mStr = matcher.group();			
			//
			if(mStr.equals(wStr)){
				return true;
			}else{
				return false;
			}
		}else{
			return false;
		}
	}
	//
	public static boolean isEditionWord(String wStr){
		if(wStr.equals("版") || wStr.equals("网") || wStr.equals("学院") || wStr.equals("集")){
			return true;
		}
		Matcher matcher = editionPattern.matcher(wStr);
		if(matcher.find()){
			String mStr = matcher.group();			
			//
			if(mStr.equals(wStr)){
				return true;
			}else{
				return false;
			}
		}else{
			return false;
		}
	}
	//
	//
	public static boolean containSeparatorSymbol(String query){
		Matcher matcher = separatorSymbolPattern.matcher(query);
		if(matcher.find()){
			return true;
		}else{
			return false;
		}
	}
	//including non-Chinese character
	public static boolean containNonSimpleChC(String query){
		Matcher matcher = nonSimpleChPattern.matcher(query);
		if(matcher.find()){
			return true;
		}else{
			return false;
		}
	}
	//including Chinese character
	public static boolean containHanCharacter(String str){		
		Matcher mat = chPattern.matcher(str);
		if(mat.find()){
			return true;
		}else{
			return false;
		}
	}	
	//including alphabet
	public static boolean containAlphabet(String str){		
		Matcher mat = alphabetPattern.matcher(str);
		if(mat.find()){
			return true;
		}else{
			return false;
		}
	}
	//whether all number and alphabet in given str
	public static boolean allNAStr(String str){		
		Matcher matcher = numAlphabetPattern.matcher(str);
		if(matcher.find()){					
			if(matcher.group().equals(str)){
				return true;
			}
		}
		//
		return false;
	}
	public static void main(String []args){
		/**1**/
		//test time word
		String w = "2006年的";
		if(PatternFactory.isTimeWord(w)){
			System.out.println("Yes");
		}		
	}
}
