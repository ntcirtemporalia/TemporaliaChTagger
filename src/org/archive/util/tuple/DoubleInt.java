package org.archive.util.tuple;

public class DoubleInt extends Pair<Double, Integer> {
	
	public DoubleInt(Double dFirst, Integer intSecond){
		super(dFirst, intSecond);
	}
	
	public String toString(){
		return this.first.toString()+" "+this.second.toString();
	}
	

}
