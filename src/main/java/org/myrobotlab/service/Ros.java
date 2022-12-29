package org.myrobotlab.service;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.lucene.search.TimeLimitingCollector.TimeExceededException;
import org.atmosphere.wasync.Client;
import org.atmosphere.wasync.ClientFactory;
import org.atmosphere.wasync.Decoder;
import org.atmosphere.wasync.Encoder;
import org.atmosphere.wasync.Event;
import org.atmosphere.wasync.Function;
import org.atmosphere.wasync.Request;
import org.atmosphere.wasync.RequestBuilder;
import org.atmosphere.wasync.Socket;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.RosConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.slf4j.Logger;

/**
 * Ros service uses websockets over the rosbridge
 * http://wiki.ros.org/rosbridge_suite
 * https://github.com/biobotus/rosbridge_suite/blob/master/ROSBRIDGE_PROTOCOL.md
 * 
 * 
 * ros topic prefix
 * 
 * 
 * http://robotwebtools.org/ - various js lib projects
 * 
 * @author GroG
 *
 */
public class Ros extends Service implements Decoder<String, Reader> {

  /**
   * @see https://github.com/biobotus/rosbridge_suite/blob/master/ROSBRIDGE_PROTOCOL.md
   * 
   *      <pre>
   *   advertise – advertise that you are publishing a topic
   *   unadvertise – stop advertising that you are publishing topic publish - a published ROS-message
   *   subscribe - a request to subscribe to a topic
   *   unsubscribe - a request to unsubscribe from a topic
   *   call_service - a service call
   *   advertise_service - advertise an external service server
   *   unadvertise_service - unadvertise an external service server
   *   service_request - a service request
   *   service_response - a service response
   *      </pre>
   * 
   * @author GroG
   *
   */

  static public class RosMsg {
    public List<Object> args;
    public String compression;
    public String id; /* optional id */
    public Object msg;
    public String op; // publish | subscribe | ? call_service ?
    public String service;
    public String topic;
    /**
     * return (optional) from service_call
     */
    public Object values;
    
    public String toString() {
      return CodecUtils.toJson(this);
    }
  }

  public class RosServiceCallback {
    public String id;
    public Object msg;
  }

  public final static Logger log = LoggerFactory.getLogger(Ros.class);
  private static final long serialVersionUID = 1L;

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);
    try {
      Ros ros = (Ros) Runtime.start("ros", "Ros");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  Map<String, RosServiceCallback> callbacks = new HashMap<>();

  @SuppressWarnings("rawtypes")
  transient private Client client = null;

  protected boolean connected = false;

  @SuppressWarnings("rawtypes")
  transient private RequestBuilder request = null;

  transient private Socket socket = null;

  public Ros(String n, String id) {
    super(n, id);
  }

  @Override
  public ServiceConfig apply(ServiceConfig c) {
    RosConfig config = (RosConfig) super.apply(c);
    if (config.connect) {
      connect(config.bridgeUrl);
      if (config.subscriptions != null) {
        for (String topic : config.subscriptions) {
          rosSubscribe(topic);
        }
      }
    }
    return c;
  }

  // FIXME - TODO reconnect
  public void connect(String url) {
    try {

      if (connected) {
        info("already connected");
        return;
      }

      client = ClientFactory.getDefault().newClient();

      request = client.newRequestBuilder().method(Request.METHOD.GET).uri(url).encoder(new Encoder<String, Reader>() {
        @Override
        public Reader encode(String s) {
          return new StringReader(s);
        }
      }).decoder(this).transport(Request.TRANSPORT.WEBSOCKET) // Try WebSocket
          .transport(Request.TRANSPORT.LONG_POLLING); // Fallback to
                                                      // Long-Polling

      socket = client.create();
      socket.on(new Function<Reader>() {
        @Override
        public void on(Reader r) {
          // log.error("r {}", r);
        }
      }).on(new Function<IOException>() {

        @Override
        public void on(IOException ioe) {
          error(ioe);
        }

      }).open(request.build())/* .fire("echo").fire("bong") */;
    } catch (Exception e) {
      error(e);
    }
  }

  // TODO - getTopics
  // getSubscriptions
  // setNodeName ?
  //

  @Override
  public Reader decode(Event e, String msg) {
    try {

      if (msg != null && "X".equals(msg)) {
        // System.out.println("MESSAGE - X");
        return null;
      }
      if ("OPEN".equals(msg)) {
        connected = true;
        broadcastState();
        return null;
      }

      if ("CLOSED".equals(msg)) {
        connected = false;
        broadcastState();
        return null;
      }

      RosMsg rosMsg = CodecUtils.fromJson(msg, RosMsg.class);
      if (rosMsg.id != null && rosMsg.service != null) {
        // service call response
        RosServiceCallback callback = callbacks.get(rosMsg.id);
        if (callback != null) {
          synchronized (callback) {
            callback.msg = rosMsg;
            callback.notifyAll();
          }
          callbacks.remove(rosMsg.id);
        } else {
          error("couldn't find callback for msg %s", rosMsg.id);
        }
      }
      invoke("publishRosMsg", rosMsg);

      log.error(msg);

      // main response
      // System.out.println(data);
      // for (RemoteMessageHandler handler : handlers) {
      // handler.onRemoteMessage(uuid, data);
      // }

      // response
      // System.out.println("OPENED" + s);
    } catch (Exception ex) {
      error(ex);
    }

    return new StringReader(msg);
  }

  public void disconnect() {
    if (socket != null) {
      socket.close();
    }
  }

  @Override
  public ServiceConfig getConfig() {
    RosConfig config = (RosConfig) super.getConfig();
    config.connect = connected;
    return config;
  }

  public List<String> getSubscriptions() {
    return ((RosConfig) config).subscriptions;
  }

  public List<String> getTopics() throws InterruptedException {

    RosServiceCallback ret = rosCallService("/rosapi/topics");
    if (ret == null) {
      return null;
    }
    RosMsg msg = (RosMsg)ret.msg;
    List topics = (List)((Map)msg.values).get("topics");
    log.info(ret.toString());
    return topics;
  }

  public RosMsg publishRosMsg(RosMsg msg) {
    return msg;
  }

  // FIXME - its a blocking call
  public RosServiceCallback rosCallService(String service) {
    try {
      RosConfig c = (RosConfig) config;
      RosServiceCallback callback = new RosServiceCallback();
      String id = UUID.randomUUID().toString();
      callback.id = id;
      callbacks.put(id, callback);

      RosMsg msg = new RosMsg();
      msg.id = id;
      msg.op = "call_service";
      msg.service = service;
      socket.fire(CodecUtils.toJson(msg));
      synchronized (callback) {
        callback.wait(c.serviceCallTimeoutMs);
      }
      return callback; //callbacks.get(id);

    } catch (TimeExceededException ex) {
      warn("timeout exceeded on ros service call");
    } catch (Exception e) {
      error(e);
    }
    return null;
  }

  public void rosPublish(String json) {
    try {
      // log.info(msg.getClass().getSimpleName());
      // socket.fire(CodecUtils.toJson(msg));
      socket.fire(json);
    } catch(Exception e) {
      error(e);
    }
  }
  
  public void rosPublish(String topic, Object data) {
      RosMsg msg = new RosMsg();
      msg.op = "publish";
      msg.topic = topic;
      msg.msg = data;
      rosPublish(topic, msg);
  }

  public void rosSendJson(String json) {
    try {
      socket.fire(json);
    } catch (Exception e) {
      error(e);
    }
  }
  
  public void rosSubscribe(String topic) {
    try {
      RosConfig c = (RosConfig) config;
      RosMsg msg = new RosMsg();
      msg.op = "subscribe";
      msg.topic = topic;
      socket.fire(CodecUtils.toJson(msg));
      if (c.subscriptions == null) {
        c.subscriptions = new ArrayList<>();
      }
      c.subscriptions.add(topic);
    } catch (Exception e) {
      error(e);
    }
  }

  public void rosUnsubscribe(String topic) {
    try {
      RosConfig c = (RosConfig) config;

      RosMsg msg = new RosMsg();
      msg.op = "unsubscribe";
      msg.topic = topic;
      socket.fire(CodecUtils.toJson(msg));
      if (c.subscriptions == null) {
        c.subscriptions = new ArrayList<>();
      }
      c.subscriptions.remove(topic);
    } catch (Exception e) {
      error(e);
    }
  }

}
