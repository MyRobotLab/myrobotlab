package org.myrobotlab.codec;

import java.io.OutputStream;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.interfaces.MessageSender;

public abstract class Api {
  
  public final static String PREFIX_API = "api";
  public final static String PARAMETER_API = "/api/";


  // because of WebGui's "bug?" of request.body().getBytes() == null - we will use String instead 
  // public Object process(MessageSender sender, OutputStream out, URI uri, byte[] data) throws Exception;
 
  //public Object process(MessageSender sender, OutputStream out, String requestUri, String data) throws Exception;
  
  /**
   * getRequestURI() does not decode the string. Where getPathInfo() does
   * decode.
   * 
   * <pre>
   *  Servlet is mapped as /test%3F/* and the application is deployed under /app.
   *
   *  http://30thh.loc:8480/app/test%3F/a%3F+b;jsessionid=S%3F+ID?p+1=c+d&p+2=e+f#a
   *  
   *  Method              URL-Decoded Result           
   *  ----------------------------------------------------
   *  getContextPath()        no      /app
   *  getLocalAddr()                  127.0.0.1
   *  getLocalName()                  30thh.loc
   *  getLocalPort()                  8480
   *  getMethod()                     GET
   *  getPathInfo()           yes     /a?+b
   *  getProtocol()                   HTTP/1.1
   *  getQueryString()        no      p+1=c+d&p+2=e+f
   *  getRequestedSessionId() no      S%3F+ID
   *  getRequestURI()         no      /app/test%3F/a%3F+b;jsessionid=S+ID
   *  getRequestURL()         no      http://30thh.loc:8480/app/test%3F/a%3F+b;jsessionid=S+ID
   *  getScheme()                     http
   *  getServerName()                 30thh.loc
   *  getServerPort()                 8480
   *  getServletPath()        yes     /test?
   *  getParameterNames()     yes     [p 2, p 1]
   *  getParameter("p 1")     yes     c d
   * </pre>
   * 
   * @throws InvocationTargetException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   * @throws InstantiationException
   * @throws SecurityException
   * @throws NoSuchMethodException
   * @throws ClassNotFoundException
   * 
   */

  /**
   * CreateMessage creates a message from a URI - this is the "default" request
   * message which can be created from 'any' uri. Potentially, a uri could fill
   * the data field. In the 'default' parsing decoding - the expected parameters
   * are url encoded and json encoded where :
   * 
   * {scheme}://{host}:{port}/api/{apiKey}/{p0}/{p1}/{p(n)}/...
   * 
   * @param uri - inbound uri
   * @return - returns message constructed from the uri
   */
  static public Message uriToMsg(String uri) {
    Message msg = new Message();
    msg.uri = uri;
    msg.name = "runtime"; // default
    msg.method = "getApis"; // default
    msg.apiKey = ApiFactory.API_TYPE_SERVICE; // default
    
    int pos = uri.indexOf("/api/");
    
    if (pos == -1) {   
      // FIXME - better would be runtime.listApis() ->
      // {uri}/api/service
      // {uri}/api/messages ...
      //   throw new IllegalArgumentException("required /api/ not found in uri");
      return msg;
    }

    String requestUri = uri.substring(pos);
    String[] parts = requestUri.split("/");


    // FIXME - check for schema {schema}
    // {schema}://{host}:{port}{path}

    // if split > ~5 -> split[0].endsWith(":")
    // then -> get {path}

    // TODO - verify not null && /api/ exists
    // if it doesn't then set apiKey name & method
    // to return 'default' instructions on what apis are
    // available

    if (parts.length > 2) {
      msg.apiKey = parts[2];
    } else {
      return msg;
    }

    if (parts.length > 3) {
      msg.name = parts[3];
    }

    if (parts.length < 4) {
      // /api/service OR /api/service/
      msg.method = "getEnvironments";
    } else if (parts.length == 4) {
      msg.name = "runtime";
      if (requestUri.endsWith("/")) {
        msg.method = "getMethodMap";
      } else {
        msg.method = "getService";
      }
      msg.data = new Object[] { parts[3] };
    } else {
      msg.method = parts[4];
    }

    // URI /param0/param1/param(n) ...
    // URI has precedence over data or inputstream ...
    // default encoding is gson/json

    if (parts.length > 5) {
      // move parts to data
      msg.data = new Object[parts.length - 5];
      for (int i = 0; i < parts.length - 5; ++i) {
        msg.data[i] = parts[5 + i];
      }
    }
    return msg;
  }

  static public String getApiKey(String requestUri) {
    
    int pos = requestUri.indexOf(Api.PARAMETER_API);
    if (pos > -1){
      pos += Api.PARAMETER_API.length();
      int pos2 = requestUri.indexOf("/", pos);
      if (pos2 > -1){
        return requestUri.substring(pos, pos2);
      } else {
        return requestUri.substring(pos);
      }
    }
    return null;
  }
  
  abstract public Object process(MessageSender sender, OutputStream out, Message msgFromUri, String data) throws Exception;

}
