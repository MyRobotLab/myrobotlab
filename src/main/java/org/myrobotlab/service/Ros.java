package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.lucene.search.TimeLimitingCollector.TimeExceededException;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.WsClient;
import org.myrobotlab.service.config.RosConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.interfaces.ConnectionEventListener;
import org.myrobotlab.service.interfaces.RemoteMessageHandler;
import org.slf4j.Logger;

import okhttp3.Response;
import okhttp3.WebSocket;

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
public class Ros extends Service<RosConfig> implements RemoteMessageHandler, ConnectionEventListener {

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

  /**
   * This operation is used to subscribe to a ROS topic. The ROS bridge server
   * will forward any messages received on the topic to the client.
   */
  static final public String OP_SUBSCRIBE = "subscribe";
  /**
   * This operation is used to unsubscribe from a ROS topic. The ROS bridge
   * server will stop forwarding messages received on the topic to the client.
   */
  static final public String OP_UNSUBSCRIBE = "unsubscribe";

  /**
   * This operation is used to publish a message to a ROS topic. The ROS bridge
   * server will forward the message to any subscribers of the topic.
   */
  static final public String OP_PUBLISH = "publish";
  /**
   * This operation is used to call a ROS service. The ROS bridge server will
   * forward the request to the service and return the response to the client.
   */
  static final public String OP_CALL_SERVICE = "call_service";

  /**
   * This operation is used to advertise a ROS topic. The ROS bridge server will
   * create a new topic with the specified name and forward any messages
   * received on the topic to any subscribers.
   */
  static final public String OP_ADVERTISE = "advertise";

  /**
   * This operation is used to stop advertising a ROS topic. The ROS bridge
   * server will stop forwarding messages received on the topic to any
   * subscribers.
   */
  static final public String OP_UNADVERTISE = "unadvertise";

  static public class RosMsg {
    public List<Object> args;
    public String compression = "none"; /* was optional :P - none | bz2 | zlib */
    public String id; /* optional id */
    public Object msg;
    public String op; // publish | subscribe | ? call_service ?
    public String service;
    public String topic;
    /**
     * return (optional) from service_call
     */
    public Object values;

    // public String toString() {
    // return CodecUtils.toJson(this);
    // }
  }

  static public class RosServiceCallback {
    public String id;
    // FIXME - change type to RosMsg
    public Object msg;
  }

  public final static Logger log = LoggerFactory.getLogger(Ros.class);
  private static final long serialVersionUID = 1L;

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);
    try {
      Ros ros = (Ros) Runtime.start("ros", "Ros");

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      // webgui.setSsl(true);
      webgui.autoStartBrowser(false);
      webgui.setPort(8888);
      // webgui.setSsl(true);
      webgui.startService();

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  Map<String, RosServiceCallback> callbacks = new HashMap<>();

  transient private WsClient client = null;

  protected boolean connected = false;

  public Ros(String n, String id) {
    super(n, id);
  }

  @Override
  public RosConfig apply(RosConfig c) {
    super.apply(c);
    if (c.connect) {
      connect(c.bridgeUrl);
      if (c.subscriptions != null) {
        for (String topic : c.subscriptions) {
          rosSubscribe(topic);
        }
      }
    }
    return c;
  }

  // FIXME - TODO reconnect
  public void connect(String url) {

    if (connected) {
      info("already connected");
      return;
    }

    client = new WsClient();
    client.connect(this, url);
  }

  // TODO - getTopics
  // getSubscriptions
  // setNodeName ?
  //

  public void disconnect() {
    if (client != null) {
      client.close();
    }
  }

  @Override
  public RosConfig getConfig() {
    super.getConfig();
    config.connect = connected;
    return config;
  }

  public List<String> getSubscriptions() {
    return ((RosConfig) config).subscriptions;
  }

  public List<String> getTopics() {

    RosServiceCallback ret = rosCallService("/rosapi/topics");
    if (ret == null) {
      return null;
    }
    RosMsg msg = (RosMsg) ret.msg;
    if (msg != null) {
      @SuppressWarnings("unchecked")
      List<String> topics = (List<String>) ((Map) msg.values).get("topics");
      log.info(ret.toString());
      return topics;
    } else {
      return new ArrayList<>();
    }
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
      msg.op = OP_CALL_SERVICE;
      msg.service = service;
      
      sendJson(CodecUtils.toJson(msg));
      synchronized (callback) {
        
        long startTime = System.currentTimeMillis();
        long remainingTime = c.serviceCallTimeoutMs;
        while (true) {
            try {
                callback.wait(remainingTime);
                break;
            } catch (InterruptedException e) {
                remainingTime = c.serviceCallTimeoutMs - (System.currentTimeMillis() - startTime);
                if (remainingTime <= 0) {
                    break;
                }
            }
        }
      }
      return callback; // callbacks.get(id);

    } catch (TimeExceededException ex) {
      warn("timeout exceeded on ros service call");
    } catch (Exception e) {
      error(e);
    }
    return null;
  }

  public void sendJson(String json) {
    try {
      if (client == null) {
        error("client not connected");
        return;
      }
      client.send(json);
    } catch (Exception e) {
      error(e);
    }
  }

  public void rosSendMsg(RosMsg msg) {
    String json = CodecUtils.toJson(msg);
    sendJson(json);
  }

  public void rosPublish(String topic, String json) {
    RosMsg msg = new RosMsg();
    msg.op = OP_PUBLISH;
    msg.topic = topic;
    msg.msg = CodecUtils.fromJson(json);
    String msgJson = CodecUtils.toJson(msg);
    sendJson(msgJson);
  }

  public void rosSubscribe(String topic) {
    try {
      RosConfig c = (RosConfig) config;
      RosMsg msg = new RosMsg();
      msg.op = OP_SUBSCRIBE;
      msg.topic = topic;
      sendJson(CodecUtils.toJson(msg));
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
      msg.op = OP_UNSUBSCRIBE;
      msg.topic = topic;
      client.send(CodecUtils.toJson(msg));
      if (c.subscriptions == null) {
        c.subscriptions = new ArrayList<>();
      }
      c.subscriptions.remove(topic);
    } catch (Exception e) {
      error(e);
    }
  }

  @Override
  public void onRemoteMessage(String uuid, String msg) {

    try {
      
      RosMsg rosMsg = CodecUtils.fromJson(msg, RosMsg.class);
      
      if (rosMsg == null) {
        error("decoding ros msg is null %s", msg);
        return;
      }
      
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
    } catch (Exception ex) {
      error(ex);
    }
  }

  @Override
  public void onOpen(WebSocket webSocket, Response response) {
    connected = true;
    broadcastState();
  }

  @Override
  public void onClosing(WebSocket webSocket, int code, String reason) {
    connected = false;
    broadcastState();
  }

  @Override
  public void onFailure(WebSocket webSocket, Throwable t, Response response) {
    error("websocket failure");
    log.error("onFailure", new Exception(t));
  }

}
