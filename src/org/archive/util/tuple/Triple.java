package org.archive.util.tuple;

public class Triple <FIRST, SECOND, THIRD> implements TripleInterface<FIRST, SECOND, THIRD> {
	//
	public FIRST first;
	public SECOND second;
	public THIRD third;
	
	//
	public Triple(FIRST fir, SECOND sec, THIRD thi){
		this.first = fir;
		this.second = sec;
		this.third = thi;
	}
	
	public final FIRST getFirst(){
		return first;
	}
	public final void setFirst (FIRST first) {
		this.first = first;		
	}
	public final SECOND getSecond(){
		return second;
	}
	public final void setSecond(SECOND second){
		this.second = second;
	}
	public final THIRD getThird(){
		return this.third;
	}
	public final void setThird(THIRD third){
		this.third = third;
	}
	
	@Override
	public boolean equals(Object obj){
		if (this == obj) {
			return true;
		}
		if (null == obj) {
			return false;
		}
		if (!(obj instanceof Triple)) {
			return false;
		}
		Triple <FIRST, SECOND, THIRD> cmpTriple = (Triple <FIRST, SECOND, THIRD>)obj;
		//
		return null==this.first? null==cmpTriple.first : this.first.equals(cmpTriple.first) 
				&& (null==this.second? null==cmpTriple.second : this.second.equals(cmpTriple.second)
						&& (null==this.third? null==cmpTriple.third : this.third.equals(cmpTriple.third)));
	}
	@Override
	public final int hashCode(){
		final long prime = 2654435761L;
		//
		long result = 1;
		result = prime * result + (null==first? 0:first.hashCode());
		result = prime * result + (null==second? 0:second.hashCode());
		result = prime * result + (null==third? 0:third.hashCode());
		//
		return (int)result;		
	}
	public String toString(){
		return first.toString()+" : "+second.toString()+" : "+third.toString();
	}

}
