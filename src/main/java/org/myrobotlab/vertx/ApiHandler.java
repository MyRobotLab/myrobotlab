package org.myrobotlab.vertx;

import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.MethodCache;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.net.Connection;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

/**
 * Minimal Handler for all websocket messages coming from the react js client.
 * 
 * TODO - what else besides text messages - websocket binary streams ??? text
 * stream ?
 * 
 * @author GroG
 *
 */
public class ApiHandler implements Handler<RoutingContext> {

  public final static Logger log = LoggerFactory.getLogger(ApiHandler.class);

  /**
   * reference to the MRL Vertx service / websocket and http server
   */
  transient private org.myrobotlab.service.Vertx service = null;

  /**
   * reference to the websocket text message handler
   */
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

        // FIXME - probably shouldn't be invoking, probable should be putting
        // the message on the out queue ... not sure
        ServiceInterface si = Runtime.getService(msg.name);
        // Object ret = method.invoke(si, params);

        // put msg on mrl msg bus :)
        // service.in(msg); <- NOT DECODE PARAMS !!

        // if ((new Random()).nextInt(100) == 0) {
        // ctx.close(); - will close the websocket !!!
        // } else {
        // ctx.writeTextMessage("ping"); Useful is writing back
        // }

        // replace with typed parameters
        msg.data = params;
        // queue the message
        si.in(msg);

      } catch (Exception e) {
        service.error(e);
      }
    }
  }

  public ApiHandler(org.myrobotlab.service.Vertx service) {
    this.service = service;
    this.textMessageHandler = new TextMessageHandler(service);
  }

  @Override
  public void handle(RoutingContext rc) {

    HttpServerRequest request = rc.request();

    try {

      // FIXME - get "id" from js client - need something unique from the js
      // client
      // if (session) {
      // TODO - show all headers including client id
      
      String id = String.format("vertx-%s", service.getName());
      String uuid = UUID.randomUUID().toString();
      String verb = request.method().name().toLowerCase();

      /**
       * Do not want to do this until you manage a session, which includes
       * removing the session too.
       * <pre>
       * 
      Connection connection = new Connection(uuid, id, service.getName());
      connection.put("c-type", service.getSimpleName());
      connection.put("gateway", service.getName());

      Runtime.getInstance().addConnection(uuid, id, connection);
      *</pre>
      **/

      // content type always json for now
      request.response().putHeader("content-type", "application/json");

      // POST vs GET handler ?
      if ("get".equals(verb)) {
        handleGet(rc);
      } else if ("post".equals(verb)) {
        handlePost(rc);
      } else {
        log.warn("got a request to handle http verb {}", verb);
        return;
      }

      // CLOSED SESSION HOW TO HANDLE ?
      // socket.closeHandler(close -> {
      // log.info("closing {}", socket.binaryHandlerID());
      // Runtime.getInstance().removeConnection(socket.binaryHandlerID());
      // });

    } catch (Exception e) {

      // error message ?

      request.response().putHeader("content-type", "application/json");
      request.response().end("Hello from Vert.x API!");

    }

  }

  private void handlePost(RoutingContext rc) {

    HttpServerRequest request = rc.request();

    String path = URLDecoder.decode(request.path(), StandardCharsets.UTF_8);
    Message msg = CodecUtils.pathToMsg(service.getFullName(), path);

    // TODO - check if is a blocking request ...
    // handle double encoded vs single encoded with class
    request.bodyHandler(body -> {
      String bodyString = body.toString();
      msg.data = CodecUtils.fromJson(bodyString, Object[].class);
      log.info("{}", msg);

      if (service.isLocal(msg)) {
        Object ret = service.invoke(msg);
        request.response().end(CodecUtils.toJson(ret));
      } else {
        // TODO - send it on its way - possibly do not decode the parameters
        // this would allow it to traverse mrl instances which did not
        // have the class definition !
        service.send(msg);
      }
      // FIXME - remove connection ! AND/OR figure out session
    });

  }

  private void handleGet(RoutingContext rc) {
    HttpServerRequest request = rc.request();

    String path = URLDecoder.decode(request.path(), StandardCharsets.UTF_8);
    Message msg = CodecUtils.pathToMsg(service.getFullName(), path);

    msg = CodecUtils.decodeMessageParams(msg);

    if (service.isLocal(msg)) {
      Object ret = service.invoke(msg);
      request.response().end(CodecUtils.toJson(ret));
    } else {
      // TODO - send it on its way - possibly do not decode the parameters
      // this would allow it to traverse mrl instances which did not
      // have the class definition !
      service.send(msg);
    }
    // FIXME - remove connection ! AND/OR figure out session
  }

}
