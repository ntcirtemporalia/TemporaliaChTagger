

/* First created by JCasGen Thu Feb 05 11:56:24 GMT 2015 */
package de.florianlaws.uima.types;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** A person annotation
 * Updated by JCasGen Mon Mar 02 16:54:30 GMT 2015
 * XML source: /Users/dryuhaitao/WorkBench/JavaBench/HeidelTimeKit/desc/type/NERDATypes.xml
 * @generated */
public class Person extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Person.class);
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int type = typeIndexID;
  /** @generated
   * @return index of the type  
   */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected Person() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Person(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Person(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Person(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }   

  /** 
   * <!-- begin-user-doc -->
   * Write your own initialization here
   * <!-- end-user-doc -->
   *
   * @generated modifiable 
   */
  private void readObject() {/*default - does nothing empty block */}
     
 
    
  //*--------------*
  //* Feature: partialCombination

  /** getter for partialCombination - gets 
   * @generated
   * @return value of the feature 
   */
  public int getPartialCombination() {
    if (Person_Type.featOkTst && ((Person_Type)jcasType).casFeat_partialCombination == null)
      jcasType.jcas.throwFeatMissing("partialCombination", "de.florianlaws.uima.types.Person");
    return jcasType.ll_cas.ll_getIntValue(addr, ((Person_Type)jcasType).casFeatCode_partialCombination);}
    
  /** setter for partialCombination - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setPartialCombination(int v) {
    if (Person_Type.featOkTst && ((Person_Type)jcasType).casFeat_partialCombination == null)
      jcasType.jcas.throwFeatMissing("partialCombination", "de.florianlaws.uima.types.Person");
    jcasType.ll_cas.ll_setIntValue(addr, ((Person_Type)jcasType).casFeatCode_partialCombination, v);}    
   
    
  //*--------------*
  //* Feature: corefChainID

  /** getter for corefChainID - gets 
   * @generated
   * @return value of the feature 
   */
  public int getCorefChainID() {
    if (Person_Type.featOkTst && ((Person_Type)jcasType).casFeat_corefChainID == null)
      jcasType.jcas.throwFeatMissing("corefChainID", "de.florianlaws.uima.types.Person");
    return jcasType.ll_cas.ll_getIntValue(addr, ((Person_Type)jcasType).casFeatCode_corefChainID);}
    
  /** setter for corefChainID - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setCorefChainID(int v) {
    if (Person_Type.featOkTst && ((Person_Type)jcasType).casFeat_corefChainID == null)
      jcasType.jcas.throwFeatMissing("corefChainID", "de.florianlaws.uima.types.Person");
    jcasType.ll_cas.ll_setIntValue(addr, ((Person_Type)jcasType).casFeatCode_corefChainID, v);}    
  }

    