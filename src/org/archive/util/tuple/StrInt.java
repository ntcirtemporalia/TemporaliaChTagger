package org.archive.util.tuple;


/**
 * String object and its frequency
 * **/
public class StrInt extends Pair<String, Integer> {		
	public StrInt(String str){
		super(str, 1);		
	}
	//
	public StrInt(String str, int intK){
		super(str, intK);
	}
	//
	public void intPlus1(){
		this.second +=1;
	}
	public void intPlusk(int k){
		this.second += k;
	}	
}

