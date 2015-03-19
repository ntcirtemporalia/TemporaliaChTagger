/**
 * 
 */
package org.archive.util.tuple;

import java.util.Collections;
import java.util.Vector;

/**
 * @author Administrator
 *
 */
public class Pair<FIRST, SECOND> implements PairInterface<FIRST, SECOND> {
	//
	public FIRST first;
	//
	public SECOND second;
	
	//
	public Pair (FIRST first, SECOND second){
		this.first = first;
		this.second = second;		
	}
	//@Override
	public final FIRST getFirst(){
		return first;
	}
	public final void setFirst (FIRST first) {
		this.first = first;		
	}
	//@Override
	public final SECOND getSecond(){
		return second;
	}
	public final void setSecond(SECOND second){
		this.second = second;
	}
	@Override
	public boolean equals(Object obj){
		if (this == obj) {
			return true;
		}
		if (null == obj) {
			return false;
		}
		if (!(obj instanceof Pair)) {
			return false;
		}
		Pair<FIRST, SECOND> other = (Pair<FIRST, SECOND>)obj;
		//
		return null==this.first? null==other.first : this.first.equals(other.first) 
				&& null==this.second? null==other.second : this.second.equals(other.second);
	}
	@Override
	public int hashCode(){
		final long prime = 2654435761L;
		//
		long result = 1;
		result = prime * result + (null==first? 0:first.hashCode());
		result = prime * result + (null==second? 0:second.hashCode());
		//
		return (int)result;		
	}
	public String toString(){
		return first.toString()+" : "+second.toString();
	}
	//
	public static void main(String []args){
		//test-1
		Vector<Pair<String, Double>> tStrVector = new Vector<Pair<String,Double>>();
		//
		tStrVector.add(new Pair<String, Double>("yama", 2.0));
		tStrVector.add(new Pair<String, Double>("hashi", 1.0));
		//asc
		Collections.sort(tStrVector, new PairComparatorBySecond_Asc<String, Double>());		
		System.out.println("ASC");
		for(Pair<String, Double> p: tStrVector){
			System.out.println(p.getFirst()+"\t"+p.getSecond());
		}
		//desc
		Collections.sort(tStrVector, new PairComparatorBySecond_Desc<String, Double>());
		System.out.println("DESC");
		for(Pair<String, Double> p: tStrVector){
			System.out.println(p.getFirst()+"\t"+p.getSecond());
		}
	}
}
