package org.myrobotlab.codec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.net.URI;

import org.junit.Test;
import org.myrobotlab.framework.Message;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Servo;
import org.slf4j.Logger;

public class ApiFactoryTest {
  
  public final static Logger log = LoggerFactory.getLogger(ApiFactoryTest.class);



  @Test
  public void testProcessString() {
    try {
      LoggingFactory.init(Level.INFO);

      Runtime runtime = (Runtime) Runtime.getInstance();
      ApiFactory api = ApiFactory.getInstance(runtime);
      Object o = null;
      ByteArrayOutputStream bos = new ByteArrayOutputStream();

      // TODO - check encoded parameters in path uri - full object encoding
      
      // =============== api messages begin =========================
      // FIXME change to CodecUtils.MIME_TYPE_JSON
      Codec codec = CodecFactory.getCodec(CodecUtils.MIME_TYPE_JSON);
      String retJson = null;
      
      // FIXME !!! - double encoded data for messages api
      Message msg = Message.createMessage(runtime, "runtime", "getUptime", null);
      ByteArrayOutputStream encoded = new ByteArrayOutputStream();
      codec.encode(encoded, msg);

      Message msg2 = Message.createMessage(runtime, "runtime", "getD", null);
      ByteArrayOutputStream encoded2 = new ByteArrayOutputStream();
      codec.encode(encoded2, msg2);
      

      URI uri = new URI("http://localhost:8888/api/messages");
      uri.getPath();
      
      // check key
      String key = Api.getApiKey("http://userid:passwd@localhost:8888/api/messages/runtime/getService/runtime");
      assertEquals(ApiFactory.API_TYPE_MESSAGES, key);
      
      // return object
      o = api.process(bos, "http://localhost:8888/api/messages/runtime/getService/runtime");
      assertTrue(o == runtime);
      
      
      // get apis
      bos.reset();
      o = api.process(bos, "http://localhost:8888/api");
      retJson = new String(bos.toByteArray());
      log.info("ret - {}", retJson);
      
      // encoded string parameter
      o = null;
      bos.reset();
      o = api.process(bos, "http://localhost:8888/api/messages/runtime/getService/\"runtime\"");
      assertTrue(o == runtime);
      retJson = new String(bos.toByteArray());
      log.info("ret - {}", retJson);
      
      // with user id
      o = null;
      bos.reset();
      o = api.process(bos, "http://userid:passwd@localhost:8888/api/messages/runtime/getService/runtime");
      assertTrue(o == runtime);
      retJson = new String(bos.toByteArray());
      log.info("ret - {}", retJson);
      Message m = CodecUtils.fromJson(retJson, Message.class);
      assertTrue(m.getClass() == Message.class);
      assertTrue(m.data.length == 1);
      

      // with user id
      o = null;
      o = api.process(bos, "http://userid:passwd@localhost:8888/api/service/runtime/start/servo/Servo");
      assertTrue(Servo.class == o.getClass());
      retJson = new String(bos.toByteArray());
      log.info("ret - {}", retJson);


      // messages with encoded data
      o = api.process(bos, "/api/messages", encoded.toByteArray());
      assertTrue(String.class == o.getClass());
      // CodecUtils.fromJson(retJson);
      log.info("ret - {}", o);
     
      bos.reset();
      o = api.process(bos, "/api/messages/", encoded.toByteArray());
      log.info("ret - {}", o);
      bos.reset();
      // uri always has precedence over data
      o = api.process(bos, "/api/messages/runtime/getService/runtime");
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      o = api.process(bos, "/api/messages/runtime/start/servo/Servo"); // FIXME
                                                                       // check
                                                                       // encoded
                                                                       // or not
                                                                       // encoded
                                                                       // values
                                                                       // for
                                                                       // params
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      o = api.process(bos, "/api/messages/servo");
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      o = api.process(bos, "/api/messages/servo/");
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      // =============== api messages end =========================

      // =============== api service begin =========================
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
      
      // wrongApi test
      /*  FIXME !!!!
      o = null;
      o = api.process(bos, "/api/wrongApi");
      log.info("ret - {}", new String(bos.toByteArray()));
      List<ApiDescription> apis = (List<ApiDescription>)o;
      assertTrue(apis.size() > 1);
      retJson = new String(bos.toByteArray());
      log.info("ret - {}", retJson);
      */
      
      o = api.process(bos, "/api/service");
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      o = api.process(bos, "/api/service/"); // FIXME -
                                              // list
                                              // service
                                              // names/types
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      o = api.process(bos, "/api/service/runtime/getUptime");
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      o = api.process(bos, "/api/service/runtime/getService/runtime");
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      o = api.process(bos, "/api/service/runtime/getService/runtime");
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      o = api.process(bos, "/api/service/runtime/start/servo/Servo");
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      o = api.process(bos, "/api/service/servo");
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      o = api.process(bos, "/api/service/servo/");
      log.info("ret - {}", new String(bos.toByteArray()));
      bos.reset();
      // o = api.process(bos,
      // "/api/service/runtime/noWorky/GroG");
      // log.info("ret - {}", new String(bos.toByteArray()));
      // bos.reset();
      // =============== api service end =========================

    } catch (Exception e) {
      log.error("main threw", e);
    }

   //  Runtime.exit();
  }

}
