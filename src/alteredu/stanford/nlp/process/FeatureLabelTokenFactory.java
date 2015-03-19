package alteredu.stanford.nlp.process;

import alteredu.stanford.nlp.ling.FeatureLabel;

/**
 * @author Marie-Catherine de Marneffe

 */

public class FeatureLabelTokenFactory implements LexedTokenFactory {
  public Object makeToken(String str, int begin, int length) {
    FeatureLabel fl = new FeatureLabel();
    fl.setWord(str);
    fl.setCurrent(str);
    return fl;
  }
}

