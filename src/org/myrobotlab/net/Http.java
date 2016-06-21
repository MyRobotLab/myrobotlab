package org.myrobotlab.net;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.slf4j.Logger;

public class Http {
  /**
   * FIXME - OFFER PROXY !!!! See Below !!! NOT APACHE HTTPCLIENT IN FACT
   * HTTPCLIENT SHOULD ABEND TO THE ENV SET HERE !!!!
   * http://stackoverflow.com/questions/15927079/how-to-use-httpsurlconnection-
   * through-proxy-by-setproperty
   */

  public final static Logger log = LoggerFactory.getLogger(Http.class);

  public static byte[] get(String theUrl) {
    log.info("get {}", theUrl);
    ByteArrayOutputStream out = null;
    InputStream in = null;
    try {
      // create a url object
      URL url = new URL(theUrl);

      // create a urlconnection object
      URLConnection urlConnection = url.openConnection();

      in = urlConnection.getInputStream();
      out = new ByteArrayOutputStream();
      byte[] buffer = new byte[1024]; // you can configure the buffer size
      int length;

      while ((length = in.read(buffer)) != -1)
        out.write(buffer, 0, length); // copy streams
      in.close(); // call this in a finally block

      out.flush();
      return out.toByteArray();

    } catch (Exception e) {
      Logging.logError(e);
      return null;
    } finally {
      try {
        if (out != null)
          out.close();
      } catch (Exception e) {
      }

      try {
        if (in != null)
          in.close();
      } catch (Exception e) {
      }
    }

  }

  public static void main(String[] args) {
    byte[] data = Http.get("http://mrl-bucket-01.s3.amazonaws.com/current/develop/version.txt");
    String version = new String(data);
    int v = Integer.parseInt(version);
    log.info(version + " " + v);

  }

}
