package alteredu.stanford.nlp.process;

import alteredu.stanford.nlp.ling.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Class AbstractListProcessor
 *
 * @author Teg Grenager
 */
public abstract class AbstractListProcessor<IN,OUT> implements ListProcessor<IN,OUT>, Processor {

  public Document processDocument(Document in) {
    List list = in;
    Document doc = in.blankDocument();
    doc.addAll(process(list));
    return doc;
  }

  /** Process a list of lists of tokens.
   * @param lists a List of objects of type List
   * @return a List of objects of type List, each of which has been processed.
   */
  public List<List<OUT>> processLists(List<List<IN>> lists) {
    List<List<OUT>> result = new ArrayList<List<OUT>>(lists.size());
    for (List<IN> list : lists) {
      List<OUT> outList = process(list);
      result.add(outList);
    }
    return result;
  }

}
