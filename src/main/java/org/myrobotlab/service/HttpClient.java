/**
 *                    
 * @author grog (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License 2.0 as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License 2.0 for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.InstallCert;
import org.myrobotlab.service.config.HttpClientConfig;
import org.myrobotlab.service.data.HttpData;
import org.myrobotlab.service.interfaces.HttpDataListener;
import org.myrobotlab.service.interfaces.HttpResponseListener;
import org.myrobotlab.service.interfaces.TextPublisher;
import org.slf4j.Logger;

/**
 * HttpClient - wrapper for Apache HttpClient
 * 
 * @author GroG * Synchronous or Asynchronous - Synchronous by default,
 *         Asynchronous if a callback method is supplied or Non-Blocking method
 *         is called
 * 
 *         Check out - Fluent interface -
 *         https://hc.apache.org/httpcomponents-client-ga/tutorial/html/fluent.html
 * 
 *         - Proxies proxies proxies ! -
 *         https://memorynotfound.com/configure-http-proxy-settings-java/
 */
public class HttpClient<C extends HttpClientConfig> extends Service<C> implements TextPublisher {

  public final static Logger log = LoggerFactory.getLogger(HttpClient.class);

  private static final long serialVersionUID = 1L;

  transient CloseableHttpClient client;
  
  /**
   * simple pojo for request data
   */
  public class HttpRequestData {
    public String url;
    public String verb;
    public String body;
    public HttpRequestData(String verb, String url, String body) {
      this.verb = verb;
      this.url = url;
      this.body = body;      
    }
  }

  public HttpClient(String n, String id) {
    super(n, id);
  }

  @Override
  public void attach(Attachable service) {
    // determine type
    if (HttpDataListener.class.isAssignableFrom(service.getClass())) {
      attachHttpDataListener((HttpDataListener) service);
    } else if (HttpResponseListener.class.isAssignableFrom(service.getClass())) {
      attachHttpResponseListener((HttpResponseListener) service);
    }
    error("%s doesn't know how to attach a %s", getClass().getSimpleName(), service.getClass().getSimpleName());
  }

  public void attachHttpDataListener(HttpDataListener service) {
    addListener("publishHttpData", service.getName());
  }

  public void attachHttpResponseListener(HttpResponseListener service) {
    addListener("publishHttpResponse", service.getName());
  }

  @Deprecated /* use attachHttpDataListener(HttpDataListener) */
  public void addHttpDataListener(HttpDataListener listener) {
    attachHttpDataListener(listener);
  }

  @Deprecated /* used attachHttpResponseListener(HttpResponseListener) */
  public void addHttpResponseListener(HttpResponseListener listener) {
    attachHttpResponseListener(listener);
  }

  /**
   * Simplest GET return string type of the endpoint
   * 
   * @param url
   *          the url to get
   * @return the data as a string
   * @throws ClientProtocolException
   *           boom
   * @throws IOException
   *           boom
   * 
   */
  public String get(String url) throws ClientProtocolException, IOException {
    HttpData response = processResponse(new HttpGet(url));
    HttpRequestData rd = new HttpRequestData("GET", url, null);
    invoke("publishHttpRequestData", rd);
    if (response.data != null) {
      return new String(response.data);
    }
    return null;
  }

  /**
   * GET bytes from endpoint
   * 
   * @param url
   *          the url
   * @return the bytes returned
   * @throws ClientProtocolException
   *           boom
   * @throws IOException
   *           boom
   * 
   */
  public byte[] getBytes(String url) throws ClientProtocolException, IOException {
    HttpRequestData rd = new HttpRequestData("GET", url, null);
    invoke("publishHttpRequestData", rd);
    return processResponse(new HttpGet(url)).data;
  }

  /**
   * GET HttpData from endpoint - returns a more rich response type - includes
   * response code and headers
   * 
   * @param url
   *          the url
   * @return the http data returned
   * @throws IOException
   *           boom
   */
  public HttpData getResponse(String url) throws IOException {
    HttpData response = processResponse(new HttpGet(url));
    return response;
  }

  /**
   * Post without body
   * 
   * @param url
   *          the url to post to
   * @return the string returned
   * @throws ClientProtocolException
   *           boom
   * @throws IOException
   *           boom
   * 
   */
  public String post(String url) throws ClientProtocolException, IOException {
    byte[] bytes = postBytes(url, null, null);
    if (bytes != null) {
      return new String(bytes);
    }
    return null;
  }
  
  /**
   * Post JSON with authorization of type bearer token
   * @param auth
   * @param url
   * @param json
   * @return
   * @throws IOException
   */
  public String postJson(String auth, String url, String json) throws IOException {
    HttpPost request = new HttpPost(url);
    HttpRequestData rd = new HttpRequestData("POST", url, json);
    invoke("publishHttpRequestData", rd);
    StringEntity params = new StringEntity(json);
    if (auth != null) {
      request.addHeader("Authorization", "Bearer " + auth);
    }    
    request.addHeader("Content-Type", "application/json");
    request.setEntity(params);
    HttpData data = processResponse(request);
    if (data.data != null) {
      return new String(data.data);
    }
    return null;

  }

  /**
   * Post a json string to an endpoint. This method adds the appropriate
   * contentype and return a string of data
   * 
   * @param url
   *          the url
   * @param json
   *          the json to post
   * @return the returned string
   * @throws IOException
   *           boom
   * 
   */
  public String postJson(String url, String json) throws IOException {
    return postJson(null, url, json);
  }

  /**
   * post and object to a json endpoint
   * 
   * @param url
   *          the url
   * @param object
   *          the object to post as json
   * @return the returned string
   * @throws IOException
   *           boom
   * 
   */
  public String postJson(String url, Object object) throws IOException {
    return postJson(url, CodecUtils.toJson(object));
  }

  /**
   * post json to an endpoint where you want bytes from
   * 
   * @param url
   *          the url
   * @param json
   *          the json
   * @return the bytes returned
   * @throws IOException
   *           boom
   * 
   */
  public byte[] postJsonToBytes(String url, String json) throws IOException {
    Map<String, String> headers = new HashMap<>();
    headers.put("content-type", "application/json");
    return postBytes(url, headers, json.getBytes());
  }

  /**
   * html form post
   * 
   * @param url
   *          the url
   * @param fields
   *          the key/value params to post
   * @return the returned string
   * @throws ClientProtocolException
   *           boom
   * @throws IOException
   *           boom
   * 
   */
  public String postForm(String url, Map<String, String> fields) throws ClientProtocolException, IOException {
    HttpPost request = new HttpPost(url);
    if (fields != null && fields.size() > 0) {
      List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(fields.size());
      for (String nvPairKey : fields.keySet()) {
        nameValuePairs.add(new BasicNameValuePair(nvPairKey, fields.get(nvPairKey)));
        request.setEntity(new UrlEncodedFormEntity(nameValuePairs));
      }
    }

    HttpData data = processResponse(request);

    if (data != null) {
      return new String(data.data);
    }
    return null;
  }

  public byte[] postBytes(String url, Map<String, String> headers, byte[] data) throws ClientProtocolException, IOException {
    String strData = null;
    if (data != null) {
      strData = new String(data); 
    }
    HttpRequestData rd = new HttpRequestData("POST", url, strData);
    invoke("publishHttpRequestData", rd);

    HttpPost request = new HttpPost(url);
    if (data != null) {
      request.setEntity(new ByteArrayEntity(data));
    }

    if (headers != null) {
      for (String key : headers.keySet()) {
        request.setHeader(key, headers.get(key));
      }
    } else {
      request.setHeader("Content-type", "application/octet-stream");
    }
    return processResponse(request).data;
  }

  /**
   * All method types process the request through this method - this is to keep
   * future maintenance to a minimum
   * 
   * @param request
   *          the http req
   * @return the httpdata
   * @throws IOException
   *           boom
   * 
   */
  public HttpData processResponse(HttpUriRequest request) throws IOException {
    String url = request.getURI().toString();
    HttpData data = new HttpData(url);
    invoke("publishUrl", url);

    log.info("url [{}]", url);

    HttpResponse response = client.execute(request);
    StatusLine statusLine = response.getStatusLine();
    data.responseCode = statusLine.getStatusCode();
    HttpEntity entity = response.getEntity();
    Header header = entity.getContentType();
    if (header != null) {
      data.contentType = header.getValue().toString();
    }

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    response.getEntity().writeTo(baos);
    data.data = baos.toByteArray();

    // publishing
    invoke("publishHttpData", data);
    if (data.data != null) {
      String text = new String(data.data);
      invoke("publishHttpResponse", text);
      invoke("publishText", text);
    }

    return data;
  }
  
  
  public HttpRequestData publishHttpRequestData(HttpRequestData data) {
    return data;
  }

  /**
   * publishing point for any http request this is the asynchronous callback
   * which will arrive typically at publishHttpData(data)
   * 
   * contains more data than just the text, can be used for any content type
   * too, since the payload is in a byte[]
   * 
   * @param data
   *          the http data
   * @return the http data
   * 
   */
  public HttpData publishHttpData(HttpData data) {
    return data;
  }

  // Set the default host/protocol for the methods to connect to.
  // This value will only be used if the methods are not given an absolute URI
  // httpClient.getHostConfiguration().setHost("hc.apache.org", 80, "http");

  // Map<String, HttpData> clients = new HashMap<String, HttpData>();

  // TODO - proxy !
  // TODO - authentication !

  /**
   * publishing point for any http request this is the asynchronous callback
   * which will arrive typically at onHttpRespone(data)
   * 
   * @param data
   *          the data
   * @return the data
   * 
   */
  public String publishHttpResponse(String data) {
    return data;
  }

  @Override
  public void startService() {
    super.startService();
    if (client == null) {
      // basic closable client created - which accepts system properties
      client = HttpClients.createSystem();
    }
  }

  public void installCert(String url) {
    try {
      InstallCert.install(url);
    } catch (Exception e) {
      error(e);
    }
  }

  @Override
  public String publishText(String text) {
    return text;
  }

  public String postForm(String url, String... fields) throws ClientProtocolException, IOException {
    if (fields == null || fields.length % 2 != 0) {
      log.error("postForm fields must be in the form \"key1\", \"value1\", \"key2\", \"value2\"");
      return null;
    }
    Map<String, String> data = new HashMap<>();
    for (int i = 0; i < fields.length; i = i + 2) {
      data.put(fields[i], fields[i + 1]);
    }
    return postForm(url, data);
  }

  public String publishUrl(String url) {
    return url;
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {

      HttpClient client = (HttpClient) Runtime.start("client", "HttpClient");

      // this is how a listener might subscribe
      // TODO - put dynamically subscribing into framework
      // with interface inspection ??

      // FIXME - add make the attach !

      // client.attach(client);

      // client.addHttpResponseListener(client);
      // client.addHttpDataListener(client);

      String data = client.get("https://postman-echo.com/get?foo1=bar1&foo2=bar2");
      log.info(data);

      // curl --location --request POST "https://postman-echo.com/post" --data
      // "foo1=bar1&foo2=bar2"
      client.postForm("https://postman-echo.com/post", "foo1", "bar1", "foo2", "bar2", "foo3", "bar with spaces");

      String response = client.post("http://www.cs.tut.fi/cgi-bin/run/~jkorpela/echo.cgi");

      log.info(response);

      response = client.post("http://www.cs.tut.fi/cgi-bin/run/~jkorpela/echo.cgi");
      log.info(response);

      response = client.get("http://www.google.com/search?hl=en&q=myrobotlab&btnG=Google+Search&aq=f&oq=");
      log.info(response);

      // <host>[:port] [passphrase]
      // InstallCert.main(new String[] { "searx.laquadrature.net:443" });

      // client.installCert("https://searx.laquadrature.net");
      String json = client.get("https://searx.laquadrature.net/?q=cat&format=json");
      log.info(json);

      // Runtime.start("gui", "SwingGui");
      boolean done = true;

      if (done) {
        return;
      }

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}
