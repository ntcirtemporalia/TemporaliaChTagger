package org.archive.util.tuple;

public class StrStrInt extends Triple<String, String, Integer> {
	public StrStrInt(String strFirst, String strSecond, Integer intThird){
		super(strFirst, strSecond, intThird);
	}
	//
	public void upThird(int deltaInt){
		this.third += deltaInt;
	}
}
