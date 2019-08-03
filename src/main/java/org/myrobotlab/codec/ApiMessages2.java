package org.myrobotlab.codec;

import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.myrobotlab.codec.ApiFactory.ApiDescription;
import org.myrobotlab.framework.HelloRequest;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.WebGui;
import org.slf4j.Logger;

public class ApiMessages2 extends Api {

  public final static Logger log = LoggerFactory.getLogger(ApiMessages2.class);

  // API MESSAGES
  @Override
  public Object process(WebGui webgui, String apiKey, String uri, String uuid, OutputStream out, String json) throws Exception {

    Object retobj = null;

    if (json != null) {
      // json = json.trim();
      // json message has precedence
      Codec codec = CodecFactory.getCodec(CodecUtils.MIME_TYPE_JSON);

      if (log.isDebugEnabled() && json != null) {
        log.debug("data - [{}]", json);
      }

      Message msg = CodecUtils.fromJson(json, Message.class);

      if (msg == null) {
        log.error("msg is null {}", json);
        return null;
      }

      if (webgui == null) {
        log.error("sender cannot be null for {}", ApiMessages2.class.getSimpleName());
        return null;
      }

      if (msg.sender == null) {
        msg.sender = webgui.getName();
      }

      ServiceInterface si = Runtime.getService(msg.name);
      if (si == null) {
        log.error("could not get service {} for msg {}", msg.name, msg);
        return null;
      }

      Class<?> clazz = si.getClass();

      Class<?>[] paramTypes = null;
      Object[] params = null;
      // decoded array of encoded parameters
      Object[] encodedArray = null;

      if (msg.data == null) {
        params = new Object[0];
        encodedArray = params;
      } else {
        params = new Object[msg.data.length];
        encodedArray = msg.data;
      }
      
      // FIXME - you can start making changes now for getHelloResponse to be called with more parameters, authorization, etc.

      paramTypes = MethodCache.getCandidateOnOrdinalSignature(si.getClass(), msg.method, encodedArray.length);

      if (log.isDebugEnabled()) {
        StringBuffer sb = new StringBuffer(String.format("(%s)%s.%s(", clazz.getSimpleName(), msg.name, msg.method));
        for (int i = 0; i < paramTypes.length; ++i) {
          if (i != 0) {
            sb.append(",");
          }
          sb.append(paramTypes[i].getSimpleName());
        }
        sb.append(")");
        log.debug(sb.toString());
      }

      // WE NOW HAVE ORDINAL AND TYPES
      params = new Object[encodedArray.length];

      // DECODE AND FILL THE PARAMS
      for (int i = 0; i < params.length; ++i) {
        params[i] = codec.decode(encodedArray[i], paramTypes[i]);
      }

      Method method = clazz.getMethod(msg.method, paramTypes);

      if (si.isLocal()) {
        log.debug("{} is local", si.getName());

        log.debug("{}.{}({})", msg.name, msg.method, Arrays.toString(params));
        retobj = method.invoke(si, params);
        
        // propagate return data to subscribers
        si.out(msg.method, retobj);
        
        // 2019.07.30 new feature - echoing back on "message" protocol if a valid outstream exists and is local
        // anything remote will be async message and will require a blocking subscriber if blocking is required
        if (out != null) {
          out.write(CodecUtils.toJson(retobj).getBytes());
        }
      } else {
        log.debug("{} is remote", si.getName());
        // TODO - inspect if blocking ...
        webgui.send(msg.name, msg.method, params);
      }

      MethodCache.cache(clazz, method);

    } else {
      // WE ARE IN THE INITIAL "GET" - no payload should be with /api/messages2 - 
      // it "could" be possible to send /api/messages2/getHelloResponse/(bunch of data) - but not worth is
      // So, a remote system has initiated contact, we will initiate "our" HelloResponse(HelloRequest)
      Message msg = Message.createMessage(webgui, webgui.getName(), "getHelloResponse", new Object[] { new HelloRequest(Runtime.getId(), uuid) });      
      out.write(CodecUtils.toJson(msg).getBytes());
    }
    return retobj;
  }
  

  public static ApiDescription getDescription() {
    ApiDescription desc = new ApiDescription("message", "{scheme}://{host}:{port}/api/messages", "ws://localhost:8888/api/messages",
        "An asynchronous api useful for bi-directional websocket communication, primary messages api for the webgui.  URI is /api/messages data contains a json encoded Message structure");
    return desc;
  }

}
