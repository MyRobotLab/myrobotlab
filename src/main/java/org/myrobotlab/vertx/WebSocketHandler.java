package org.myrobotlab.vertx;

import java.lang.reflect.Method;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.MethodCache;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

import io.vertx.core.Handler;
import io.vertx.core.http.ServerWebSocket;

/**
 * 
 * TODO - what else besides text messages - websocket binary streams ???  text stream ?
 * 
 * @author GroG
 *
 */
public class WebSocketHandler implements Handler<ServerWebSocket> {
  
  public final static Logger log = LoggerFactory.getLogger(WebSocketHandler.class);

  transient private org.myrobotlab.service.Vertx service = null;
  TextMessageHandler textMessageHandler = null;
  
  public static class TextMessageHandler implements Handler<String> {
    
    org.myrobotlab.service.Vertx service = null;
    
    public TextMessageHandler(org.myrobotlab.service.Vertx service) {
      this.service = service;
    }

    @Override
    public void handle(String json) {
      log.info("handling {}", json);

      Method method;
      try {

        org.myrobotlab.framework.Message msg = CodecUtils.fromJson(json, org.myrobotlab.framework.Message.class);

        Class<?> clazz = Runtime.getClass(msg.name);
        if (clazz == null) {
          log.error("cannot derive local type from service {}", msg.name);
          return;
        }

        MethodCache cache = MethodCache.getInstance();
        Object[] params = cache.getDecodedJsonParameters(clazz, msg.method, msg.data);

        method = cache.getMethod(clazz, msg.method, params);
        if (method == null) {
          service.error("method cache could not find %s.%s(%s)", clazz.getSimpleName(), msg.method, msg.data);
          return;
        }

        ServiceInterface si = Runtime.getService(msg.name);
        Object ret = method.invoke(si, params);

        // put msg on mrl msg bus :)
        // service.in(msg); <- NOT DECODE PARAMS !!

        // if ((new Random()).nextInt(100) == 0) {
        // ctx.close(); - will close the websocket !!!
        // } else {
        // ctx.writeTextMessage("ping"); Useful is writing back
        // }

      } catch (Exception e) {
        service.error(e);
      }
    }    
  }
  
  public WebSocketHandler(org.myrobotlab.service.Vertx service) {
    this.service = service;
    this.textMessageHandler = new TextMessageHandler(service);
  }
  
  @Override
  public void handle(ServerWebSocket event) {

    // ctx.writeTextMessage("ping"); FIXME - query ?
    // FIXME - thread-safe ? how many connections mapped to objects ?
    event.textMessageHandler(new TextMessageHandler(service));
  
  }

}
