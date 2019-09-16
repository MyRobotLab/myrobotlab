package org.myrobotlab.codec;

import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.myrobotlab.codec.ApiFactory.ApiDescription;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.interfaces.MessageSender;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

public class ApiMessages extends Api {

  public final static Logger log = LoggerFactory.getLogger(ApiMessages.class);

  // API MESSAGES
  @Override
  public Object process(MessageSender webgui, String apiKey, String uri, String uuid, OutputStream out, String json) throws Exception {

    Object retobj = null;

    if (json != null) {
      // json = json.trim();
      // json message has precedence

      if (log.isDebugEnabled() && json != null) {
        log.debug("data - [{}]", json);
      }

      Message msg = CodecUtils.fromJson(json, Message.class);

      if (msg == null) {
        log.error("msg is null {}", json);
        return null;
      }

      if (webgui == null) {
        log.error("sender cannot be null for {}", ApiMessages.class.getSimpleName());
        return null;
      }

      if (msg.sender == null) {
        msg.sender = webgui.getName();
      }

      ServiceInterface si = Runtime.getService(msg.getName());
      if (si == null) {
        log.error("could not get service {} for msg {}", msg.getName(), msg);
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

      paramTypes = MethodCache.getCandidateOnOrdinalSignature(si.getClass(), msg.method, encodedArray.length);

      if (log.isDebugEnabled()) {
        StringBuffer sb = new StringBuffer(String.format("(%s)%s.%s(", clazz.getSimpleName(), msg.getName(), msg.method));
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
        params[i] = CodecUtils.fromJson((String)encodedArray[i], paramTypes[i]);
      }

      Method method = clazz.getMethod(msg.method, paramTypes);

      if (si.isLocal()) {
        log.debug("{} is local", si.getName());

        log.debug("{}.{}({})", msg.getName(), msg.method, Arrays.toString(params));
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
        webgui.send(msg.getFullName(), msg.method, params);
      }

      MethodCache.cache(clazz, method);

    } else {
      // FIXME - this is terrible ! .. changing to a different api ??? wtf ???
      // First GET /api/messages - has data == null !
      // use different api to process GET ?
      // return hello ?
      // FALLBACK
      apiKey = "service";
      Api api = ApiFactory.getApiProcessor(apiKey);
      String newUri = uri.replace("/messages", "/service");
      // we send out = null, because we don't want service api to stream back a
      // 'non' message response
      // but we do want the functionality of the services api
      retobj = api.process(webgui, apiKey, newUri, uuid, out, json);//(sender, null, newUri, json);

      // FIXME - WebGui Client is expecting
      // FIXME - WebGui Angular FIX is needed - this IS NOT getLocalServices its
      // getEnvironments

      // Create msg from the return - and send it back
      // - is this correct ? should it be double encoded ?
      Message msg = Message.createMessage(webgui.getName(), webgui.getName(), "onLocalServices", new Object[] { retobj });
      // apiKey == messages api uses JSON
      
      CodecUtils.toJson(out, msg);

    }
    return retobj;
  }
  

  public static ApiDescription getDescription() {
    ApiDescription desc = new ApiDescription("message", "{scheme}://{host}:{port}/api/messages", "ws://localhost:8888/api/messages",
        "An asynchronous api useful for bi-directional websocket communication, primary messages api for the webgui.  URI is /api/messages data contains a json encoded Message structure");
    return desc;
  }

}
