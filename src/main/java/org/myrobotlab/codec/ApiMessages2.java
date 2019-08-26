package org.myrobotlab.codec;

import java.io.OutputStream;

import org.myrobotlab.codec.ApiFactory.ApiDescription;
import org.myrobotlab.framework.HelloRequest;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.MethodEntry;
import org.myrobotlab.framework.interfaces.MessageSender;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

public class ApiMessages2 extends Api {

  public final static Logger log = LoggerFactory.getLogger(ApiMessages2.class);

  // API MESSAGES
  @Override
  public Object process(MessageSender webgui, String apiKey, String uri, String uuid, OutputStream out, String json) throws Exception {

    Object retobj = null;

    if (json != null) {

      if (log.isDebugEnabled()) {
        log.debug("data - [{}]", json);
      }

      // decoding 1st pass - decodes the containers
      Message msg = CodecUtils.fromJson(json, Message.class);
      msg.setProperty("uuid", uuid);
      
      
      ServiceInterface si = Runtime.getService(msg.name);
      
      // decode the parameters
      if (msg.data != null) {
        MethodEntry me = si.getMethodMap().get(msg.method);
      }

      if (si.isLocal()) {
        log.debug("{} is local", si.getName());

        retobj = si.invoke(msg);
        
        // propagate return data to subscribers
        si.out(msg.method, retobj);
        
        // 2019.07.30 new feature - echoing back on "message" protocol if a valid outstream exists and is local
        // anything remote will be async message and will require a blocking subscriber if blocking is required
        /** NOT IN MSG FORMAT !!!
        if (out != null) {
          out.write(CodecUtils.toJson(retobj).getBytes());
        }
        */
      } else {
        log.debug("{} is remote", si.getName());
        // TODO - inspect if blocking ...
        // FIXME - TODO - default route !!
        webgui.send(msg);
      }


    } else {
      // WE ARE IN THE INITIAL "GET" - no payload should be with /api/messages2 - 
      // it "could" be possible to send /api/messages2/getHelloResponse/(bunch of data) - but not worth is
      // So, a remote system has initiated contact, we will initiate "our" HelloResponse(HelloRequest)
      // FIXME double encode !!!
      // FIXME - should this be clientRemote.fire ???
      // encode parameters - encode msg container !!
      Message msg = Message.createMessage(webgui, "runtime", "getHelloResponse", new Object[] {"fill-uuid", CodecUtils.toJson(new HelloRequest(Runtime.getId(), uuid)) });      
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
