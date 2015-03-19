package org.archive.util.tuple;

import java.util.Comparator;

public class PairComparatorByFirst_Asc<FIRST extends Comparable<? super FIRST>, SECOND extends Comparable<? super SECOND>> 
implements Comparator<Pair<FIRST, SECOND>> {
//
//@Override
public int compare(Pair<FIRST, SECOND> obj1, Pair<FIRST, SECOND> obj2){
	if(null!=obj1.first && null!=obj2.first){
		return obj1.first.compareTo(obj2.first);
	}else if(null != obj1.first){
		return 1;
	}else if(null != obj2.first){
		return -1;
	}else{
		return 0;
	}
}
}
