/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
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
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.HttpData;
import org.myrobotlab.service.interfaces.HttpDataListener;
import org.myrobotlab.service.interfaces.HttpResponseListener;
import org.slf4j.Logger;

/**
 * HttpClient - wrapper for Apache HttpClient
 * 
 * @author GroG
 * 
 *         TODO - asynchronous call back similar to AngularJS promise - or at
 *         least a callback method is called .. onHttpResponse
 * 
 *         Synchronous or Asynchronous - Synchronous by default, Asynchronous
 *         if a callback method is supplied or Non-Blocking method is called
 *         
 *         Check out - Fluent interface - https://hc.apache.org/httpcomponents-client-ga/tutorial/html/fluent.html
 */
public class HttpClient extends Service implements HttpDataListener, HttpResponseListener {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(HttpClient.class);

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(HttpClient.class.getCanonicalName());
    meta.addDescription("a general purpose http client, used to fetch information on the web");
    meta.addCategory("network");
    meta.addDependency("org.apache.commons.httpclient", "4.5.2");
    meta.setCloudService(true);
    return meta;
  }

  transient CloseableHttpClient client;

  transient HashMap<String, String> formFields = new HashMap<String, String>();

  public HttpClient(String n) {
    super(n);
  }

  public void addFormField(String name, String value) {
    formFields.put(name, value);
  }

  public void addHttpDataListener(ServiceInterface listener) {
    /*
     * TODO - finish this thought out .. it would mean a Map of method
     * signatures to interface methods .. and direct callbacks Pro - is an
     * optimization Con - is potentially blocking the callback thread for "too"
     * long
     * 
     * if (SerialDataListener.class.isAssignableFrom(listener.getClass()) &&
     * listener.isLocal()) { // direct callback
     * listeners.put(si.getName(),(SerialDataListener) si); } else {
     */

    // pub sub
    // instead of getting the data twice and expecting 2 methods for more or
    // less the same material
    // we will leave it up to the subscribing service to do subscribe and
    // implement onHttpData
    listener.subscribe(getName(), "publishHttpData");

    // }
  }

  public void addHttpResponseListener(ServiceInterface listener) {
    listener.subscribe(getName(), "publishHttpResponse");
  }

  public void clearForm() {
    formFields.clear();
  }

  public String get(String uri) throws ClientProtocolException, IOException {
    HttpData response = processResponse((HttpUriRequest) new HttpGet(uri), null);
    if (response.data != null) {
      return new String(response.data);
    }
    return null;
  } 

  public byte[] getBytes(String uri) throws ClientProtocolException, IOException {
    return processResponse((HttpUriRequest) new HttpGet(uri), null).data;
  }

  /**
   * publishHttpData contains more information content type, response code,
   * etc... need to subscribe to it manually for testing purposes
   * 
   */
  @Override
  public void onHttpData(HttpData data) {
    log.info(data.toString());
  }

  /**
   * for testing purposes
   * 
   */
  @Override
  public void onHttpResponse(String data) {
    log.info(data);
  }

  public String post(String uri) throws ClientProtocolException, IOException {
    HttpData response = processResponse((HttpUriRequest) new HttpPost(uri), null);
    if (response.data != null) {
      return new String(response.data);
    }
    return null;
  }

  public String post(String uri, HashMap<String, String> fields) throws ClientProtocolException, IOException {
    byte[] data = postBytes(uri, fields);
    if (data != null) {
      return new String(data);
    }
    return null;
  }

  public byte[] postBytes(String uri, HashMap<String, String> fields) throws ClientProtocolException, IOException {
    return processResponse((HttpUriRequest) new HttpPost(uri), fields).data;
  }

  public HttpData processResponse(HttpUriRequest request, HashMap<String, String> fields) throws IOException {
    HttpData data = new HttpData(request.getURI().toString());
    if (fields == null) {

      fields = formFields;
    }

    // Mats changed 2017-01-03. I think it was a bug 
    // if (request.getClass().equals(HttpPost.class) && formFields.size() > 0)
    if (request.getClass().equals(HttpPost.class) && fields.size() > 0) {
      List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(fields.size());
      for (String nvPairKey : fields.keySet()) {
        nameValuePairs.add(new BasicNameValuePair(nvPairKey, fields.get(nvPairKey)));
        ((HttpPost) request).setEntity(new UrlEncodedFormEntity(nameValuePairs));
      }
    }
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
      invoke("publishHttpResponse", new String(data.data));
    }

    return data;
  }

  /**
   * publishing point for any http request this is the asynchronous callback
   * which will arrive typically at publishHttpData(data)
   * 
   * contains more data than just the text, can be used for any content type
   * too, since the payload is in a byte[]
   * @param data the http data
   * @return the http data
   * 
   */
  public HttpData publishHttpData(HttpData data) {
    return data;
  }

  /**
   * publishing point for any http request this is the asynchronous callback
   * which will arrive typically at onHttpRespone(data)
   * @param data the data
   * @return the data
   * 
   */
  public String publishHttpResponse(String data) {
    return data;
  }

  public void startService() {
    super.startService();
    if (client == null) {
      // new MultiThreadedHttpConnectionManager()
      client = HttpClients.createDefault();
    }
  }

  // Set the default host/protocol for the methods to connect to.
  // This value will only be used if the methods are not given an absolute URI
  // httpClient.getHostConfiguration().setHost("hc.apache.org", 80, "http");

  // Map<String, HttpData> clients = new HashMap<String, HttpData>();

  // TODO - proxy !
  // TODO - authentication !

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {

      HttpClient client = (HttpClient) Runtime.start("client", "HttpClient");
      Runtime.start("gui", "SwingGui");
      boolean done = true;
      
      if (done){
        return;
      }
      // this is how a listener might subscribe
      // TODO - put dynamically subscribing into framework
      // with interface inspection ??
      client.addHttpResponseListener(client);
      client.addHttpDataListener(client);

      // TODO - getByteArray(...)
      String index = client.get("https://www.cs.tut.fi/~jkorpela/forms/testing.html");
      log.info(index);

      client.addFormField("Comments", "This is a different comment");
      client.addFormField("Box", "yes");
      client.addFormField("Unexpected", "this is an unexpected field");
      client.addFormField("hidden field", "something else");

      String response = client.post("http://www.cs.tut.fi/cgi-bin/run/~jkorpela/echo.cgi");

      log.info(response);

      client.clearForm();
      client.addFormField("hidden field", "something else");
      response = client.post("http://www.cs.tut.fi/cgi-bin/run/~jkorpela/echo.cgi");
      log.info(response);

      response = client.get("http://www.google.com/search?hl=en&q=myrobotlab&btnG=Google+Search&aq=f&oq=");
      log.info(response);

    } catch (Exception e) {
      Logging.logError(e);
    }

  }

}
