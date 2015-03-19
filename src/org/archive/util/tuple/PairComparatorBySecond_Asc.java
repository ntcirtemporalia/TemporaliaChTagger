package org.archive.util.tuple;

import java.util.Comparator;

public class PairComparatorBySecond_Asc<FIRST extends Comparable<? super FIRST>, SECOND extends Comparable<? super SECOND>> 
	implements Comparator<Pair<FIRST, SECOND>> {
	//
	//@Override
	public int compare(Pair<FIRST, SECOND> obj1, Pair<FIRST, SECOND> obj2){
		if(null!=obj1.second && null!=obj2.second){
			return obj1.second.compareTo(obj2.second);
		}else if(null != obj1.second){
			return 1;
		}else if(null != obj2.second){
			return -1;
		}else{
			return 0;
		}
	}
}
