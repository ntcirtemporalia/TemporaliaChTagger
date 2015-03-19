package de.unihd.dbs.heideltime.standalone.components.impl;

import java.util.HashSet;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;

import de.florianlaws.uima.types.stanford.GPE;
import de.florianlaws.uima.types.stanford.Location;
import de.florianlaws.uima.types.stanford.Misc;
import de.florianlaws.uima.types.stanford.Organization;
import de.florianlaws.uima.types.stanford.Person;
import de.unihd.dbs.heideltime.standalone.components.ResultFormatter;
import de.unihd.dbs.uima.types.heideltime.Sentence;
import de.unihd.dbs.uima.types.heideltime.Timex3;
import de.unihd.dbs.uima.types.heideltime.Timex3Interval;
import de.unihd.dbs.uima.annotator.stanfordtagger.StanfordCoreNLPWrapper;

public class NERTimeResultFormatter implements ResultFormatter{
	
	
	public String format(JCas jcas) throws Exception {
		
		final String documentText = jcas.getDocumentText();
		
		// get the timex3 intervals, do some pre-selection on them
		FSIterator iterIntervals = jcas.getAnnotationIndex(Timex3Interval.type).iterator();
		TreeMap<Integer, Timex3Interval> intervals = new TreeMap<Integer, Timex3Interval>();
		while(iterIntervals.hasNext()) {
			Timex3Interval t = (Timex3Interval) iterIntervals.next();
			
			// disregard intervals that likely aren't a real interval, but just a timex-translation
			if(t.getTimexValueEB().equals(t.getTimexValueLB()) && t.getTimexValueEE().equals(t.getTimexValueLE()))
				continue;
			
			if(intervals.containsKey(t.getBegin())) {
				Timex3Interval tInt = intervals.get(t.getBegin());
				
				// always get the "larger" intervals
				if(t.getEnd() - t.getBegin() > tInt.getEnd() - tInt.getBegin()) {
					intervals.put(t.getBegin(), t);
				}
			} else {
				intervals.put(t.getBegin(), t);
			}
		}

		/* 
		 * loop through the timexes to create two treemaps:
		 * - one containing startingposition=>timex tuples for eradication of overlapping timexes
		 * - one containing endposition=>timex tuples for assembly of the XML file
		 */
		FSIterator iterTimex = jcas.getAnnotationIndex(Timex3.type).iterator();
		TreeMap<Integer, Timex3> forwardTimexes = new TreeMap<Integer, Timex3>(),
				backwardTimexes = new TreeMap<Integer, Timex3>();
		while(iterTimex.hasNext()) {
			Timex3 t = (Timex3) iterTimex.next();
			forwardTimexes.put(t.getBegin(), t);
			backwardTimexes.put(t.getEnd(), t);
		}
		
		HashSet<Timex3> timexesToSkip = new HashSet<Timex3>();
		Timex3 prevT = null;
		Timex3 thisT = null;
		// iterate over timexes to find overlaps
		for(Integer begin : forwardTimexes.navigableKeySet()) {
			thisT = (Timex3) forwardTimexes.get(begin);
			
			// check for whether this and the previous timex overlap. ex: [early (friday] morning)
			if(prevT != null && prevT.getEnd() > thisT.getBegin()) {
				
				Timex3 removedT = null; // only for debug message
				// assuming longer value string means better granularity
				if(prevT.getTimexValue().length() > thisT.getTimexValue().length()) {
					timexesToSkip.add(thisT);
					removedT = thisT;
					/* prevT stays the same. */
				} else {
					timexesToSkip.add(prevT);
					removedT = prevT;
					prevT = thisT; // this iteration's prevT was removed; setting for new iteration 
				}
				
				// ask user to let us know about possibly incomplete rules
				Logger l = Logger.getLogger("TimeMLResultFormatter");
				l.log(Level.WARNING, "Two overlapping Timexes have been discovered:" + System.getProperty("line.separator")
						+ "Timex A: " + prevT.getCoveredText() + " [\"" + prevT.getTimexValue() + "\" / " + prevT.getBegin() + ":" + prevT.getEnd() + "]" 
						+ System.getProperty("line.separator")
						+ "Timex B: " + removedT.getCoveredText() + " [\"" + removedT.getTimexValue() + "\" / " + removedT.getBegin() + ":" + removedT.getEnd() + "]" 
						+ " [removed]" + System.getProperty("line.separator")
						+ "The writer chose, for granularity: " + prevT.getCoveredText() + System.getProperty("line.separator")
						+ "This usually happens with an incomplete ruleset. Please consider adding "
						+ "a new rule that covers the entire expression.");
			} else { // no overlap found? set current timex as next iteration's previous timex
				prevT = thisT;
			}
		}
		
		//--
		FSIterator iterSen = jcas.getAnnotationIndex(Sentence.type).iterator();
		TreeMap<Integer, Sentence> sentenceMap = new TreeMap<Integer, Sentence>();
		while(iterSen.hasNext()) {
			Sentence sen = (Sentence) iterSen.next();
			sentenceMap.put(sen.getBegin(), sen);
		}
		
		FSIterator iterStPerson = jcas.getAnnotationIndex(Person.type).iterator();
		TreeMap<Integer, Person> stPersonMap = new TreeMap<Integer, Person>();
		while(iterStPerson.hasNext()) {
			Person stPerson = (Person) iterStPerson.next();
			stPersonMap.put(stPerson.getBegin(), stPerson);
		}
		
		FSIterator iterStOrg = jcas.getAnnotationIndex(Organization.type).iterator();
		TreeMap<Integer, Organization> stOrgMap = new TreeMap<Integer, Organization>();
		while(iterStOrg.hasNext()) {
			Organization stOrg = (Organization) iterStOrg.next();
			stOrgMap.put(stOrg.getBegin(), stOrg);
		}
		
		FSIterator iterStLoc = jcas.getAnnotationIndex(Location.type).iterator();
		TreeMap<Integer, Location> stLocMap = new TreeMap<Integer, Location>();
		while(iterStLoc.hasNext()) {
			Location stLoc = (Location) iterStLoc.next();
			stLocMap.put(stLoc.getBegin(), stLoc);
		}
		
		FSIterator iterStMisc = jcas.getAnnotationIndex(Misc.type).iterator();
		TreeMap<Integer, Misc> stMiscMap = new TreeMap<Integer, Misc>();
		while(iterStMisc.hasNext()) {
			Misc stMisc = (Misc) iterStMisc.next();
			stMiscMap.put(stMisc.getBegin(), stMisc);
		}
		//--
		
		String outText = new String();
		
		outText += "<text>";

		// alternative xml creation method
		Timex3Interval interval = null;
		Timex3 timex = null;
		//--
		Sentence sentence = null;
		Person stPerson = null;
		Organization stOrganization = null;
		Location stLocation = null;
		Misc stMisc = null;
		//--
		for(Integer docOffset = 0; docOffset <= documentText.length(); docOffset++) {
			
			//(1)
			/**
			 *  see if we have to finish off old timexes/intervals
			 */
			if(timex != null && timex.getEnd() == docOffset) {
				outText += "</TIMEX3>";
				timex = null;
			}
			if(interval != null && interval.getEnd() == docOffset) {
				outText += "</TIMEX3INTERVAL>";
				interval = null;
			}
			//(2)
			//--
			if(null==timex && null==interval){
				
				if(null!=stPerson && stPerson.getEnd()==docOffset){
					outText += "</E>";
					stPerson = null;
				}
				if(null!=stOrganization && stOrganization.getEnd()==docOffset){
					outText += "</E>";
					stOrganization = null;
				}
				if(null!=stLocation && stLocation.getEnd()==docOffset){
					outText += "</E>";
					stLocation = null;
				}
				if(null!=stMisc && stMisc.getEnd()==docOffset){
					outText += "</E>";
					stMisc = null;
				}
			}			
			//(3)
			if(null!=sentence && sentence.getEnd()==docOffset){
				outText += "</SE>";
				sentence = null;
			}
			
			/**
			 *  grab a new interval/timex if this offset marks the beginning of one
			 */
			//(1)
			if(interval == null && intervals.containsKey(docOffset))
				interval = intervals.get(docOffset);
			if(timex == null && forwardTimexes.containsKey(docOffset) && !timexesToSkip.contains(forwardTimexes.get(docOffset)))
				timex = forwardTimexes.get(docOffset);
			//(2)
			if(null==timex && null==interval){
				if(stPerson==null && stPersonMap.containsKey(docOffset)){
					stPerson = stPersonMap.get(docOffset);
				}
				if(stOrganization==null && stOrgMap.containsKey(docOffset)){
					stOrganization = stOrgMap.get(docOffset);
				}
				if(stLocation==null && stLocMap.containsKey(docOffset)){
					stLocation = stLocMap.get(docOffset);				
				}
				if(stMisc==null && stMiscMap.containsKey(docOffset)){
					stMisc = stMiscMap.get(docOffset);
				}
			}
			//(3)
			if(null==sentence && sentenceMap.containsKey(docOffset)){
				sentence = sentenceMap.get(docOffset);
			}
			/**
			 *  if an interval/timex begin here, append the opening tag. interval first, timex afterwards
			 */
			//(1)
			if(null!=sentence && sentence.getBegin()==docOffset){
				outText += "<SE>";
			}
			// handle interval openings first
			if(interval != null && interval.getBegin() == docOffset) {
				String intervalTag = "<TIMEX3INTERVAL";
				if (!interval.getTimexValueEB().equals(""))
					intervalTag += " earliestBegin=\"" + interval.getTimexValueEB() + "\"";
				if (!interval.getTimexValueLB().equals(""))
					intervalTag += " latestBegin=\"" + interval.getTimexValueLB() + "\"";
				if (!interval.getTimexValueEE().equals(""))
					intervalTag += " earliestEnd=\"" + interval.getTimexValueEE() + "\"";
				if (!interval.getTimexValueLE().equals(""))
					intervalTag += " latestEnd=\"" + interval.getTimexValueLE() + "\"";
				intervalTag += ">";
				outText += intervalTag;
			}
			// handle timex openings after that
			if(timex != null && timex.getBegin() == docOffset) {
				String timexTag = "<TIMEX3";
				if (!timex.getTimexId().equals(""))
					timexTag += " tid=\"" + timex.getTimexId() + "\"";
				if (!timex.getTimexType().equals(""))
					timexTag += " type=\"" + timex.getTimexType() + "\"";
				if (!timex.getTimexValue().equals(""))
					timexTag += " value=\"" + timex.getTimexValue() + "\"";
				if (!timex.getTimexQuant().equals(""))
					timexTag += " quant=\"" + timex.getTimexQuant() + "\"";
				if (!timex.getTimexFreq().equals(""))
					timexTag += " freq=\"" + timex.getTimexFreq() + "\"";
				if (!timex.getTimexMod().equals(""))
					timexTag += " mod=\"" + timex.getTimexMod() + "\"";
				timexTag += ">";
				outText += timexTag;
			}
			
			if(null==timex && null==interval){
				if(stPerson!=null || stOrganization!=null
						|| stLocation!=null || stMisc!=null){
					
					if(stPerson!=null && stPerson.getBegin()==docOffset){
						String eTag = "<E type=\"PER\">";
						outText += eTag;
					}
					if(stOrganization!=null && stOrganization.getBegin()==docOffset){
						String eTag = "<E type=\"ORG\">";
						outText += eTag;
					}
					if(stLocation!=null && stLocation.getBegin()==docOffset){
						String eTag = "<E type=\"GPE\">";	
						outText += eTag;
					}
					if(stMisc!=null && stMisc.getBegin()==docOffset){
						String eTag = "<E type=\"MISC\">";
						outText += eTag;
					}					
				}				
			}
			
			/**
			 * append the current character
			 */
			if(docOffset + 1 <= documentText.length())
				outText += documentText.substring(docOffset, docOffset + 1);
		}
		
		
		
		// Add TimeML start and end tags		
		outText += "</text>";
		
		outText = "<?xml version=\"1.0\"?>\n<!DOCTYPE TimeML SYSTEM \"TimeML.dtd\">\n<TimeML>\n" + outText + "\n</TimeML>\n";
		
		return outText;
	}
	//
	public static String temFormat(JCas jcas) throws Exception {
		
		final String documentText = jcas.getDocumentText();
		
		// get the timex3 intervals, do some pre-selection on them
		FSIterator iterIntervals = jcas.getAnnotationIndex(Timex3Interval.type).iterator();
		TreeMap<Integer, Timex3Interval> intervals = new TreeMap<Integer, Timex3Interval>();
		while(iterIntervals.hasNext()) {
			Timex3Interval t = (Timex3Interval) iterIntervals.next();
			
			// disregard intervals that likely aren't a real interval, but just a timex-translation
			if(t.getTimexValueEB().equals(t.getTimexValueLB()) && t.getTimexValueEE().equals(t.getTimexValueLE()))
				continue;
			
			if(intervals.containsKey(t.getBegin())) {
				Timex3Interval tInt = intervals.get(t.getBegin());
				
				// always get the "larger" intervals
				if(t.getEnd() - t.getBegin() > tInt.getEnd() - tInt.getBegin()) {
					intervals.put(t.getBegin(), t);
				}
			} else {
				intervals.put(t.getBegin(), t);
			}
		}

		/* 
		 * loop through the timexes to create two treemaps:
		 * - one containing startingposition=>timex tuples for eradication of overlapping timexes
		 * - one containing endposition=>timex tuples for assembly of the XML file
		 */
		FSIterator iterTimex = jcas.getAnnotationIndex(Timex3.type).iterator();
		TreeMap<Integer, Timex3> forwardTimexes = new TreeMap<Integer, Timex3>(),
				backwardTimexes = new TreeMap<Integer, Timex3>();
		while(iterTimex.hasNext()) {
			Timex3 t = (Timex3) iterTimex.next();
			forwardTimexes.put(t.getBegin(), t);
			backwardTimexes.put(t.getEnd(), t);
		}
		
		HashSet<Timex3> timexesToSkip = new HashSet<Timex3>();
		Timex3 prevT = null;
		Timex3 thisT = null;
		// iterate over timexes to find overlaps
		for(Integer begin : forwardTimexes.navigableKeySet()) {
			thisT = (Timex3) forwardTimexes.get(begin);
			
			// check for whether this and the previous timex overlap. ex: [early (friday] morning)
			if(prevT != null && prevT.getEnd() > thisT.getBegin()) {
				
				Timex3 removedT = null; // only for debug message
				// assuming longer value string means better granularity
				if(prevT.getTimexValue().length() > thisT.getTimexValue().length()) {
					timexesToSkip.add(thisT);
					removedT = thisT;
					/* prevT stays the same. */
				} else {
					timexesToSkip.add(prevT);
					removedT = prevT;
					prevT = thisT; // this iteration's prevT was removed; setting for new iteration 
				}
				
				// ask user to let us know about possibly incomplete rules
				Logger l = Logger.getLogger("TimeMLResultFormatter");
				l.log(Level.WARNING, "Two overlapping Timexes have been discovered:" + System.getProperty("line.separator")
						+ "Timex A: " + prevT.getCoveredText() + " [\"" + prevT.getTimexValue() + "\" / " + prevT.getBegin() + ":" + prevT.getEnd() + "]" 
						+ System.getProperty("line.separator")
						+ "Timex B: " + removedT.getCoveredText() + " [\"" + removedT.getTimexValue() + "\" / " + removedT.getBegin() + ":" + removedT.getEnd() + "]" 
						+ " [removed]" + System.getProperty("line.separator")
						+ "The writer chose, for granularity: " + prevT.getCoveredText() + System.getProperty("line.separator")
						+ "This usually happens with an incomplete ruleset. Please consider adding "
						+ "a new rule that covers the entire expression.");
			} else { // no overlap found? set current timex as next iteration's previous timex
				prevT = thisT;
			}
		}
		
		//--
		FSIterator iterSen = jcas.getAnnotationIndex(Sentence.type).iterator();
		TreeMap<Integer, Sentence> sentenceMap = new TreeMap<Integer, Sentence>();
		while(iterSen.hasNext()) {
			Sentence sen = (Sentence) iterSen.next();
			sentenceMap.put(sen.getBegin(), sen);
		}
		
		FSIterator iterStPerson = jcas.getAnnotationIndex(Person.type).iterator();
		TreeMap<Integer, Person> stPersonMap = new TreeMap<Integer, Person>();
		while(iterStPerson.hasNext()) {
			Person stPerson = (Person) iterStPerson.next();
			stPersonMap.put(stPerson.getBegin(), stPerson);
		}
		
		FSIterator iterStOrg = jcas.getAnnotationIndex(Organization.type).iterator();
		TreeMap<Integer, Organization> stOrgMap = new TreeMap<Integer, Organization>();
		while(iterStOrg.hasNext()) {
			Organization stOrg = (Organization) iterStOrg.next();
			stOrgMap.put(stOrg.getBegin(), stOrg);
		}
		
		FSIterator iterStLoc = jcas.getAnnotationIndex(Location.type).iterator();
		TreeMap<Integer, Location> stLocMap = new TreeMap<Integer, Location>();
		while(iterStLoc.hasNext()) {
			Location stLoc = (Location) iterStLoc.next();
			stLocMap.put(stLoc.getBegin(), stLoc);
		}
		
		FSIterator iterStGpe = jcas.getAnnotationIndex(GPE.type).iterator();
		TreeMap<Integer, GPE> stGpeMap = new TreeMap<Integer, GPE>();
		while(iterStGpe.hasNext()) {
			GPE stGpe = (GPE) iterStGpe.next();
			stGpeMap.put(stGpe.getBegin(), stGpe);
		}
		
		FSIterator iterStMisc = jcas.getAnnotationIndex(Misc.type).iterator();
		TreeMap<Integer, Misc> stMiscMap = new TreeMap<Integer, Misc>();
		while(iterStMisc.hasNext()) {
			Misc stMisc = (Misc) iterStMisc.next();
			stMiscMap.put(stMisc.getBegin(), stMisc);
		}
		//--
		
		String outText = new String();
		
		outText += "<text>";

		// alternative xml creation method
		Timex3Interval interval = null;
		Timex3 timex = null;
		//--
		Sentence sentence = null;
		Person stPerson = null;
		Organization stOrganization = null;
		Location stLocation = null;
		GPE stGpe = null;
		Misc stMisc = null;
		//--
		for(Integer docOffset = 0; docOffset <= documentText.length(); docOffset++) {
			
			//(1)
			/**
			 *  see if we have to finish off old timexes/intervals
			 */
			if(timex != null && timex.getEnd() == docOffset) {
				//outText += "</TIMEX3>";
				outText += "</T>";
				timex = null;
			}
			if(interval != null && interval.getEnd() == docOffset) {
				outText += "</TIMEX3INTERVAL>";
				interval = null;
			}
			//(2)
			//--
			if(null==timex && null==interval){
				
				if(null!=stPerson && stPerson.getEnd()==docOffset){
					outText += "</E>";
					stPerson = null;
				}
				if(null!=stOrganization && stOrganization.getEnd()==docOffset){
					outText += "</E>";
					stOrganization = null;
				}
				if(null!=stLocation && stLocation.getEnd()==docOffset){
					outText += "</E>";
					stLocation = null;
				}
				if(null!=stGpe && stGpe.getEnd()==docOffset){
					outText += "</E>";
					stGpe = null;
				}
				if(null!=stMisc && stMisc.getEnd()==docOffset){
					outText += "</E>";
					stMisc = null;
				}
			}			
			//(3)
			if(null!=sentence && sentence.getEnd()==docOffset){
				outText += "</SE>\n";
				sentence = null;
			}
			
			/**
			 *  grab a new interval/timex if this offset marks the beginning of one
			 */
			//(1)
			if(interval == null && intervals.containsKey(docOffset))
				interval = intervals.get(docOffset);
			if(timex == null && forwardTimexes.containsKey(docOffset) && !timexesToSkip.contains(forwardTimexes.get(docOffset)))
				timex = forwardTimexes.get(docOffset);
			//(2)
			if(null==timex && null==interval){
				if(stPerson==null && stPersonMap.containsKey(docOffset)){
					stPerson = stPersonMap.get(docOffset);
				}
				if(stOrganization==null && stOrgMap.containsKey(docOffset)){
					stOrganization = stOrgMap.get(docOffset);
				}
				if(stLocation==null && stLocMap.containsKey(docOffset)){
					stLocation = stLocMap.get(docOffset);				
				}
				if(stGpe==null && stGpeMap.containsKey(docOffset)){
					stGpe = stGpeMap.get(docOffset);
				}
				if(stMisc==null && stMiscMap.containsKey(docOffset)){
					stMisc = stMiscMap.get(docOffset);
				}
			}
			//(3)
			if(null==sentence && sentenceMap.containsKey(docOffset)){
				sentence = sentenceMap.get(docOffset);
			}
			/**
			 *  if an interval/timex begin here, append the opening tag. interval first, timex afterwards
			 */
			//(1)
			if(null!=sentence && sentence.getBegin()==docOffset){
				outText += "<SE>";
			}
			// handle interval openings first
			if(interval != null && interval.getBegin() == docOffset) {
				String intervalTag = "<TIMEX3INTERVAL";
				if (!interval.getTimexValueEB().equals(""))
					intervalTag += " earliestBegin=\"" + interval.getTimexValueEB() + "\"";
				if (!interval.getTimexValueLB().equals(""))
					intervalTag += " latestBegin=\"" + interval.getTimexValueLB() + "\"";
				if (!interval.getTimexValueEE().equals(""))
					intervalTag += " earliestEnd=\"" + interval.getTimexValueEE() + "\"";
				if (!interval.getTimexValueLE().equals(""))
					intervalTag += " latestEnd=\"" + interval.getTimexValueLE() + "\"";
				intervalTag += ">";
				outText += intervalTag;
			}
			// handle timex openings after that
			if(timex != null && timex.getBegin() == docOffset) {
				//alternative
				//String timexTag = "<TIMEX3";
				String timexTag = "<T";
				
				//alternative
				/*
				if (!timex.getTimexId().equals(""))
					timexTag += " tid=\"" + timex.getTimexId() + "\"";
				*/
				if (!timex.getTimexType().equals(""))
					timexTag += " type=\"" + timex.getTimexType() + "\"";
				
				if (!timex.getTimexValue().equals(""))
					timexTag += " value=\"" + timex.getTimexValue() + "\"";
				
				//alternative
				/*
				if (!timex.getTimexQuant().equals(""))
					timexTag += " quant=\"" + timex.getTimexQuant() + "\"";
				if (!timex.getTimexFreq().equals(""))
					timexTag += " freq=\"" + timex.getTimexFreq() + "\"";
				if (!timex.getTimexMod().equals(""))
					timexTag += " mod=\"" + timex.getTimexMod() + "\"";
				*/
				timexTag += ">";
				outText += timexTag;
			}
			
			if(null==timex && null==interval){
				if(stPerson!=null || stOrganization!=null || stLocation!=null 
						|| stGpe!=null ||stMisc!=null){
					
					if(stPerson!=null && stPerson.getBegin()==docOffset){
						String eTag = "<E type=\""+StanfordCoreNLPWrapper.NER_PER+"\">";
						outText += eTag;
					}
					if(stOrganization!=null && stOrganization.getBegin()==docOffset){
						String eTag = "<E type=\""+StanfordCoreNLPWrapper.NER_ORG+"\">";
						outText += eTag;
					}
					if(stLocation!=null && stLocation.getBegin()==docOffset){
						String eTag = "<E type=\""+StanfordCoreNLPWrapper.NER_LOC+"\">";	
						outText += eTag;
					}
					if(stGpe!=null && stGpe.getBegin()==docOffset){
						String eTag = "<E type=\""+StanfordCoreNLPWrapper.NER_GPE+"\">";	
						outText += eTag;
						//System.out.println(outText);
					}
					if(stMisc!=null && stMisc.getBegin()==docOffset){
						String eTag = "<E type=\""+StanfordCoreNLPWrapper.NER_MISC+"\">";
						outText += eTag;
					}					
				}				
			}
			
			/**
			 * append the current character
			 */
			if(docOffset + 1 <= documentText.length())
				outText += documentText.substring(docOffset, docOffset + 1);
		}
		
		
		
		// Add TimeML start and end tags		
		outText += "</text>\n";
		
		//outText = "<?xml version=\"1.0\"?>\n<!DOCTYPE TimeML SYSTEM \"TimeML.dtd\">\n<TimeML>\n" + outText + "\n</TimeML>\n";
		
		return outText;
	}
	
}
