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

  public Object process(MessageSender sender, OutputStream out, Message requestUri, String json) throws Exception {

    Object retobj = null;
    
    // FIXME - consider msg.data - if its not null !!!!
    
    // initial GET /api/messages - has data == null
    // ws always starts with a GET (no data)
    if (json != null) {
      // json message has precedence
      Codec codec = CodecFactory.getCodec(CodecUtils.MIME_TYPE_JSON);
      
      if (log.isDebugEnabled() && json != null) {
        log.debug("data - [{}]",json);
      }

      Message msg = CodecUtils.fromJson(json, Message.class);

      if (msg == null) {
        log.error(String.format("msg is null %s", json));
        return null;
      }

      if (sender == null) {
        log.error(String.format("sender cannot be null for %s", ApiMessages.class.getSimpleName()));
        return null;
      }

      // FIXME - unfortunately the message comes in as msg.sender = ""
      // but we should only have to test for null (bug on client)
      if (msg.sender == null || msg.sender.length() == 0){
        msg.sender = sender.getName();
      }
      
      // TODO - this is a registry provider / service provider
      // get the service or service description...
      ServiceInterface si = Runtime.getService(msg.name);
      if (si == null) {
        log.error("could not get service {} for msg {}", msg.name, msg);
        return null;
      }
      
      // convert message.data from json to pojos
      // based on target's methods signature
      
      // if local invoke
      
      // if remote send
      
      // Message Api is "double" encoded json data
      
      Class<?> clazz = si.getClass();

      Class<?>[] paramTypes = null;
      Object[] params = null;
      // decoded array of encoded parameters
      Object[] encodedArray = null;
      
      if (msg.data == null){
        params = new Object[0];
        encodedArray = params;       
      } else {
        params = new Object[msg.data.length];
        encodedArray = msg.data;
      }

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

      // FIXME FIXME FIXME !!!!
      // Service.invoke needs to use method cach BUT - internal queues HAVE
      // type information
      // AND decoded json DOES NOT - needs to be optimized such that it knows
      // the encoding
      // before using the method cache - and the "hint" determines
      // getBestCanidate !!!!

      // log.info("{}.{}({})", msg.name, msg.method,
      // Arrays.toString(paramTypes));

      Method method = clazz.getMethod(msg.method, paramTypes);

      // NOTE --------------
      // strategy of find correct method with correct parameter types
      // "name" is the strongest binder - but without a method cache we
      // are condemned to scan through all methods
      // also without a method cache - we have to figure out if the
      // signature would fit with instanceof for each object
      // and "boxed" types as well

      // best to fail - then attempt to resolve through scanning through
      // methods and trying types - then cache the result

      // FIXME - not good - using my thread to execute another services
      // method and put its return on the the services out queue :P
      if (si.isLocal()) {
        log.debug("{} is local", si.getName());

        log.debug("{}.{}({})", msg.name, msg.method, Arrays.toString(params));
        retobj =  method.invoke(si, params);
        // use Service.invoke since that will broadcast to any subscribers
        // Object retobj = si.invoke(msg.name, params);

        // FIXME - Is this how to support synchronous ?
        // What does this mean ?
        // respond(out, codec, method.getName(), ret);

        // propagate return data to subscribers 
        si.out(msg.method, retobj);
      } else {
        log.debug("{} is remote", si.getName());
        // TODO - inspect if blocking ...
        sender.send(msg.name, msg.method, params);
      }

      MethodCache.cache(clazz, method);
      
    } else {
      // First GET /api/messages - has data == null !
      // use different api to process GET ?
      // return hello ?
      // FALLBACK 
      ApiFactory api = ApiFactory.getInstance();
      String newUri = requestUri.uri.replace("/messages", "/service");
      // we send out = null, because we don't want service api to stream back a 'non' message response
      // but we do want the functionality of the services api
      retobj = api.process(sender, null, newUri, json);
     
      // FIXME - WebGui Client is expecting 
      // FIXME - WebGui Angular FIX is needed - this IS NOT getLocalServices its getEnvironments
      
      // Create msg from the return - and send it back
      // - is this correct ? should it be double encoded ?
      Message msg = Message.createMessage(sender, sender.getName(), "onLocalServices", new Object[]{retobj});
      // apiKey == messages api uses JSON
      Codec codec = CodecFactory.getCodec(CodecUtils.MIME_TYPE_JSON);
      codec.encode(out, msg);
      
    }
    return retobj;
  }
  
  public static ApiDescription getDescription() {
    ApiDescription desc = new ApiDescription("message", "{scheme}://{host}:{port}/api/messages", "ws://localhost:8888/api/messages",
        "An asynchronous api useful for bi-directional websocket communication, primary messages api for the webgui.  URI is /api/messages data contains a json encoded Message structure");
    return desc;
  }

}
