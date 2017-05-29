package org.myrobotlab.codec;

import java.io.OutputStream;
import java.net.URI;

import org.myrobotlab.framework.Message;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.interfaces.MessageSender;
import org.slf4j.Logger;

public class ApiProcessorMessages implements ApiProcessor {
  
  public final static Logger log = LoggerFactory.getLogger(ApiProcessorMessages.class);

  
  public Object process(MessageSender sender, OutputStream out, URI uri, byte[] data){
    
    // endpoint/uri has precedence over data payload
    String[] parts = uri.getPath().split("/");
    
    // OR should you be allowed to construct a message from a GET ?
    if (parts.length != 3 || data == null){
      // FIXME - handle return error
      log.error("parts.length != 3  or data null");
      return null;
    }
    
    if (log.isDebugEnabled() && data != null){
      log.debug("data -[{}]", new String(data));
    }
    
    String json = new String(data);
    Message msg = CodecUtils.fromJson(json, Message.class);
   
    if (msg == null) {
      log.error(String.format("msg is null %s", json));
      return null;
    }
    
    // out(msg);
    if (sender == null){
      log.error(String.format("sender cannot be null for %s", ApiProcessorMessages.class.getSimpleName()));
      return null;
    }
    
    sender.send(msg.name, msg.method, msg.data);
 
    return null;
  }
  
}
