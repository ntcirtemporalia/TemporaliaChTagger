package org.archive.util;

public class Language {
	//
	public static enum Lang{English, Chinese}
	
	//
	public static boolean isChinese(Lang lang){
		return lang == Lang.Chinese? true: false;		
	}
	//
	public static boolean isEnglish(Lang lang){
		return lang == Lang.English? true: false;	
	}

}
