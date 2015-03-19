package alteredu.stanford.nlp.ie;

import alteredu.stanford.nlp.ie.crf.CRFClassifier;
import alteredu.stanford.nlp.util.StringUtils;

import java.io.*;
import java.net.*;
import java.util.Properties;


/*****************************************************************************
 * A named-entity recognizer server for Stanford's NER.
 * Runs on a socket and waits for text to annotate and returns the 
 * annotated text.  (Internally, it uses the <code>testString()</code>
 * method on the default CRFClassifier which is serialized inside the jar
 * file from which it is called.
 * 
 * @version $Id: NERServer.java,v 1.6 2006/09/18 22:06:47 manning Exp $
 * @author
 *      Bjorn Aldag<BR>
 *      Copyright &copy; 2000 - 2004 Cycorp, Inc.  All rights reserved.
 * @author Christopher Manning 2006
 *
*****************************************************************************/

public class NERServer {

  //// Constants
//////////////////////////////////////////////////////////////

  /**
   * Debugging toggle.
   */
  private boolean DEBUG = true;

  /**
   * The listener socket of this server.
   */
  private final ServerSocket LISTENER;

  /**
   * The classifier that does the actual tagging.
   */
  //  private CMMClassifier NER = CMMClassifier.getClassifier("/home.local/tkbuser/ner-2004-06-16/ner.eng2004.gz");
  private final AbstractSequenceClassifier NER ;


  //// Constructors
///////////////////////////////////////////////////////////

  /**
   * Creates a new named entity recognizer server on the specified port.
   * @param port the port this NERServer listens on.
   */
  public NERServer(int port, AbstractSequenceClassifier asc) throws IOException {
    NER = asc;
    LISTENER = new ServerSocket(port);
  }

  //// Public Methods
/////////////////////////////////////////////////////////

  /**
   * Runs this named entity recognizer server.
   */
  public void run() {
    Socket client = null;
    while (true) {
        try {
          client = LISTENER.accept();
          if (DEBUG) {
            System.out.println("Accepted request from " +
client.getInetAddress().getHostName());
          }
          new Session(client);
        }
        catch (Exception e1) {
          System.err.println("NERServer: couldn't accept");
          e1.printStackTrace(System.err);
          try {
            client.close();
          }
          catch (Exception e2) {
            System.err.println("NERServer: couldn't close client");
            e2.printStackTrace(System.err);
          }
        }
    }
  }


  //// Inner Classes
//////////////////////////////////////////////////////////

  /**
   * A single user session, accepting one request, processing it, and
sending 
   * back the results.
   */
  private class Session extends Thread {

  //// Instance Fields
////////////////////////////////////////////////////////      

    /**
     * The socket to the client.
     */
    private Socket client;

    /**
     * The input stream from the client.
     */
    private BufferedReader in;

    /**
     * The output stream to the client.
     */
    private PrintWriter out;


    //// Constructors
///////////////////////////////////////////////////////////

    private Session(Socket socket) throws IOException {
      client = socket;
      in = new BufferedReader(new
InputStreamReader(client.getInputStream()));
      out = new PrintWriter(client.getOutputStream());
      start();
    }


    //// Public Methods
/////////////////////////////////////////////////////////

    /**
     * Runs this session by reading a string, tagging it, and writing
back the 
     * result.
     */
    public void run() {
      if (DEBUG) {System.out.println("Created new session");}
      String input = null;
      try {
        input = in.readLine();
        if (DEBUG) {
          System.out.println("Receiving: \"" + input + "\"");
        }
      }
      catch (IOException e) {
        System.err.println("NERServer:Session: couldn't read input");
        e.printStackTrace(System.err);
      }
      catch (NullPointerException npe) {
        System.err.println("NERServer:Session: connection closed by peer");
        npe.printStackTrace(System.err);
      }
      if (! (input == null)) {
        //String output = NER.testSentence(input);
        String output = NER.testString(input);
        if (DEBUG) {
          System.out.print("Sending: \"" + output + "\"");
        }
        out.print(output);
        out.flush();
      }
      close();
    }

    /**
     * Terminates this session gracefully.
     */
    private void close() {
      try {
        in.close();
        out.close();
        client.close();
      }
      catch (Exception e) {
        System.err.println("NERServer:Session: can't close session");
        e.printStackTrace(System.err);
      }
    }

  }


  private static final String USAGE = "Usage: NERServer [-loadFile file|-loadJarFile resource] portNumber";

  /**
   * Starts this server on the specified port. <p>
   * Usage: <code>java edu.stanford.nlp.ie.NERServer [-loadFile file|-loadJarFile resource] portNumber</code>
   */
  public static void main (String[] args) throws Exception {
    Properties props = StringUtils.argsToProperties(args);
    String loadFile = props.getProperty("loadClassifier");
    String loadJarFile = props.getProperty("loadJarClassifier");
    String portStr = props.getProperty("");
    if (portStr == null || portStr.equals("")) {
      System.err.println(USAGE);
      System.exit(1);
    }

    AbstractSequenceClassifier asc;
    if (loadFile != null && ! loadFile.equals("")) {
      asc = CRFClassifier.getClassifier(loadFile);
    } else if (loadJarFile != null && ! loadJarFile.equals("")) {
      asc = CRFClassifier.getJarClassifier(loadFile, props);
    } else {
      asc = CRFClassifier.getDefaultClassifier();
    }

    int port = 0;
    try {
      port = Integer.parseInt(portStr);
    } catch (NumberFormatException e) {
      System.err.println("Non-numerical port");
      System.err.println(USAGE);
      System.exit(1);
    }
    new NERServer(port, asc).run();
  }

}
