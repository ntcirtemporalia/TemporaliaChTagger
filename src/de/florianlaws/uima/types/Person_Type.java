
/* First created by JCasGen Thu Feb 05 11:56:24 GMT 2015 */
package de.florianlaws.uima.types;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;

/** A person annotation
 * Updated by JCasGen Mon Mar 02 16:54:30 GMT 2015
 * @generated */
public class Person_Type extends Annotation_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (Person_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = Person_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new Person(addr, Person_Type.this);
  			   Person_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new Person(addr, Person_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = Person.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.florianlaws.uima.types.Person");
 
  /** @generated */
  final Feature casFeat_partialCombination;
  /** @generated */
  final int     casFeatCode_partialCombination;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getPartialCombination(int addr) {
        if (featOkTst && casFeat_partialCombination == null)
      jcas.throwFeatMissing("partialCombination", "de.florianlaws.uima.types.Person");
    return ll_cas.ll_getIntValue(addr, casFeatCode_partialCombination);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setPartialCombination(int addr, int v) {
        if (featOkTst && casFeat_partialCombination == null)
      jcas.throwFeatMissing("partialCombination", "de.florianlaws.uima.types.Person");
    ll_cas.ll_setIntValue(addr, casFeatCode_partialCombination, v);}
    
  
 
  /** @generated */
  final Feature casFeat_corefChainID;
  /** @generated */
  final int     casFeatCode_corefChainID;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getCorefChainID(int addr) {
        if (featOkTst && casFeat_corefChainID == null)
      jcas.throwFeatMissing("corefChainID", "de.florianlaws.uima.types.Person");
    return ll_cas.ll_getIntValue(addr, casFeatCode_corefChainID);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setCorefChainID(int addr, int v) {
        if (featOkTst && casFeat_corefChainID == null)
      jcas.throwFeatMissing("corefChainID", "de.florianlaws.uima.types.Person");
    ll_cas.ll_setIntValue(addr, casFeatCode_corefChainID, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public Person_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_partialCombination = jcas.getRequiredFeatureDE(casType, "partialCombination", "uima.cas.Integer", featOkTst);
    casFeatCode_partialCombination  = (null == casFeat_partialCombination) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_partialCombination).getCode();

 
    casFeat_corefChainID = jcas.getRequiredFeatureDE(casType, "corefChainID", "uima.cas.Integer", featOkTst);
    casFeatCode_corefChainID  = (null == casFeat_corefChainID) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_corefChainID).getCode();

  }
}



    