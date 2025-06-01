package org.myrobotlab.net;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

// FIXME - replace apache with okhttp
public class Http {

  private static volatile CloseableHttpClient httpclient = null;

  public final static Logger log = LoggerFactory.getLogger(Http.class);

  public static void init() {
    if (httpclient != null) {
      return;
    }
    synchronized (CloseableHttpClient.class) {

      // Set the maximum number of connections in the pool
      PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
      connManager.setMaxTotal(20);

      // Create a ClientBuilder Object by setting the connection manager
      HttpClientBuilder clientbuilder = HttpClients.custom().setConnectionManager(connManager);

      // Build the CloseableHttpClient object using the build() method.
      httpclient = clientbuilder.build();

    }
  }

  public static byte[] post(String url, String postBody) {
    return post(url, postBody, null, null);
  }

  public static byte[] post(String url, String postBody, String contentType) {
    return post(url, postBody, contentType, null);
  }

  /**
   * a super simple post method - TODO implement postResponse to get detailed
   * meta data around the request otherwise its just null if not successful and
   * non null when successful
   * 
   * @param url
   *          end point url e.g. https://google.com
   * @param postBody
   *          body for the post
   * @return byte data from post
   */
  public static byte[] post(String url, String postBody, String contentType, Map<String, String> formValues) {

    byte[] bytes = null;

    try {
      init();

      if (contentType == null) {
        contentType = "application/json";
      }

      // TODO - url encoding
      // List<NameValuePair> params = new ArrayList<NameValuePair>(2);
      // params.add(new BasicNameValuePair("param-1", "12345"));
      // params.add(new BasicNameValuePair("param-2", "Hello!"));
      // httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

      StringEntity jsonEntity = new StringEntity(postBody, "UTF-8");
      jsonEntity.setContentEncoding(contentType);

      HttpPost httpPost = new HttpPost(url);
      httpPost.setEntity(jsonEntity);
      CloseableHttpResponse response = httpclient.execute(httpPost);

      HttpEntity entity = response.getEntity();
      // Header encodingHeader = entity.getContentEncoding();

      // you need to know the encoding to parse correctly
      // Charset encoding = encodingHeader == null ? StandardCharsets.UTF_8 :
      // Charsets.toCharset(encodingHeader.getValue());

      bytes = EntityUtils.toByteArray(entity);

      // String ret = new String(bytes);
      // log.info("string value {}", ret);

    } catch (Exception e) {
      log.error("Http.post {} {} threw", url, postBody, e);
    }

    try {
      // httpclient.close(); NEED TO DO THIS ON A SHUTDOWN HOOK !!!
    } catch (Exception e) {
      /* don't care */}

    return bytes;
  }

  /**
   * GET the byte return from a url - if it fails return null TODO - getResponse
   * should be implemented if the desire is to get the meta info of
   * failure/success e.g. response code
   * 
   * @param theUrl
   * @return byte data from the get
   */
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
   * download the file as {filename}.part until it is complete, then do an os
   * rename this is a common safety technique, so that other processes or
   * threads do not consume a partially downloaded file.
   * 
   * @param theUrl
   *          url
   * @param outFile
   *          file to save
   * 
   */
  public static void getSafePartFile(String theUrl, String outFile) {
    getSafePartFile(theUrl, outFile, true);
  }

  public static void getSafePartFile(String theUrl, String outFile, boolean removePrevious) {
    try {
      get(theUrl, outFile + ".part");
      File f = new File(outFile);
      if (f.exists() && removePrevious) {
        f.delete();
      }
      File newFile = new File(outFile + ".part");
      newFile.renameTo(new File(outFile));
    } catch (Exception e) {
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
