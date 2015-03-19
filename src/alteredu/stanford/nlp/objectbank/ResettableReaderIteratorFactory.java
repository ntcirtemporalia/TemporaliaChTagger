package alteredu.stanford.nlp.objectbank;

import alteredu.stanford.nlp.util.StringUtils;

import java.util.Iterator;
import java.util.Collection;
import java.util.ArrayList;
import java.io.Reader;
import java.io.File;

/**
 * Vends ReaderIterators which can alway be rewound.
 * Java's Readers cannot be reset, but this ReaderIteratorFactory allows resetting.
 * It the input types are anything other than Readers, then it resets them in
 * the obvious way.  If the input is a Reader, then it's contents are saved
 * to a tmp file (which is destroyed when the VM exits) which is then resettable.
 *
 * @author Jenny Finkel
 */
public class ResettableReaderIteratorFactory extends ReaderIteratorFactory {


  /**
   * Constructs a ResettableReaderIteratorFactory from the input sources
   * contained in the Collection.  The Collection should contain
   * Objects of type File, String, URL and Reader.  See class
   * description for details.
   *
   * @param c Collection of input sources.
   */
  public ResettableReaderIteratorFactory(Collection c) {
    super(c);
  }

  /**
   * Convenience constructor to construct a ResettableReaderIteratorFactory
   * from a single input source. The Object should be of type File,
   * String, URL and Reader.  See class
   * description for details.
   *
   * @param o an input source that can be converted into a Reader
   */
  public ResettableReaderIteratorFactory(Object o) {
    super(o);
  }

  /**
   * Constructs a ResettableReaderIteratorFactory with no initial
   * input sources.
   */
  public ResettableReaderIteratorFactory() {
    super();
  }
  
  /**
   * Returns an Iterator over the input sources in the underlying Collection.
   *
   * @return an Iterator over the input sources in the underlying Collection.
   */
  public Iterator<Reader> iterator() {
    Collection newCollection = new ArrayList();
    for (Object o : c) {
      if (o instanceof Reader) {
        String name = o.toString()+".tmp";
        File tmpFile;
        try {
          tmpFile = File.createTempFile(name,"");
        } catch (Exception e) {
          throw new RuntimeException(e.getMessage());
        }
        tmpFile.deleteOnExit();
        StringUtils.printToFile(tmpFile, StringUtils.slurpReader((Reader)o));
        newCollection.add(tmpFile);
      } else {
        newCollection.add(o);
      }
    }
    c = newCollection;
    return new ReaderIterator();
  }
    
}
