package org.myrobotlab.codec;

import java.io.OutputStream;
import java.net.URI;

import org.myrobotlab.framework.Message;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.interfaces.MessageSender;
import org.slf4j.Logger;

public class ApiProcessorMessages implements ApiProcessor {

  public final static Logger log = LoggerFactory.getLogger(ApiProcessorMessages.class);

  public Object process(MessageSender sender, OutputStream out, URI uri, byte[] data) throws Exception {

    // endpoint/uri has precedence over data payload
    String[] parts = uri.getPath().split("/");

    // OR should you be allowed to construct a message from a GET ?
    if (parts.length < 3) {
      // FIXME - handle return error
      log.error("uri parts.length < 3 e.g. [/api/messages]");
      return null;
    }

    // first GET /api/messages - has data == null
    if (data != null) {
      if (log.isDebugEnabled() && data != null) {
        log.debug("data -[{}]", new String(data));
      }

      String json = new String(data);
      Message msg = CodecUtils.fromJson(json, Message.class);

      if (msg == null) {
        log.error(String.format("msg is null %s", json));
        return null;
      }

      // out(msg);
      if (sender == null) {
        log.error(String.format("sender cannot be null for %s", ApiProcessorMessages.class.getSimpleName()));
        return null;
      }

      sender.send(msg.name, msg.method, msg.data);
    } else {
      // first GET /api/messages - has data == null
      // use different api to process GET ?
      // return hello ?
      ApiFactory api = ApiFactory.getInstance();
      String u = uri.toString().replace("/messages", "/services");
      URI newUri = new URI(u);
      api.process(sender, out, newUri, data);
    }

    return null;
  }

}
