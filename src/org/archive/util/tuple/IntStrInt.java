package org.archive.util.tuple;

public class IntStrInt extends Triple<Integer, String, Integer> implements Comparable {
	
	public IntStrInt(Integer id, String text, Integer fre){
		super(id, text, fre);
	}
	
	public int compareTo(Object o) {
		IntStrInt cmp = (IntStrInt) o;			
		return this.second.compareTo(cmp.second);
	}
}
