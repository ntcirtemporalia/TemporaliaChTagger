package alteredu.stanford.nlp.process;

import alteredu.stanford.nlp.ling.Word;

/**
 * Constructs a Word from a String. This is the default
 * TokenFactory for PTBLexer. It discards the positional information.
 */
public class WordTokenFactory implements LexedTokenFactory {
  public Object makeToken(String str, int begin, int length) {
    return new Word(str);
  }
}
