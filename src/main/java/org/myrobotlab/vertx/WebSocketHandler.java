package org.myrobotlab.vertx;

import java.lang.reflect.Method;
import java.util.UUID;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.DescribeQuery;
import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.MethodCache;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.net.Connection;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.ServerWebSocket;

/**
 * Minimal Handler for all websocket messages coming from the react js client.
 * 
 * TODO - what else besides text messages - websocket binary streams ??? text
 * stream ?
 * 
 * @author GroG
 *
 */
public class WebSocketHandler implements Handler<ServerWebSocket> {

  public final static Logger log = LoggerFactory.getLogger(WebSocketHandler.class);

  /**
   * reference to the MRL Vertx service / websocket and http server
   */
  transient private org.myrobotlab.service.Vertx service = null;

  /**
   * reference to the websocket text message handler
   */
  TextMessageHandler textMessageHandler = null;

  public static class TextMessageHandler implements Handler<String> {

    private transient org.myrobotlab.service.Vertx service = null;

    /**
     * incoming message count
     */
    protected int msgCnt = 0;

    /**
     * session uuid
     */
    protected String uuid = null;

    protected boolean newConnection = true;
    
    public TextMessageHandler(org.myrobotlab.service.Vertx service, String uuid) {
      this.service = service;
      this.uuid = uuid;
    }

    @Override
    public void handle(String json) {
      log.info("handling {}", json);

      msgCnt++;
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
        // ServiceInterface si = Runtime.getService(msg.name);
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
        // si.in(msg);
        // si.send(msg); <- interesting MockGateway delegates this and it sends a send
        
        // the way to do it
        // no direct handling of a service
        // no direct invoking
        // just send a message after decoding
        service.send(msg);

        if (newConnection) {
        // New Connection 1st message
        // we automatically send a "describe" to the remote (after sending an
        // addListener("describe")
        // to describe the new client - and in the process send our process
        // "id" name

        MRLListener listener = new MRLListener("describe", String.format("runtime@%s", service.getId()), "onDescribe");
        Message subscribe = Message.createMessage(service.getFullName(), "runtime", "addListener", listener);
        // Default serialization to json/text is to json encode the parameter
        // list
        // then json encode the message
        service.sendRemote(subscribe);

        // FIXME get rid of fill-uuid
        Message describe = Message.createMessage(String.format("%s@%s", service.getName(), Runtime.get().getId()),
            "runtime", "describe",
            new Object[] { "fill-uuid", new DescribeQuery(Runtime.getInstance().getId(), uuid) });
        service.sendRemote(describe);
        log.info(String.format("<-- %s", describe));
        newConnection = false;
         }

      } catch (Exception e) {
        service.error(e);
      }
    }
  }

  public WebSocketHandler(org.myrobotlab.service.Vertx service) {
    this.service = service;
  }

  @Override
  public void handle(ServerWebSocket socket) {
    // FIXME - get "id" from js client - need something unique from the js
    // client
    MultiMap headers = socket.headers();
    String uri = socket.uri();

    // FIXME - get "id" from js client - need something unique from the js
    // client
    // String id = r.getRequest().getParameter("id");
    // String id = String.format("vertx-%s", service.getName());

    // FIXME - not correct - but needed for testing
    String id = String.format("webgui-client", service.getName());

    // String uuid = UUID.randomUUID().toString();
    String uuid = socket.binaryHandlerID();

    Connection connection = new Connection(uuid, id, service.getName());
    connection.put("c-type", service.getSimpleName());
    connection.put("gateway", service.getName());
    connection.putTransient("websocket", socket);
    Runtime.getInstance().addConnection(uuid, id, connection);
    // ctx.writeTextMessage("ping"); FIXME - query ?
    // FIXME - thread-safe ? how many connections mapped to objects ?

    socket.textMessageHandler(new TextMessageHandler(service, uuid));
    log.info("new ws connection {}", uuid);

    socket.closeHandler(close -> {
      log.info("closing {}", socket.binaryHandlerID());
      Runtime.getInstance().removeConnection(socket.binaryHandlerID());
    });

  }

}
