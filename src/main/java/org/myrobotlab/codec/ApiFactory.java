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

import org.atmosphere.cpr.AtmosphereResource;
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

  public static final String API_TYPE_MESSAGES = "messages";
  public static final String API_TYPE_MESSAGES_BLOCKING = "messagesBlocking";
  public static final String API_TYPE_SERVICE = "service";

  static Map<String, Api> processors = new HashMap<String, Api>();

  // per instance
  // MessageSender sender = null;

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
    String[] apiKeys = new String[] { API_TYPE_MESSAGES, API_TYPE_MESSAGES_BLOCKING, API_TYPE_SERVICE };
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

  public static Api getApiProcessor(String key) {
    try {
      log.debug("getInstance({})", key);

      if (processors.containsKey(key)) {
        return processors.get(key);
      } else {
        String className = null;
        Object p = null;

        className = String.format("org.myrobotlab.codec.Api%s", key.substring(0, 1).toUpperCase() + key.substring(1));
        Class<?> clazz = Class.forName(className);
        Constructor<?> ctor = clazz.getConstructor();
        p = ctor.newInstance(new Object[] {});
        processors.put(key, (Api) p);
        return (Api) p;
      }
    } catch (Exception e) {
      log.error("could not create api", e);
      return null;
    }
  }

  public static void main(String[] args) throws InterruptedException {
    try {
      LoggingFactory.init(Level.INFO);

      Runtime runtime = (Runtime) Runtime.getInstance();
      ApiService api = (ApiService)ApiFactory.getApiProcessor("service");
      // ApiFactory api = ApiFactory.getInstance("");
      Object o = null;
      ByteArrayOutputStream bos = new ByteArrayOutputStream();

      // =============== api messages begin =========================
      // FIXME change to CodecUtils.MIME_TYPE_JSON

      // FIXME !!! - double encoded data for messages api
      Message msg = Message.createMessage(runtime, "runtime", "getUptime", null);
      ByteArrayOutputStream encoded = new ByteArrayOutputStream();
      CodecUtils.toJson(encoded, msg);

      Message msg2 = Message.createMessage(runtime, "runtime", "getD", null);
      ByteArrayOutputStream encoded2 = new ByteArrayOutputStream();
      CodecUtils.toJson(encoded2, msg2);

      URI uri = new URI("http://localhost:8888/api/messages");
      uri.getPath();

      
      o = api.process(bos, "http://localhost:8888/api/messages/runtime/getService/runtime");
      log.info("ret - {}", new String(bos.toByteArray()));

      /*
      o = api.process(bos, "/api/messages", encoded.toByteArray());
      log.info("ret - {}", o);
      bos.reset();
      o = api.process(bos, "/api/messages/", encoded.toByteArray());
      log.info("ret - {}", o);
      bos.reset();
      */
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

}
