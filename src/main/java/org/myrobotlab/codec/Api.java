package org.myrobotlab.codec;

import java.io.OutputStream;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereRequestImpl.Body;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.cpr.Broadcaster;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.interfaces.MessageSender;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.WebGui;
import org.myrobotlab.string.StringUtil;
import org.slf4j.Logger;

public abstract class Api {

  public final static String PREFIX_API = "api";
  public final static String PARAMETER_API = "/api/";

  public final static Logger log = LoggerFactory.getLogger(Api.class);

  // because of WebGui's "bug?" of request.body().getBytes() == null - we will
  // use String instead
  // public Object process(MessageSender sender, OutputStream out, URI uri,
  // byte[] data) throws Exception;

  // public Object process(MessageSender sender, OutputStream out, String
  // requestUri, String data) throws Exception;

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
   * @param uri
   *          - inbound uri
   * @return - returns message constructed from the uri
   */
  public Message uriToMsg(String uri) {
    Message msg = new Message();
    msg.name = "runtime"; // default
    msg.method = "getApis"; // default
    msg.apiKey = ApiFactory.API_TYPE_SERVICE; // default

    int pos = uri.indexOf("/api/");

    if (pos == -1) {
      // FIXME - better would be runtime.listApis() ->
      // {uri}/api/service
      // {uri}/api/messages ...
      // throw new IllegalArgumentException("required /api/ not found in uri");
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
      msg.method = getDefaultMethod();
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

  protected String getDefaultMethod() {
    return "getRegistry";
  }

  /**
   * needed to get the api key to select the appropriate api processor
   * 
   * @param r
   * @return
   */
  static public String getApiKey(AtmosphereResource r) {
    String requestUri = r.getRequest().getRequestURI();
    return getApiKey(requestUri);
  }

  static public String getApiKey(String uri) {
    int pos = uri.indexOf(Api.PARAMETER_API);
    if (pos > -1) {
      pos += Api.PARAMETER_API.length();
      int pos2 = uri.indexOf("/", pos);
      if (pos2 > -1) {
        return uri.substring(pos, pos2);
      } else {
        return uri.substring(pos);
      }
    }
    return null;
  }

  // Api Imp has opportunity to override
  @Deprecated /* does nothing - we handle session - but we don't handle aging (if necessary) */
  protected void handleSession(WebGui webgui, String apiKey, AtmosphereResource r) {
    AtmosphereRequest request = r.getRequest();
    HttpSession s = request.getSession();
    if (s == null) {
      log.info("session is null");
    } else {
      // log.info("session {}", s);
    }
  }

  // FIXME - processBroacasts
  protected void setBroadcaster(WebGui webgui, String apiKey, AtmosphereResource r) {
    // FIXME - maintain single broadcaster for each session ?
    String uuid = r.uuid();
    
    // set root gateway broadcaster
    // r.broadcasters()
    // log.info("resource {}", r);
    // log.info("resource {}", StringUtil.toString(r.broadcasters()));
    // log.info("broadcaster {}", r.getBroadcaster());
    // AtmosphereResourceEvent event = r.getAtmosphereResourceEvent();
    /*
    if (event.equals("xx")) {
      r.setBroadcaster(webgui.getBroadcaster());
    }
    */

    Broadcaster uuiBroadcaster = webgui.getBroadcasterFactory().lookup(uuid);
    // create a unique broadcaster in the framework for this uuid
    if ( uuiBroadcaster == null) {
      uuiBroadcaster = webgui.getBroadcasterFactory().get(uuid);      
    } 
    uuiBroadcaster.addAtmosphereResource(r);
    uuiBroadcaster.getAtmosphereResources();
    r.addBroadcaster(uuiBroadcaster);
    // log.info("resource {}", r);
    // log.info("resource {}", StringUtil.toString(r.broadcasters()));
    // log.info("broadcaster {}", r.getBroadcaster());
    
    // FIXME - should not handle 
    // RELAYS HANDLED HERE !!
    if (webgui.getRelays().containsKey(uuid)) {
      List<String> list = webgui.getRelays().get(uuid);
      for (String ruuid : list) {
        Broadcaster rb = webgui.getBroadcasterFactory().lookup(ruuid);
        if (rb != null) {
          Body b = r.getRequest().body();
          if (b != null) {
            rb.broadcast(r.getRequest().body().asString());
          } else {
            log.warn("body null for {}", uuid);
          }
        } else {
          log.error("no broadcaster for {}", ruuid);
        }
      }
    }
  }

  protected void setHeaderContentType(WebGui webgui, String apiKey, AtmosphereResource r) {
    r.getResponse().addHeader("Content-Type", CodecUtils.MIME_TYPE_JSON);
  }

  protected void suspend(WebGui webgui, String apiKey, AtmosphereResource r) {
    if (!apiKey.equals(ApiFactory.API_TYPE_SERVICE) && !r.isSuspended()) {
      r.suspend();
    }
  }

  protected void handleError(WebGui webgui, String apiKey, AtmosphereResource r, Exception e) {

    try {

      AtmosphereResponse response = r.getResponse();
      OutputStream out = response.getOutputStream();

      response.addHeader("Content-Type", CodecUtils.MIME_TYPE_JSON);

      Status error = Status.error(e); // name == null anonymous ?
      Message msg = Message.createMessage(webgui, null, CodecUtils.getCallbackTopicName("getStatus"), error);
      if (ApiFactory.API_TYPE_SERVICE.equals(apiKey)) {
        // for the purpose of only returning the data
        // e.g. http://api/services/runtime/getUptime -> return the uptime
        // only not the message
        if (msg.data == null) {
          log.warn("<< {}", CodecUtils.toJson(null));
          CodecUtils.toJson(out, (Object)null);
        } else {
          // return the return type
          log.warn("<< {}", CodecUtils.toJson(msg.data[0]));
          CodecUtils.toJson(out, msg.data[0]);
        }
      } else {
        // API_TYPE_MESSAGES
        // DEPRECATE - FOR LOGGING ONLY REMOVE
        log.warn("<< {}", CodecUtils.toJson(msg)); // FIXME if logTraffic
        CodecUtils.toJson(out, msg);
      }

    } catch (Exception e2) {
      log.error("respond threw", e2, e);
    }

  }

  // FIXME - max complexity !!!!
  // abstract public Object process(WebGui webgui, OutputStream out, Message
  // msgFromUri, String data) throws Exception;

  /**
   * High level minimal parameter process for AtmosphereResource Many of the
   * details of the flow control (suspend, broadcast, error handling) are
   * dependent on the api being used
   * 
   * @param webgui
   * @param apiKey
   * @param r
   */
  public Object process(WebGui webgui, String apiKey, AtmosphereResource r) {

    try {
      String uuid = r.uuid();
      suspend(webgui, apiKey, r);
      setBroadcaster(webgui, apiKey, r);
      handleSession(webgui, apiKey, r);      
      setHeaderContentType(webgui, apiKey, r);
      String data = r.getRequest().body().asString();
      
      // addClient(webgui, apiKey, r, null);

      // CALLING MAX COMPLEXITY
      return process(webgui, apiKey, r.getRequest().getRequestURI(), uuid, r.getResponse().getOutputStream(), data);

    } catch (Exception e) {
      handleError(webgui, apiKey, r, e);
      return null;
    }
  }

  // MAX COMPLEXITY WITH SIMPLEST DATA TYPES
  public abstract Object process(MessageSender webgui, String apiKey, String uri, String uuid, OutputStream out, String json) throws Exception; // {
 /* FIXME - temporarily disabled ... 
  public void process(Runtime runtime, String apiKey, String uuid, Endpoint endpoint, String data) {
    try {

      // suspend(webgui, apiKey, r); is auto-suspended from the listening end
      // setBroadcaster(webgui, apiKey, r); // FIXME - relays are handled here
      // handleSession(webgui, apiKey, r); // FIXME - does nothing      
      setHeaderContentType(webgui, apiKey, r);
      // String data = r.getRequest().body().asString();
      
      // addClient(webgui, apiKey, r, null);

      // CALLING MAX COMPLEXITY
      return process(webgui, apiKey, endpoint.uri, uuid, r.getResponse().getOutputStream(), data);

    } catch (Exception e) {
      handleError(webgui, apiKey, r, e);
      return null;
    }
    
  }
*/
}
