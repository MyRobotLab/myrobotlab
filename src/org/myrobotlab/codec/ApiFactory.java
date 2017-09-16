package org.myrobotlab.codec;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.interfaces.MessageSender;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

/**
 * ApiProcessor - it selects the appropriate API from the apiKey located in the
 * URI. e.g. mrl://host:port/api/{apiKey}
 * 
 * The ApiFactory maintains all the resources to properly send and receive data
 * 
 * purpose of this class is to use as the gateway for processing incoming data
 * and requests it configures, 'simplifies', and maintains state for the long
 * many parameter (stateless) ApiProcessor.process method
 * 
 * FIXME - directory services - list methods/interfaces/data FIXME - add
 * configuration Map&lt;String, Object &gt; in the factory for any ApiProcessor
 * FIXME - add directory services for api/ , api/{apiKey} &amp; api/{apiKey}/
 * FIXME - you don't need a NameProvider - you just need the sender's name !
 * refactor
 * 
 * @author GroG
 *
 */
public class ApiFactory {

  public final static Logger log = LoggerFactory.getLogger(ApiFactory.class);

  public final static String API_TYPE_MESSAGES = "messages";
  public static final String API_TYPE_SERVICE = "service";

  Map<String, Api> processors = new HashMap<String, Api>();

  // per instance
  MessageSender sender = null;

  public static class ApiDescription {
    String key;
    String path; // {scheme}://{host}:{port}/api/messages
    String exampleUri;
    String description;

    public ApiDescription(String key, String uriDescription, String exampleUri, String description) {
      this.key = key;
      this.path = uriDescription;
      this.exampleUri = exampleUri;
      this.description = description;
    }
  }

  public static List<ApiDescription> getApis() {
    List<ApiDescription> apis = new ArrayList<ApiDescription>();
    String[] apiKeys = new String[] { API_TYPE_MESSAGES, API_TYPE_SERVICE };
    for (String key : apiKeys) {
      try {
        Class<?> clazz = Class.forName(String.format("org.myrobotlab.codec.Api%s", key.substring(0, 1).toUpperCase() + key.substring(1)));
        Method method = clazz.getMethod("getDescription");
        Object o = method.invoke(null);
        apis.add((ApiDescription) o);
      } catch (Exception e) {
        log.error("getApis threw", e);
      }
    }
    return apis;
  }

  /**
   * Api Router - this takes requests and delegates them to the appropriate
   * processor depending primarily on a {apiKey}
   * 
   * {scheme}://{host}:{port}/api/{apiKey}
   * 
   * String[] parts = requestUri.split("/") "api".equals(parts[1]) apiKey =
   * parts[2]
   * 
   * @param sender
   *          - MessageSender - if apiKey specifies a message based processor,
   *          the router will convert and send
   * @param out
   *          - an OutputStream from the requestor, results (if any) will be
   *          sent back to this OutputStream
   * @param requestUri - request Uri - the original requestUri of the request
   * @param data - request Payload data - inbound data
   * @return - return data from the invoked method
   * @throws Exception - can throw from invalid or broken streams
   */
  // TODO public Object process(MessageSender sender, OutputStream out, String
  // requestUri, byte data) throws Exception {
  // TODO public Object process(MessageSender sender, OutputStream out, String
  // requestUri, InputStream data) throws Exception {
  public Object process(MessageSender sender, OutputStream out, String requestUri, String data) throws Exception {
    log.debug("{} - data[{}]", requestUri, data);

    // FIXME - handle errors by returning structured errors - or allow
    // Exceptions to be thrown ?
    Message msg = Api.uriToMsg(requestUri);
    String apiKey = msg.apiKey;

    log.warn("{}", msg);
    
    if (!processors.containsKey(apiKey)) {
      String className = null;
      Object p = null;
      try {
        className = String.format("org.myrobotlab.codec.Api%s", apiKey.substring(0, 1).toUpperCase() + apiKey.substring(1));
        Class<?> clazz = Class.forName(className);
        Constructor<?> ctor = clazz.getConstructor();
        p = ctor.newInstance(new Object[] {});     
        processors.put(msg.apiKey, (Api) p);
      } catch (Exception e) {
        log.error("could not create api", e);
        return null; // vs return message status error with detail or getApis ?
      }      
    }

    // delegate request to process
    Api processor = processors.get(apiKey);
    return processor.process(sender, out, msg, data);
  }

  public static ApiFactory getInstance() {
    return getInstance(null);
  }

  public static ApiFactory getInstance(MessageSender sender) {
    ApiFactory api = new ApiFactory();
    api.sender = sender;
    return api;
  }

  public Object process(String requestUri) throws Exception {
    return process(sender, null, requestUri, null);
  }

  public Object process(OutputStream out, String requestUri) throws Exception {
    return process(sender, out, requestUri, null);
  }

  public Object process(OutputStream out, String requestUri, InputStream is) throws Exception {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();

    int nRead;
    byte[] b = new byte[16384];

    while ((nRead = is.read(b, 0, b.length)) != -1) {
      bos.write(b, 0, nRead);
    }
    bos.flush();
    String data = new String(bos.toByteArray());
    return process(null, out, requestUri, data);
  }

  public Object process(OutputStream out, String requestUri, byte[] byteArray) throws Exception {
    // FIXME - manage statics differently -
    String data = null;
    if (byteArray != null) {
      data = new String(byteArray);
    }
    return process(sender, out, requestUri, data);
  }

  public static void main(String[] args) throws InterruptedException {
    try {
      LoggingFactory.init(Level.INFO);

      Runtime runtime = (Runtime) Runtime.getInstance();
      ApiFactory api = ApiFactory.getInstance(runtime);
      Object o = null;
      ByteArrayOutputStream bos = new ByteArrayOutputStream();

      // =============== api messages begin =========================
      // FIXME change to CodecUtils.MIME_TYPE_JSON
      Codec codec = CodecFactory.getCodec(CodecUtils.MIME_TYPE_JSON);

      // FIXME !!! - double encoded data for messages api
      Message msg = Message.createMessage(runtime, "runtime", "getUptime", null);
      ByteArrayOutputStream encoded = new ByteArrayOutputStream();
      codec.encode(encoded, msg);

      Message msg2 = Message.createMessage(runtime, "runtime", "getD", null);
      ByteArrayOutputStream encoded2 = new ByteArrayOutputStream();
      codec.encode(encoded2, msg2);

      URI uri = new URI("http://localhost:8888/api/messages");
      uri.getPath();

      o = api.process(bos, "http://localhost:8888/api/messages/runtime/getService/runtime");
      log.info("ret - {}", new String(bos.toByteArray()));

      o = api.process(bos, "/api/messages", encoded.toByteArray());
      log.info("ret - {}", o);
      bos.reset();
      o = api.process(bos, "/api/messages/", encoded.toByteArray());
      log.info("ret - {}", o);
      bos.reset();
      // uri always has precedence over data
      o = api.process(bos, "/api/messages/runtime/getService/runtime");
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      o = api.process(bos, "/api/messages/runtime/start/servo/Servo");
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      o = api.process(bos, "/api/messages/servo");
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      o = api.process(bos, "/api/messages/servo/");
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      // =============== api messages end =========================

      // =============== api services begin =========================
      // FIXME - try both cases in each api
      // FIXME - try encoded json on the param lines
      o = api.process(bos, "mrl://localhost");
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      o = api.process(bos, "mrl://localhost:8888");
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      o = api.process(bos, "");
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      o = api.process(bos, "/");
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      o = api.process(bos, "/api");
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      o = api.process(bos, "/api/");
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      o = api.process(bos, "/api/services");
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      o = api.process(bos, "/api/services/"); // FIXME -
                                              // list
                                              // service
                                              // names/types
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      o = api.process(bos, "/api/services/runtime/getUptime");
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      o = api.process(bos, "/api/services/runtime/getService/runtime");
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      o = api.process(bos, "/api/services/runtime/getService/runtime");
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      o = api.process(bos, "/api/services/runtime/start/servo/Servo");
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      o = api.process(bos, "/api/services/servo");
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      o = api.process(bos, "/api/services/servo/");
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      // o = api.process(bos,
      // "/api/services/runtime/noWorky/GroG");
      // log.info("ret - {}", new String(bos.toByteArray()));
      // bos.reset();
      // =============== api services end =========================

    } catch (Exception e) {
      log.error("main threw", e);
    }

    Runtime.shutdown();
  }

  /*
   * public static String getServiceName(String[] parts) { if (parts == null ||
   * parts.length < 4){ return "runtime"; // relies on singlton standard ! }
   * else { return parts[3]; } }
   * 
   * public static String getMethodName(String requestUri) { String [] parts =
   * requestUri.split("/"); if (parts.length < 4){ return "getLocalServices"; }
   * else if (parts.length == 4){ if (requestUri.endsWith("/")){
   * 
   * }
   * 
   * } else { return parts[4]; } }
   */

}
