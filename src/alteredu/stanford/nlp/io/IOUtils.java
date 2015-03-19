package alteredu.stanford.nlp.io;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Helper Class for storing serialized objects to disk.
 * 
 * @author Kayur Patel, Teg Grenager
 */

public class IOUtils {

  // A class of static methods
  private IOUtils() {
  }

  /**
   * Write object to a file with the specified name.
   *
   * @param o object to be written to file
   * @param filename name of the temp file
   *
   * @return File containing the object
   */
  public static File writeObjectToFile(Serializable o, String filename) throws IOException{
    File file = new File(filename);
    // file.createNewFile(); // cdm may 2005: does nothing needed
    ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(file))));
    oos.writeObject(o);
    oos.close();
    return file;
  }

  /**
   * Write object to a file with the specified name.
   *
   * @param o object to be written to file
   * @param filename name of the temp file
   *
   * @return File containing the object, or null if an exception was caught
   */
  public static File writeObjectToFileNoExceptions(Serializable o, String filename) {
    File file = null;
    ObjectOutputStream oos = null;
    try {
      file = new File(filename);
      // file.createNewFile(); // cdm may 2005: does nothing needed
      oos = new ObjectOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(file))));
      oos.writeObject(o);
      oos.close();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (oos != null) {
        try {
          oos.close();
        } catch (Exception ioe) {
        }
      }
    }
    return file;
  }

  /**
   * Write object to temp file which is destroyed when the program exits.
   * 
   * @param o object to be written to file
   * @param filename name of the temp file
   * @return File containing the object
   */
  public static File writeObjectToTempFile(Serializable o, String filename) throws IOException{
    File file = File.createTempFile(filename, ".tmp");
    file.deleteOnExit();
    ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(file))));
    oos.writeObject(o);
    oos.close();
    return file;
  }

  /**
   * Write object to a temp file and ignore exceptions.
   * 
   * @param o object to be written to file
   * @param filename name of the temp file
   * @return File containing the object
   */
  public static File writeObjectToTempFileNoExceptions(Serializable o, String filename) {
    try {
      return writeObjectToTempFile(o, filename);
    } catch (Exception e) {
      System.err.println("Error writing object to file " + filename);
      e.printStackTrace();
      return null;
    }
  }


  /**
   * Read an object from a stored file.
   *
   * @param file the file pointing to the object to be retrived
   * @return the object read from the file.
   */
  public static Object readObjectFromFile(File file) throws IOException, ClassNotFoundException {
    ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(file))));
    Object o = ois.readObject();
    ois.close();
    return o;
  }

  /**
   * Read an object from a stored file.
   *
   * @param filename The filename of the object to be retrived
   * @return The object read from the file.
   */
  public static Object readObjectFromFile(String filename) throws IOException, ClassNotFoundException {
    return readObjectFromFile(new File(filename));
  }


  /**
   * Read an object from a stored file without throwing exceptions.
   *
   * @param file the file pointing to the object to be retrived
   * @return the object read from the file, or null if an exception occurred.
   */
  public static Object readObjectFromFileNoExceptions(File file) {
    Object o = null;
    try {
      ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(file))));
      o = ois.readObject();
      ois.close();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
    return o;
  }

  public static int lineCount(File textFile) throws IOException {
    BufferedReader r = new BufferedReader(new FileReader(textFile));
    int numLines = 0;
    while (r.readLine()!=null) {
      numLines++;
    }
    return numLines;
  }
}
