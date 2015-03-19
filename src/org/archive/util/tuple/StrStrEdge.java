package org.archive.util.tuple;

public class StrStrEdge extends Pair<String, String>{
	//
	public StrStrEdge(String first, String second){
		super(first, second);
	}
	/**
	 * order irrelevant
	 * **/	
	@Override
	public final int hashCode(){	
		//
		long result = 1;
		result += (null==first? 0:first.hashCode());
		result += (null==second? 0:second.hashCode());
		//
		return (int)result;		
	}
}
