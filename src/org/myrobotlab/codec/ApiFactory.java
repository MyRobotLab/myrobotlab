package org.myrobotlab.codec;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.framework.Message;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.MessageSender;
import org.slf4j.Logger;

/**
 * ApiProcessor - it selects the appropriate API from the apiKey located in the
 * URI. e.g. mrl://host:port/api/{apiKey}
 * 
 * The ApiFactory maintains all the resources to properly send and receive data
 * 
 * purpose of this class is to use as the gateway for processing incoming data and requests
 * it configures, 'simplifies', and maintains state for the long many parameter (stateless) 
 * ApiProcessor.process method
 * 
 * FIXME - add configuration Map<String, Object> in the factory for any ApiProcessor
 * 
 * @author GroG
 *
 */
public class ApiFactory /* implements ApiProcessor */ {

  public final static Logger log = LoggerFactory.getLogger(ApiFactory.class);

  static Map<String, ApiProcessor> processors = new HashMap<String, ApiProcessor>();
  
  // TODO add registry reference from Runtime ??

  static ApiFactory api = null;
  // FIXME - probably don't want this as static
  static MessageSender sender = null;

  public Object process(MessageSender sender, OutputStream out, URI uri, byte[] data) throws Exception {
    log.info("{}", uri);
    String[] parts = null;
    String path = uri.getPath();
    if (path != null) {
      parts = path.split("/");
    }

    if (parts == null || parts.length < 3) {
      // return error in default (SWAGGER) API
      // return default (SWAGGER API) handle error ? THROW ???
    }

    /**
     * 0 / 1 / 2 ""/"api"/{apiKey}
     * 
     * String apiLitteral = parts[1]; if (!apiLitteral.equals("api")){
     * DEFAULT_API ERROR }
     */

    // TODO - default to something valid, if parts[2] is not valid ...

    String apiKey = parts[2];

    if (!processors.containsKey(apiKey)) {
      String className = null;

      className = String.format("org.myrobotlab.codec.ApiProcessor%s", apiKey.substring(0, 1).toUpperCase() + apiKey.substring(1));
      Class<?> clazz = Class.forName(className);
      Constructor<?> ctor = clazz.getConstructor();
      Object p = ctor.newInstance(new Object[] {});
      processors.put(apiKey, (ApiProcessor) p);
    }

    ApiProcessor processor = processors.get(apiKey);
    return processor.process(sender, out, uri, data);
  }

  public static ApiFactory getInstance() {
    return getInstance(null);
  }
  
  public static ApiFactory getInstance(MessageSender sender) {
    if (api == null) {
      init();
    }
    ApiFactory.sender = sender;
    return api;
  }

  private static synchronized void init() {
    if (api == null) {
      api = new ApiFactory();
    }
  }
  
  public Object process(String uri) throws Exception {
    return process(sender, null, new URI(uri), null);
  }
  


  public Object process(OutputStream out, String uri) throws Exception {
    return process(sender, out, new URI(uri), null);
  }

  
  ///////////////////////////////////////////////////////////

  public Object process(OutputStream out, String uri, InputStream is) throws Exception {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    int nRead;
    byte[] data = new byte[16384];

    while ((nRead = is.read(data, 0, data.length)) != -1) {
      buffer.write(data, 0, nRead);
    }

    buffer.flush();
    return process(null, out, new URI(uri), buffer.toByteArray());
  }

  public Object process(OutputStream out, String uri, byte[] data) throws Exception {
    // FIXME - manage statics differently
    return process(sender, out, new URI(uri), data);
  }

  public Object process(OutputStream out, String uri, Object... params) throws Exception {
    return process(sender, out, new URI(uri), null);
  }


  public Object process(OutputStream out, URI uri, byte[] data) throws Exception {
    return process(sender, out, uri, data);
  }

  
  /////////////////////////////////////////////////////////

  public static void main(String[] args) throws InterruptedException {
    try {
      LoggingFactory.init(Level.INFO);

      Runtime runtime = (Runtime)Runtime.getInstance();
      byte[] data = null;
      ApiFactory api = ApiFactory.getInstance(runtime);
      Object o = null;
      ByteArrayOutputStream bos = new ByteArrayOutputStream();

      // =============== api messages begin =========================
   // FIXME change to CodecUtils.MIME_TYPE_JSON
      Codec codec = CodecFactory.getCodec(CodecUtils.MIME_TYPE_MRL_JSON);
      
      Message msg = Message.createMessage(runtime, "runtime", "getUptime", null);
      ByteArrayOutputStream encoded = new ByteArrayOutputStream();
      codec.encode(encoded, msg);

      o = api.process(bos, "http://localhost:8888/api/messages", encoded.toByteArray());
      log.info("ret - {}", o);
      bos.reset();
      o = api.process(bos, "http://localhost:8888/api/messages/", encoded.toByteArray());
      log.info("ret - {}", o);
      bos.reset();
      o = api.process(bos, "http://localhost:8888/api/messages/runtime/getService/runtime");
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      o = api.process(bos, "http://localhost:8888/api/messages/runtime/start/servo/Servo");
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      o = api.process(bos, "http://localhost:8888/api/messages/servo");
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      o = api.process(bos, "http://localhost:8888/api/messages/servo/");
      log.info("ret - {}", new String(bos.toByteArray()));
      // =============== api messages end =========================

      // =============== api services begin =========================
      // FIXME - try both cases in each api
      // FIXME - try encoded json on the param lines
      o = api.process("mrl://localhost");
      log.info("ret - {}", o);
      bos.reset();
      o = api.process("mrl://localhost:8888");
      log.info("ret - {}", o);
      bos.reset();
      o = api.process("http://localhost:8888");
      log.info("ret - {}", o);
      bos.reset();
      o = api.process("http://localhost:8888/");
      log.info("ret - {}", o);
      bos.reset();
      o = api.process("http://localhost:8888/api");
      log.info("ret - {}", o);
      bos.reset();
      o = api.process("http://localhost:8888/api/");
      log.info("ret - {}", o);
      bos.reset();
      o = api.process("http://localhost:8888/api/services");
      log.info("ret - {}", o);
      bos.reset();
      o = api.process("http://localhost:8888/api/services/"); // FIXME - list service names/types
      log.info("ret - {}", o);
      bos.reset();
      o = api.process("http://localhost:8888/api/services/runtime/getUptime");
      log.info("ret - {}", o);
      bos.reset();
      o = api.process("http://localhost:8888/api/services/runtime/getService/runtime");
      log.info("ret - {}", o);
      bos.reset();
      o = api.process(bos, "http://localhost:8888/api/services/runtime/getService/runtime");
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      o = api.process(bos, "http://localhost:8888/api/services/runtime/start/servo/Servo");
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      o = api.process(bos, "http://localhost:8888/api/services/servo");
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      o = api.process(bos, "http://localhost:8888/api/services/servo/");
      log.info("ret - {}", new String(bos.toByteArray()));
      // =============== api services end =========================

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

 
}
