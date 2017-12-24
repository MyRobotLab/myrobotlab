package org.myrobotlab.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

/**
 * Client HTTP Request class This class helps to send POST HTTP requests with
 * various form data, including files. Cookies can be added to be included in
 * the request.
 * 
 * @author Vlad Patryshev
 * @version 1.0
 * 
 *          Modified by grog - added buffered output to increase performance in
 *          larger POSTs. In file operations buffered output is 10x faster.
 *          Currently, a POST of a large file takes ~6 seconds TODO - fill in
 *          details and result
 * 
 *          References:
 *          http://www.javabeat.net/tips/36-file-upload-and-download-
 *          using-java.html http://www.java2s.com/Code/Java/File-Input-Output/
 *          ComparingBufferedandUnbufferedWritingPerformance.htm
 * 
 *          The big beautiful kahuna from stack overflow
 *          http://stackoverflow.com
 *          /questions/2793150/how-to-use-java-net-urlconnection
 *          -to-fire-and-handle-http-requests Proxy info -
 *          http://edn.embarcadero.com/article/29783 Proxy info -
 *          http://docs.oracle
 *          .com/javase/6/docs/technotes/guides/net/proxies.html Proxy info -
 *          http
 *          ://stackoverflow.com/questions/120797/how-do-i-set-the-proxy-to-be
 *          -used-by-the-jvm
 * 
 */

public class HttpRequest {
  public final static Logger log = LoggerFactory.getLogger(HttpRequest.class);

  URLConnection connection;

  OutputStream osstr = null;
  BufferedOutputStream os = null;
  Map<String, String> cookies = new HashMap<String, String>();

  String boundary = "---------------------------";

  String error = null;

  public static void main(String[] args) throws Exception {

    Logging logging = LoggingFactory.getInstance();
    logging.configure();
    LoggingFactory.getInstance().setLevel(Level.DEBUG);

    // HTTPRequest http;
    // http = new HTTPRequest("http://www.google.com");
    // String s = http.getString();

    HttpRequest.postFile("http://myrobotlab.org/myrobotlab_log/postLogFile.php", "GroG", "file", new File(LoggingFactory.getLogFileName()));

    /*
     * 
     * HTTPRequest http = new HTTPRequest(
     * "http://www.mkyong.com/java/how-do-convert-byte-array-to-string-in-java/"
     * ); String s = http.getString(); log.info(s);
     * 
     * String language = "en"; String toSpeak = "hello"; URI uri = new
     * URI("http", null, "translate.google.com", 80, "/translate_tts", "tl=" +
     * language + "&q=" + toSpeak, null);
     * 
     * URL url = uri.toURL();
     * 
     * HttpURLConnection.setFollowRedirects(true); HttpURLConnection connection
     * = (HttpURLConnection) url.openConnection(); System.out.println(
     * "Response code = " + connection.getResponseCode()); String header =
     * connection.getHeaderField("location"); if (header != null)
     * System.out.println("Redirected to " + header);
     * 
     * HTTPRequest request = new HTTPRequest(uri.toURL()); request.getBinary();
     */
  }

  /*
   * Graciously lifted from - http://stackoverflow.com/questions/2793150/how-to
   * -use-java-net-urlconnection-to-fire-and-handle-http-requests
   */
  static public String postFile(String url, String userid, String fieldName, File textFile) throws Exception {
    // String param = "value";
    // File textFile = new File("/path/to/file.txt");
    // File binaryFile = new File("/path/to/file.bin");
    String boundary = Long.toHexString(System.currentTimeMillis()); // Just
    // generate
    // some
    // unique
    // random
    // value.
    String CRLF = "\r\n"; // Line separator required by multipart/form-data.

    URLConnection connection = new URL(url).openConnection();
    connection.setDoOutput(true);
    connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
    PrintWriter writer = null;
    try {
      String charset = "ISO-8859-1"; // was ISO-8859-1
      OutputStream output = connection.getOutputStream();
      writer = new PrintWriter(new OutputStreamWriter(output, charset), true); // true
      // =
      // autoFlush,
      // important!

      // Send text file.
      writer.append("--" + boundary).append(CRLF);
      writer.append("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + textFile.getName() + "\"").append(CRLF);
      writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
      writer.append(CRLF).flush();
      BufferedReader reader = null;
      try {
        reader = new BufferedReader(new InputStreamReader(new FileInputStream(textFile), charset));
        for (String line; (line = reader.readLine()) != null;) {
          writer.append(line).append(CRLF);
        }
      } finally {
        if (reader != null)
          try {
            reader.close();
          } catch (IOException logOrIgnore) {
          }
      }

      // Send normal param.

      writer.append("--" + boundary).append(CRLF);
      writer.append("Content-Disposition: form-data; name=\"user\"").append(CRLF);
      writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
      writer.append(CRLF);
      writer.append(userid).append(CRLF).flush();

      writer.flush();

      /*
       * // Send binary file. writer.append("--" + boundary).append(CRLF);
       * writer.append(
       * "Content-Disposition: form-data; name=\"binaryFile\"; filename=\"" +
       * binaryFile.getName() + "\"").append(CRLF); writer.append(
       * "Content-Type: " + URLConnection.guessContentTypeFromName
       * (binaryFile.getName())).append(CRLF); writer.append(
       * "Content-Transfer-Encoding: binary").append(CRLF);
       * writer.append(CRLF).flush(); InputStream input = null; try { input =
       * new FileInputStream(binaryFile); byte[] buffer = new byte[1024]; for
       * (int length = 0; (length = input.read(buffer)) > 0;) {
       * output.write(buffer, 0, length); } output.flush(); // Important! Output
       * cannot be closed. Close of writer will close output as well. } finally
       * { if (input != null) try { input.close(); } catch (IOException
       * logOrIgnore) {} } writer.append(CRLF).flush(); // CRLF is important! It
       * indicates end of binary boundary.
       */

      // End of multipart/form-data.
      writer.append("--" + boundary + "--").append(CRLF);
      writer.append(CRLF).flush(); // CRLF is important! It indicates end
      // of binary boundary.

      byte[] data;
      InputStream in = null;
      in = new BufferedInputStream(connection.getInputStream());
      ByteArrayOutputStream bos = new ByteArrayOutputStream(16384);
      try {

        int BUFFER_SIZE = 16384;
        byte[] tmp = new byte[BUFFER_SIZE];
        int ret;
        while ((ret = in.read(tmp)) > 0) {
          bos.write(tmp, 0, ret);
        }
      } catch (IOException e) {
        Logging.logError(e);
      }

      data = bos.toByteArray();
      log.info(String.format("read %d bytes", data.length));

      String s = new String(data);
      log.info(s);
      try {
        in.close();
      } catch (IOException e) {
        // don't care
      }

      return s;

    } finally {
      if (writer != null)
        writer.close();
    }
  }

  /**
   * Creates a new multipart POST HTTP request for a specified URL string
   * 
   * @param urlString
   *          the string representation of the URL to send request to
   * @throws IOException e
   */
  public HttpRequest(String urlString) throws IOException {
    this(new URL(urlString));
  }

  /**
   * Creates a new multipart POST HTTP request for a specified URL
   * 
   * @param url
   *          the URL to send request to
   * @throws IOException e
   */
  public HttpRequest(URL url) throws IOException {
    this(url.openConnection());
  }

  /**
   * Creates a new multipart POST HTTP request on a freshly opened URLConnection
   * 
   * @param connection
   *          an already open URL connection
   * @throws IOException e
   */
  public HttpRequest(URLConnection connection) throws IOException {
    log.info("http request for " + connection.getURL());
    this.connection = connection;
    connection.setDoOutput(true);
  }

  private void boundary() throws IOException {
    write("--");
    write(boundary);
  }

  protected void connect() throws IOException {
    if (os == null)
      os = new BufferedOutputStream(connection.getOutputStream());
  }

  public byte[] getBinary() throws IOException {
    // URL u = new URL("http://www.java2s.com/binary.dat");
    // URLConnection uc = url.openConnection();

    String contentType = null;
    int contentLength = -1;

    // a little weird - this will throw NoSuchElementException
    // if nothing was recieved
    contentType = connection.getContentType();
    contentLength = connection.getContentLength();

    log.info(String.format("contentType %s contentLength %d", contentType, contentLength));

    InputStream raw;
    byte[] data = null;
    int initSize = (contentLength == -1) ? 65536 : contentLength;
    ByteArrayOutputStream bos = new ByteArrayOutputStream(initSize);
    InputStream in = null;
    // try {
    raw = connection.getInputStream();
    in = new BufferedInputStream(raw);

    byte[] tmp = new byte[initSize];
    int ret;
    while ((ret = in.read(tmp)) > 0) {
      log.debug(String.format("read %d bytes", ret));
      bos.write(tmp, 0, ret);
    }
    /*
     * } catch (IOException e) { Logging.logException(e); }
     */

    data = bos.toByteArray();
    log.info(String.format("read %d bytes", data.length));

    try {
      if (in != null) {
        in.close();
      }
    } catch (IOException e) {
      // don't care
    }

    /*
     * 
     * try { // content size sent back data = new byte[contentLength]; int
     * bytesRead = 0; int offset = 0; while (offset < contentLength) { bytesRead
     * = in.read(data, offset, data.length - offset); if (bytesRead == -1)
     * break; offset += bytesRead; } in.close();
     * 
     * if (offset != contentLength) { throw new IOException("Only read " +
     * offset + " bytes; Expected " + contentLength + " bytes"); } } catch
     * (IOException e1) { Logging.logException(e1); error = e1.getMessage(); } }
     */

    /*
     * String filename = u.getFile().substring(filename.lastIndexOf('/') + 1);
     */

    /*
     * FileOutputStream out; try { out = new FileOutputStream("hello.mp3");
     * out.write(data); out.flush(); out.close(); } catch (Exception e) { //
     * TODO Auto-generated catch block e.printStackTrace(); }
     */

    return data;
  }

  public String getError() {
    return error;
  }

  public String getString() throws IOException {
    byte[] b = getBinary();
    if (b != null) {
      return new String(b);
    }

    return null;
  }

  public boolean hasError() {
    return error != null;
  }

  protected void newline() throws IOException {
    connect();
    write("\r\n");
  }

  private void pipe(InputStream in, OutputStream out) throws IOException {
    byte[] buf = new byte[500000];
    int nread;
    // int navailable;
    // int total = 0;
    synchronized (in) {
      while ((nread = in.read(buf, 0, buf.length)) >= 0) {
        out.write(buf, 0, nread);
        // total += nread;
      }
    }
    out.flush();
    in.close();
    buf = null;
  }

  /**
   * posts the requests to the server, with all the cookies and parameters that
   * were added
   * 
   * @return input stream with the server response
   */
  public InputStream post() {
    try {
      boundary();
      writeln("--");
      os.close();
      return connection.getInputStream();
    } catch (IOException e) {
      // TODO Auto-generated catch block
    }
    return null;
  }

  /**
   * posts the requests to the server, with all the cookies and parameters that
   * were added before (if any), and with parameters that are passed in the
   * argument
   * 
   * @param parameters
   *          request parameters
   * @throws IOException e
   * @return input stream with the server response
   * @see #setParameters
   */
  public InputStream post(Map<String, String> parameters) throws IOException {
    setParameters(parameters);
    return post();
  }

  /**
   * posts the requests to the server, with all the cookies and parameters that
   * were added before (if any), and with cookies and parameters that are passed
   * in the arguments
   * 
   * @param cookies
   *          request cookies
   * @param parameters
   *          request parameters
   * @return input stream with the server response
   * @throws IOException e
   * @see #setParameters
   */
  public InputStream post(Map<String, String> cookies, Map<String, String> parameters) throws IOException {
    setCookies(cookies);
    setParameters(parameters);
    return post();
  }

  /**
   * posts the requests to the server, with all the cookies and parameters that
   * were added before (if any), and with parameters that are passed in the
   * argument
   * 
   * @param parameters
   *          request parameters
   * @return input stream with the server response
   * @throws IOException e
   * @see #setParameters
   */
  public InputStream post(Object[] parameters) throws IOException {
    setParameters(parameters);
    return post();
  }

  /**
   * post the POST request to the server, with the specified parameter
   * 
   * @param name
   *          parameter name
   * @param value
   *          parameter value
   * @return input stream with the server response
   * @throws IOException io exception
   * @see #setParameter
   */
  public InputStream post(String name, Object value) throws IOException {
    setParameter(name, value);
    return post();
  }

  /**
   * post the POST request to the server, with the specified parameters
   * 
   * @param name1
   *          first parameter name
   * @param value1
   *          first parameter value
   * @param name2
   *          second parameter name
   * @param value2
   *          second parameter value
   * @return input stream with the server response
   * @throws IOException e
   * @see #setParameter
   */
  public InputStream post(String name1, Object value1, String name2, Object value2) throws IOException {
    setParameter(name1, value1);
    return post(name2, value2);
  }

  /**
   * post the POST request to the server, with the specified parameters
   * 
   * @param name1
   *          first parameter name
   * @param value1
   *          first parameter value
   * @param name2
   *          second parameter name
   * @param value2
   *          second parameter value
   * @param name3
   *          third parameter name
   * @param value3
   *          third parameter value
   * @return input stream with the server response
   * @throws IOException e
   * @see #setParameter
   */
  public InputStream post(String name1, Object value1, String name2, Object value2, String name3, Object value3) throws IOException {
    setParameter(name1, value1);
    return post(name2, value2, name3, value3);
  }

  /**
   * post the POST request to the server, with the specified parameters
   * 
   * @param name1
   *          first parameter name
   * @param value1
   *          first parameter value
   * @param name2
   *          second parameter name
   * @param value2
   *          second parameter value
   * @param name3
   *          third parameter name
   * @param value3
   *          third parameter value
   * @param name4
   *          fourth parameter name
   * @param value4
   *          fourth parameter value
   * @return input stream with the server response
   * @throws IOException e
   * @see #setParameter
   */
  public InputStream post(String name1, Object value1, String name2, Object value2, String name3, Object value3, String name4, Object value4) throws IOException {
    setParameter(name1, value1);
    return post(name2, value2, name3, value3, name4, value4);
  }

  /**
   * posts the requests to the server, with all the cookies and parameters that
   * were added before (if any), and with cookies and parameters that are passed
   * in the arguments
   * 
   * @param cookies
   *          request cookies
   * @param parameters
   *          request parameters
   * @return input stream with the server response
   * @throws IOException e
   * @see #setParameters
   * see setCookies
   */
  public InputStream post(String[] cookies, Object[] parameters) throws IOException {
    setCookies(cookies);
    setParameters(parameters);
    return post();
  }

  /**
   * posts a new request to specified URL, with parameters that are passed in
   * the argument
   * @param url u
   * @param parameters
   *          request parameters
   * @return input stream with the server response
   * @throws IOException e
   * @see #setParameters
   * 
   */
  public InputStream post(URL url, Map<String, String> parameters) throws IOException {
    return new HttpRequest(url).post(parameters);
  }

  /**
   * posts a new request to specified URL, with cookies and parameters that are
   * passed in the argument
   * 
   * @param url u
   * @param cookies
   *          request cookies
   * @param parameters
   *          request parameters
   * @return input stream with the server response
   * @throws IOException e
   * @see #setCookies
   * @see #setParameters
   */
  public InputStream post(URL url, Map<String, String> cookies, Map<String, String> parameters) throws IOException {
    return new HttpRequest(url).post(cookies, parameters);
  }

  /**
   * posts a new request to specified URL, with parameters that are passed in
   * the argument
   * 
   * @param url u
   * @param parameters
   *          request parameters
   * @return input stream with the server response
   * @throws IOException e
   * @see #setParameters
   */
  public InputStream post(URL url, Object[] parameters) throws IOException {
    return new HttpRequest(url).post(parameters);
  }

  /**
   * post the POST request specified URL, with the specified parameter
   * @param url u
   * 
   * @param name1 
   *          parameter name
   * @param value1
   *          parameter value
   * @return input stream with the server response
   * @throws IOException e
   * @see #setParameter
   */
  public InputStream post(URL url, String name1, Object value1) throws IOException {
    return new HttpRequest(url).post(name1, value1);
  }

  /**
   * post the POST request to specified URL, with the specified parameters
   * @param url u
   * 
   * @param name1
   *          first parameter name
   * @param value1
   *          first parameter value
   * @param name2
   *          second parameter name
   * @param value2
   *          second parameter value
   * @return input stream with the server response
   * @throws IOException e
   * @see #setParameter
   */
  public InputStream post(URL url, String name1, Object value1, String name2, Object value2) throws IOException {
    return new HttpRequest(url).post(name1, value1, name2, value2);
  }

  /**
   * post the POST request to specified URL, with the specified parameters
   * @param url u
   * 
   * @param name1
   *          first parameter name
   * @param value1
   *          first parameter value
   * @param name2
   *          second parameter name
   * @param value2
   *          second parameter value
   * @param name3
   *          third parameter name
   * @param value3
   *          third parameter value
   * @return input stream with the server response
   * @throws IOException e
   * @see #setParameter
   */
  public InputStream post(URL url, String name1, Object value1, String name2, Object value2, String name3, Object value3) throws IOException {
    return new HttpRequest(url).post(name1, value1, name2, value2, name3, value3);
  }

  /**
   * post the POST request to specified URL, with the specified parameters
   * @param url u
   * 
   * @param name1
   *          first parameter name
   * @param value1
   *          first parameter value
   * @param name2
   *          second parameter name
   * @param value2
   *          second parameter value
   * @param name3
   *          third parameter name
   * @param value3
   *          third parameter value
   * @param name4
   *          fourth parameter name
   * @param value4
   *          fourth parameter value
   * @return input stream with the server response
   * @throws IOException e
   * @see #setParameter
   */
  public InputStream post(URL url, String name1, Object value1, String name2, Object value2, String name3, Object value3, String name4, Object value4) throws IOException {
    return new HttpRequest(url).post(name1, value1, name2, value2, name3, value3, name4, value4);
  }

  /**
   * posts a new request to specified URL, with cookies and parameters that are
   * passed in the argument
   * @param url u
   * 
   * @param cookies
   *          request cookies
   * @param parameters
   *          request parameters
   * @return input stream with the server response
   * @throws IOException e
   * @see #setCookies
   * @see #setParameters
   */
  public InputStream post(URL url, String[] cookies, Object[] parameters) throws IOException {
    return new HttpRequest(url).post(cookies, parameters);
  }

  public void postCookies() {
    StringBuffer cookieList = new StringBuffer();

    for (Iterator<Entry<String, String>> i = cookies.entrySet().iterator(); i.hasNext();) {
      Entry<String, String> entry = (i.next());
      cookieList.append(entry.getKey().toString() + "=" + entry.getValue());

      if (i.hasNext()) {
        cookieList.append("; ");
      }
    }
    if (cookieList.length() > 0) {
      connection.setRequestProperty("Cookie", cookieList.toString());
    }
  }

  /**
   * adds a cookie to the requst
   * 
   * @param name
   *          cookie name
   * @param value
   *          cookie value
   * @throws IOException e
   */
  public void setCookie(String name, String value) throws IOException {
    cookies.put(name, value);
  }

  /**
   * adds cookies to the request
   * 
   * @param cookies
   *          the cookie "name-to-value" map
   * @throws IOException e
   */
  public void setCookies(Map<String, String> cookies) throws IOException {
    if (cookies == null)
      return;
    this.cookies.putAll(cookies);
  }

  /**
   * adds cookies to the request
   * 
   * @param cookies
   *          array of cookie names and values (cookies[2*i] is a name,
   *          cookies[2*i + 1] is a value)
   * @throws IOException e
   */
  public void setCookies(String[] cookies) throws IOException {
    if (cookies == null)
      return;
    for (int i = 0; i < cookies.length - 1; i += 2) {
      setCookie(cookies[i], cookies[i + 1]);
    }
  }

  /**
   * adds a file parameter to the request
   * 
   * @param name
   *          parameter name
   * @param file
   *          the file to upload
   * @throws IOException e
   */
  public void setParameter(String name, File file) throws IOException {
    Logging.logTime("pre set file");
    setParameter(name, file.getPath(), new FileInputStream(file));
    Logging.logTime("post set file");
  }

  /**
   * adds a parameter to the request; if the parameter is a File, the file is
   * uploaded, otherwise the string value of the parameter is passed in the
   * request
   * 
   * @param name
   *          parameter name
   * @param object
   *          parameter value, a File or anything else that can be stringified
   * @throws IOException e
   */
  public void setParameter(String name, Object object) throws IOException {
    if (object instanceof File) {
      setParameter(name, (File) object);
    } else {
      setParameter(name, object.toString());
    }
  }

  /**
   * adds a string parameter to the request
   * 
   * @param name
   *          parameter name
   * @param value
   *          parameter value
   * @throws IOException e
   */
  public void setParameter(String name, String value) throws IOException {
    boundary();
    writeName(name);
    newline();
    newline();
    writeln(value);
  }

  /**
   * adds a file parameter to the request
   * 
   * @param name
   *          parameter name
   * @param filename
   *          the name of the file
   * @param is
   *          input stream to read the contents of the file from
   * @throws IOException e
   */
  public void setParameter(String name, String filename, InputStream is) throws IOException {
    Logging.logTime("setParameter begin (after new fileinput)");
    boundary();
    writeName(name);
    write("; filename=\"");
    write(filename);
    write('"');
    newline();
    write("Content-Type: ");
    Logging.logTime("pre guessContentTypeFromName");
    String type = URLConnection.guessContentTypeFromName(filename);
    if (type == null)
      type = "application/octet-stream";
    writeln(type);
    Logging.logTime("post guessContentTypeFromName");
    newline();
    pipe(is, os);
    newline();
  }

  /**
   * adds parameters to the request
   * 
   * @param parameters
   *          "name-to-value" map of parameters; if a value is a file, the file
   *          is uploaded, otherwise it is stringified and sent in the request
   * @throws IOException e
   */
  public void setParameters(Map<String, String> parameters) throws IOException {
    if (parameters == null)
      return;
    for (Iterator i = parameters.entrySet().iterator(); i.hasNext();) {
      Map.Entry entry = (Map.Entry) i.next();
      setParameter(entry.getKey().toString(), entry.getValue());
    }
  }

  /**
   * adds parameters to the request
   * 
   * @param parameters
   *          array of parameter names and values (parameters[2*i] is a name,
   *          parameters[2*i + 1] is a value); if a value is a file, the file is
   *          uploaded, otherwise it is stringified and sent in the request
   * @throws IOException e
   */
  public void setParameters(Object[] parameters) throws IOException {
    if (parameters == null)
      return;
    for (int i = 0; i < parameters.length - 1; i += 2) {
      setParameter(parameters[i].toString(), parameters[i + 1]);
    }
  }

  public void setRequestProperty(String key, String value) {
    if (connection != null) {
      connection.setRequestProperty(key, value);
    }
  }

  protected void write(char c) throws IOException {
    connect();
    os.write(c);
  }

  protected void write(String s) throws IOException {
    Logging.logTime("write-connect");
    connect();
    Logging.logTime("write-post connect");
    os.write(s.getBytes());
    Logging.logTime("post write s.getBytes");
  }

  protected void writeln(String s) throws IOException {
    connect();
    write(s);
    newline();
  }

  private void writeName(String name) throws IOException {
    newline();
    write("Content-Disposition: form-data; name=\"");
    write(name);
    write('"');
  }

}
