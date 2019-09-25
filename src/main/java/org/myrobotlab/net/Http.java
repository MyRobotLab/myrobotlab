package org.myrobotlab.net;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class Http {
  /**
   * FIXME = there should be no statics - and when a file is downloaded or errors occur
   * there should be published statuses
   * 
   * FIXME - OFFER PROXY !!!! See Below !!! NOT APACHE HTTPCLIENT IN FACT
   * HTTPCLIENT SHOULD ABEND TO THE ENV SET HERE !!!!
   * http://stackoverflow.com/questions/15927079/how-to-use-httpsurlconnection-
   * through-proxy-by-setproperty
   * 
   * FIXME - return a Callable !
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

    } catch (FileNotFoundException e) {
      log.error("404 - {} not found", theUrl);
      return null;
    } catch (Exception e) {
      log.error("get threw", e);
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
  
  /**
   * download the file as {filename}.part until it is complete, then do an os rename
   * this is a common safety technique, so that other processes or threads do not consume
   * a partially downloaded file.
   * 
   * @param theUrl
   * @param outFile
   */
  public static void getSafePartFile(String theUrl, String outFile) {
    getSafePartFile(theUrl, outFile, true);
  }
  
  public static void getSafePartFile(String theUrl, String outFile, boolean removePrevious) {
    try {
        get(theUrl, outFile  + ".part");
        File f = new File(outFile);
        if (f.exists() && removePrevious) {
          f.delete();
        }
        File newFile = new File(outFile  + ".part");
        newFile.renameTo(new File(outFile));
    } catch(Exception e) {
      log.error("getSafePartFile threw", e);
    }
  }

  public static void get(String theUrl, String outFile) throws IOException {
    log.info("get {} --save to--> {}", theUrl, outFile);

      URL url = new URL(theUrl);
      URLConnection urlConnection = url.openConnection();
      InputStream in = urlConnection.getInputStream();
      FileOutputStream out = new FileOutputStream(outFile);
      
      byte[] buffer = new byte[8192]; // you can configure the buffer size
      int length;

      while ((length = in.read(buffer)) != -1) {
        out.write(buffer, 0, length); // copy streams
      }
      
      in.close(); // call this in a finally block
      out.close();
  }
  
  public static void getFile(String url) throws IOException {
    getFile(url, null);
  }

  public static void getFile(String url, String outFile) throws IOException {
    if (outFile == null) {
      outFile = url.substring(url.lastIndexOf("/") + 1);
    }
    get(url, outFile);
  }


}
