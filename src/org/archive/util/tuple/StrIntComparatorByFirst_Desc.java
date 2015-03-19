package org.archive.util.tuple;

import java.util.Comparator;

public class StrIntComparatorByFirst_Desc implements Comparator<StrInt> {
	//attributes of edge between words
	public static enum CmpType{StrLength, StrAlphabet}
	//
	CmpType cmpType;
	public StrIntComparatorByFirst_Desc(CmpType cmpType){
		this.cmpType = cmpType;
	}
	//@Override
	public int compare(StrInt obj1, StrInt obj2){
		if(this.cmpType == CmpType.StrLength){
			//by str length
			if(null!=obj1.first && null!=obj2.first){
				Integer obj1Len = obj1.first.length();
				Integer obj2Len = obj2.first.length();
				return obj2Len.compareTo(obj1Len);			
			}else if(null != obj1.first){
				return -1;
			}else if(null != obj2.first){
				return 1;
			}else{
				return 0;
			}
		}else{
			//by str default order
			if(null!=obj1.first && null!=obj2.first){				
				return obj1.first.compareTo(obj2.first);		
			}else if(null != obj1.first){
				return -1;
			}else if(null != obj2.first){
				return 1;
			}else{
				return 0;
			}
		}				
	}
}
