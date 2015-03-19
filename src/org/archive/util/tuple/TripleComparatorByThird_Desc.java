package org.archive.util.tuple;

import java.util.Comparator;

public class TripleComparatorByThird_Desc <FIRST extends Comparable<? super FIRST>,
											SECOND extends Comparable<? super SECOND>,
											THIRD extends Comparable<? super THIRD>>
												implements Comparator<Triple <FIRST, SECOND, THIRD>> {
	//@Override
	public int compare(Triple <FIRST, SECOND, THIRD> obj1, Triple <FIRST, SECOND, THIRD> obj2){
		if(null!=obj1.third && null!=obj2.third){
			return obj2.third.compareTo(obj1.third);			
		}else if(null != obj1.third){
			return -1;
		}else if(null != obj2.third){
			return 1;
		}else{
			return 0;
		}		
	}
}