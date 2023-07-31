package org.myrobotlab.service;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.MethodCache;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.mqtt.MqttMsg;
import org.myrobotlab.net.Connection;
import org.myrobotlab.service.config.MqttBrokerConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.interfaces.Gateway;
import org.myrobotlab.service.interfaces.KeyConsumer;
import org.slf4j.Logger;

import io.moquette.broker.Server;
import io.moquette.broker.config.MemoryConfig;
import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.InterceptAcknowledgedMessage;
import io.moquette.interception.messages.InterceptConnectMessage;
import io.moquette.interception.messages.InterceptConnectionLostMessage;
import io.moquette.interception.messages.InterceptDisconnectMessage;
import io.moquette.interception.messages.InterceptMessage;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.moquette.interception.messages.InterceptSubscribeMessage;
import io.moquette.interception.messages.InterceptUnsubscribeMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;

/**
 * Embedded moquette mqtt broker. It has the same capability of a standard mqtt
 * broker in addition it has an interface to the mrl pub/sub framework.
 * 
 * The mrl framework is accessible through a topic with a prefix. Default prefix
 * is "mrl". The topic at that point follows /{serviceName}/{method} with the
 * payload being a json array with encoded parameters e.g.
 * /mrl/clock01/setInterval with payload ['2000']
 * 
 * this will invoke clock01.setInterval(Integer ms) Make note: the parameter
 * list values are encoded, that is why its '2000' vs 2000, because first the
 * whole array is decoded - then each parameter is, similar to http form values
 *
 */
public class MqttBroker extends Service implements InterceptHandler, Gateway, KeyConsumer {

  public final static Logger log = LoggerFactory.getLogger(MqttBroker.class);

  private static final long serialVersionUID = 1L;

  protected static final String HASH_SHA_256 = "SHA-256";

  private static String bytesToHex(byte[] hash) {
    StringBuilder hexString = new StringBuilder(2 * hash.length);
    for (int i = 0; i < hash.length; i++) {
      String hex = Integer.toHexString(0xff & hash[i]);
      if (hex.length() == 1) {
        hexString.append('0');
      }
      hexString.append(hex);
    }
    return hexString.toString();
  }

  private final MqttMsg toMqttMsg(InterceptMessage msg) {
    MqttMsg retMsg = new MqttMsg();
    if (msg instanceof InterceptPublishMessage) {
      InterceptPublishMessage m = (InterceptPublishMessage) msg;
      retMsg.setClientID(m.getClientID());
      retMsg.setUsername(m.getUsername());
      retMsg.setTopicName(m.getTopicName());
      ByteBuf b = m.getPayload();
      b.hasArray();
      String payload = b.hasArray() ? new String(m.getPayload().array(), UTF_8) : b.toString(UTF_8);
      retMsg.setPayload(payload);

    } else if (msg instanceof InterceptAcknowledgedMessage) {
      InterceptAcknowledgedMessage m = (InterceptAcknowledgedMessage) msg;
      retMsg.setUsername(m.getUsername());

    } else if (msg instanceof InterceptConnectionLostMessage) {
      InterceptConnectionLostMessage m = (InterceptConnectionLostMessage) msg;
      retMsg.setClientID(m.getClientID());
      retMsg.setUsername(m.getUsername());

    } else if (msg instanceof InterceptConnectMessage) {
      InterceptConnectMessage m = (InterceptConnectMessage) msg;
      retMsg.setClientID(m.getClientID());
      retMsg.setUsername(m.getUsername());

    } else if (msg instanceof InterceptDisconnectMessage) {
      InterceptDisconnectMessage m = (InterceptDisconnectMessage) msg;
      retMsg.setClientID(m.getClientID());
      retMsg.setUsername(m.getUsername());

    } else if (msg instanceof InterceptSubscribeMessage) {
      InterceptSubscribeMessage m = (InterceptSubscribeMessage) msg;
      retMsg.setClientID(m.getClientID());
      retMsg.setUsername(m.getUsername());
      retMsg.setTopicName(m.getTopicFilter());

    } else if (msg instanceof InterceptUnsubscribeMessage) {
      InterceptUnsubscribeMessage m = (InterceptUnsubscribeMessage) msg;
      retMsg.setClientID(m.getClientID());
      retMsg.setUsername(m.getUsername());
      retMsg.setTopicName(m.getTopicFilter());
    }
    return retMsg;
  }

  protected String address = "0.0.0.0";

  protected Boolean allow_zero_byte_client_id = false;

  protected Set<String> connectedClients = new HashSet<>();

  /**
   * determines if the broker will attempt to process any messages published on
   * the api topics .. typically api/service and api/messages
   */
  boolean processApiMessages = true;

  protected boolean listening = false;

  transient final Server mqttBroker = new Server();

  protected Integer mqttPort = 1883;

  protected String mrlTopicApiPrefix = "api";

  protected String messagesTopic = mrlTopicApiPrefix + "/" + CodecUtils.API_MESSAGES + "/";

  protected String serviceTopic = mrlTopicApiPrefix + "/" + CodecUtils.API_SERVICE + "/";

  protected Map<String, List<MRLListener>> notifyList = new HashMap<>();

  protected String password = null;

  protected String passwordFilePath = getDataDir(MqttBroker.class.getSimpleName()) + fs + "password_file.conf";

  protected String username = null;

  protected Integer wsPort = 8080;

  public MqttBroker(String n, String id) {
    super(n, id);

    // restore keys if they exist
    Security security = Security.getInstance();
    username = security.getKey(getName() + ".username");
    password = security.getKey(getName() + ".password");
  }

  // FIXME - more than one type of gateway ... client gateway and server gateway  
  @Override
  public void connect(String uri) throws Exception {
    // Mqtt Brokers do not "connect" to other instances
    // NOOP
  }

  public String getAddress() {
    return address;
  }

  @Override
  public List<String> getClientIds() {
    return Runtime.getInstance().getConnectionUuids(getName());
  }

  @Override
  public Map<String, Connection> getClients() {
    return Runtime.getInstance().getConnections(getName());
  }

  @Override
  public String getID() {
    return getName();
  }

  @Override
  public Class<?>[] getInterceptedMessageTypes() {
    Class<?>[] ALL_MESSAGE_TYPES = { InterceptConnectMessage.class, InterceptDisconnectMessage.class, InterceptConnectionLostMessage.class, InterceptPublishMessage.class,
        InterceptSubscribeMessage.class, InterceptUnsubscribeMessage.class, InterceptAcknowledgedMessage.class };

    return ALL_MESSAGE_TYPES;
  }

  public Integer getMqttPort() {
    return mqttPort;
  }

  public String getPassword() {
    return password;
  }

  public String getUsername() {
    return username;
  }

  public Integer getWsPort() {
    return wsPort;
  }

  @Override
  public boolean isLocal(Message msg) {
    return Runtime.getInstance().isLocal(msg);
  }

  public void listen() {
    listen(address, mqttPort, wsPort, username, password, allow_zero_byte_client_id);
  }

  public void listen(int mqttPort) {
    listen(address, mqttPort, wsPort, username, password, allow_zero_byte_client_id);
  }

  public void listen(String address, int mqttPort, int wsPort, String username, String password, boolean allow_zero_byte_client_id) {
    try {

      this.address = (address == null) ? "0.0.0.0" : address;
      this.mqttPort = mqttPort;
      this.wsPort = wsPort;
      this.username = username;
      this.password = password;
      this.allow_zero_byte_client_id = allow_zero_byte_client_id;

      if (listening) {
        info("broker already started - stop first to start again");
        return;
      }

      Properties props = new Properties();
      props.setProperty("port", mqttPort + "");
      props.setProperty("websocket_port", wsPort + "");
      props.setProperty("host", address);
      props.setProperty("password_file", passwordFilePath);

      // false to accept only client connetions with credentials
      // true to accept client connection without credentails, validating only
      // the one that provides
      if (username != null && username.length() > 0) {
        saveIdentities();
        props.setProperty("allow_anonymous", "false");
      } else {
        props.setProperty("allow_anonymous", "true");
      }

      props.setProperty("allow_zero_byte_client_id", String.format("%b", allow_zero_byte_client_id));
      props.setProperty("netty.mqtt.message_size", "1048576");

      MemoryConfig mc = new MemoryConfig(props);
      Collections.singletonList(this);
      mqttBroker.startServer(mc, Collections.singletonList(this));

      listening = true;
      broadcastState();
      log.info("broker started");
    } catch (Exception e) {
      error(e);
    }
  }

  /**
   * New Mqtt device connected, not the same as a mrl connection which goes
   * through a registration process
   */
  @Override
  public void onConnect(InterceptConnectMessage msg) {
    invoke("publishConnect", msg);
    // are connections from generic devices to be handled as mrl connections ?
    // if so, the addConnection should be here ?
    connectedClients.add(msg.getClientID());
    broadcastState();
  }

  @Override
  public void onConnectionLost(InterceptConnectionLostMessage msg) {
    invoke("publishConnectionLost", msg);
    connectedClients.remove(msg.getClientID());

    // are connections from generic devices to be handled as mrl connections ?
    Runtime runtime = Runtime.getInstance();
    runtime.removeConnection(msg.getClientID());

    broadcastState();
  }

  @Override
  public void onDisconnect(InterceptDisconnectMessage msg) {
    invoke("publishDisconnect", msg);
    connectedClients.remove(msg.getClientID());
    // are connections from generic devices to be handled as mrl connections ?
    Runtime runtime = Runtime.getInstance();
    runtime.removeConnection(msg.getClientID());

    broadcastState();
  }

  @Override
  public void onMessageAcknowledged(InterceptAcknowledgedMessage msg) {
    invoke("publishMessageAcknowledged", msg);
  }

  /**
   * Callback from embedded broker. Every message published from external mqtt
   * client goes through here
   */
  @Override
  public void onPublish(InterceptPublishMessage im) {
    // endless loop from self publish !
    // FIXME - filter out self published msgs
    log.info("topic " + im.getTopicName());

    MqttMsg m = null;
    try {
      m = toMqttMsg(im);
      invoke("publishMqttMsg", m);
      log.info("publishMqtt {}", m);
      // parse topic name
      String topic = m.getTopicName();

      // don't let broker process messages
      if (processApiMessages && topic.startsWith(serviceTopic)) {
        String mrlUri = "/" + topic.substring((serviceTopic).length());
        // FIXME - should they all be full name ?
        Message msg = CodecUtils.pathToMsg(getFullName(), mrlUri);
        String payload = m.getPayload();
        if (payload != null && payload.length() > 0) {
          msg.data = CodecUtils.decodeArray(payload);
        }

        MethodCache cache = MethodCache.getInstance();

        if (isLocal(msg)) {
          String serviceName = msg.getFullName();// getName();
          Class<?> clazz = Runtime.getClass(serviceName);
          Object[] params = cache.getDecodedJsonParameters(clazz, msg.method, msg.data);
          msg.data = params;

          // ahh .. how nice and easy is returning data from a synchronous call
          // ... :)
          // too bad its not as powerful as sending an asynchronous message
          // Object ret = invoke(msg);
          // send(msg) - send is "optimized" to invoke, must have "out(msg) to
          // get the pre-processor hook
          out(msg);

        } else {
          // TODO - send it on its way - possibly do not decode the parameters
          // this would allow it to traverse mrl instances which did not
          // have the class definition !
          send(msg);
        }

        // TODO - check if subscribed
        if (notifyList.containsKey(topic)) {
          List<MRLListener> listeners = notifyList.get(topic);
          for (MRLListener listener : listeners) {
            msg.name = listener.callbackName;
            msg.method = listener.callbackMethod;
          }
          send(msg);
        }

      }
    } catch (Exception e) {
      error(e);
    }
  }

  @Override
  public void onSubscribe(InterceptSubscribeMessage m) {
    invoke("publishSubscribe", m);
    try {
      String topic = m.getTopicFilter();
      if (topic != null && topic.length() > 0 && topic.charAt(0) != '$' && topic.charAt(0) != '/') {
        topic = '/' + topic;
      }

      String[] parts = topic.split("/");

      if (processApiMessages && parts.length == 5 && parts[1].equals(mrlTopicApiPrefix) && parts[2].equals("service")) {

        subscribe(parts[3], parts[4], m.getClientID(), CodecUtils.getCallbackTopicName(parts[4]));
        // subscribe(parts[2], parts[3], getName(), parts[3]);
        // subscribe(parts[2], parts[3], m.getClientID(), parts[3]);
      }
    } catch (Exception e) {
      error(e);
    }

    broadcastState();
  }

  @Override
  public void onUnsubscribe(InterceptUnsubscribeMessage msg) {
    invoke("publishUnsubscribe", msg);
  }

  @Override
  public boolean preProcessHook(Message msg) {
    // let the messages for this service
    // get processed normally
    if (methodSet.contains(msg.method)) {
      return true;
    }
    // otherwise its target is for the
    // scripting environment
    // set the data - and call the call-back function
    // handling call-back input needs to be
    // done by another thread - in case its doing blocking
    // or is executing long tasks - the inbox thread needs to
    // be freed of such tasks - it has to do all the inbound routing

    // FIXME - NOT NEEDED OR WANTED ?
    MqttPublishMessage message = MqttMessageBuilders.publish().topicName(mrlTopicApiPrefix + "/" + msg.getFullName() + "/" + msg.getMethod()).retained(true)
        .qos(MqttQoS.AT_LEAST_ONCE).payload(Unpooled.copiedBuffer(CodecUtils.toJson(msg).getBytes(UTF_8))).build();

    mqttBroker.internalPublish(message, null); // I need the client id ? why?
    return false;
  }

  public MqttMsg publishConnect(InterceptConnectMessage msg) {
    return toMqttMsg(msg);
  }

  public MqttMsg publishConnectionLost(InterceptConnectionLostMessage msg) {
    return toMqttMsg(msg);
  }

  public MqttMsg publishDisconnect(InterceptDisconnectMessage msg) {
    return toMqttMsg(msg);
  }

  public MqttMsg publishMessageAcknowledged(InterceptAcknowledgedMessage msg) {
    return toMqttMsg(msg);
  }

  public MqttMsg publishMqttMsg(MqttMsg msg) {
    return msg;
  }

  public MqttMsg publishSubscribe(InterceptSubscribeMessage msg) {
    return toMqttMsg(msg);
  }

  public MqttMsg publishUnsubscribe(InterceptUnsubscribeMessage msg) {
    return toMqttMsg(msg);
  }

  private void saveIdentities() {
    try {
      if (username != null && username.length() > 0) {
        // the "right" way - save it to secure store
        setKey(getName() + ".username", username);
        setKey(getName() + ".password", password);

        // the "wrong" way - but moquette forces this - no way of setting
        // username/pwd in memory programmatically :(
        FileOutputStream fos = new FileOutputStream(passwordFilePath);
        MessageDigest digest = MessageDigest.getInstance(HASH_SHA_256);
        byte[] encodedhash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
        // String credline = String.format(username + ":" +
        // StringUtil.bytesToHex(encodedhash));
        String credline = String.format(username + ":" + bytesToHex(encodedhash));
        fos.write(credline.getBytes());
        fos.close();
      }
    } catch (Exception e) {
      error(e);
    }
  }

  @Override
  public void sendRemote(Message msg) throws Exception {

    // add our id - we don't want to see it again
    msg.addHop(getId());

    // uni-cast mode - all clients have their own id
    Connection c = Runtime.getInstance().getRoute(msg.getId());
    /*
     * MqttPublishMessage message =
     * MqttMessageBuilders.publish().topicName(mrlMessageTopicPrefix + "/" +
     * msg.getFullName() + "/" + msg.getMethod()).retained(true) // cuz
     * .qos(MqttQoS.EXACTLY_ONCE).payload(Unpooled.copiedBuffer(CodecUtils.
     * toJson(msg).getBytes(UTF_8))).build();
     * 
     */

    MqttPublishMessage message = MqttMessageBuilders.publish().topicName(messagesTopic + "/" + msg.getId()).retained(false).qos(MqttQoS.AT_LEAST_ONCE)
        .payload(Unpooled.copiedBuffer(CodecUtils.toJson(msg).getBytes(UTF_8))).build();

    mqttBroker.internalPublish(message, null); // I need the client id ? why?

  }

  public void setAddress(String address) {
    this.address = address;
  }

  public void setMqttPort(Integer mqttPort) {
    this.mqttPort = mqttPort;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void setWsPort(Integer wsPort) {
    this.wsPort = wsPort;
  }

  public void stopListening() {
    if (mqttBroker != null) {
      try {
        mqttBroker.stopServer();
      } catch (Exception e) {
        // don't care - moquette will throw if you stop it before starting
      }
    }
    connectedClients = new HashSet<>();
    listening = false;
    broadcastState();
  }

  @Override
  public void stopService() {
    super.stopService();
    stopListening();
  }

  public void subscribe(String mqttTopic, String callbackName, String callbackMethod) {
    MRLListener listener = new MRLListener(mqttTopic, callbackName, callbackMethod);
    if (notifyList.containsKey(listener.topicMethod)) {
      // iterate through all looking for duplicate
      boolean found = false;
      List<MRLListener> nes = notifyList.get(listener.topicMethod);
      for (int i = 0; i < nes.size(); ++i) {
        MRLListener entry = nes.get(i);
        if (entry.equals(listener)) {
          log.debug("attempting to add duplicate MRLListener {}", listener);
          found = true;
          break;
        }
      }
      if (!found) {
        log.debug("adding addListener from {}.{} to {}.{}", this.getName(), listener.topicMethod, listener.callbackName, listener.callbackMethod);
        nes.add(listener);
      }
    } else {
      List<MRLListener> nl = new CopyOnWriteArrayList<MRLListener>();
      nl.add(listener);
      log.debug("adding addListener from {}.{} to {}.{}", this.getName(), listener.topicMethod, listener.callbackName, listener.callbackMethod);
      notifyList.put(listener.topicMethod, nl);
    }
  }

  public void unsubscribe(String mqttTopic, String callbackName, String callbackMethod) {
    // FIXME - implement
  }

  public static void main(String[] args) {
    try {
      LoggingFactory.init("info");
      
      
      Runtime.main(new String[] {"--log-level", "info", "-s", "webgui", "WebGui", "intro", "Intro", "python", "Python"});
      

      boolean done = true;
      if (done) {
        return;
      }      
      
      Runtime.main(new String[] { "--id", "c2"});
      Python python = (Python) Runtime.start("python", "Python");

      python.exec("test_value = None\ndef test(msg):\n\tglobal test_value\n\ttest_value = msg\n\tprint(msg)");
      MqttBroker broker = (MqttBroker) Runtime.start("broker", "MqttBroker");
      broker.listen(1883);

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();

      Clock clock01 = (Clock) Runtime.start("clock01", "Clock");
      // clock01.startClock();
      
 


      Mqtt mqtt = (Mqtt) Runtime.start("mqtt02", "Mqtt");
      mqtt.setAutoConnect(false);
            
      mqtt.connect("mqtt://localhost:1883");
      // mqtt.connect("mqtt://test.mosquitto.org:1883");
      // mqtt.publish("mrl/");

      for (int i = 0; i < 10; ++i) {

        mqtt.subscribe("topic/echo");
        mqtt.subscribe("api/service/clock01/pulse");
        mqtt.publish("api/service/clock01/startClock");
        // check if clock started

        // mqtt.publish("arbitrary/topic", "hello " + i);
        // mqtt.publish("mrl/python/onMqttMsg", "ahoy !");
        mqtt.publish("api/service/clock01/startClock");
        mqtt.publish("api/service/python/exec/test('blah')");
        // mqtt.publish(String.format("api/service/python/exec/test('blah %d')",
        // i));
        long start = System.currentTimeMillis();
        Object o = python.waitFor("python", "finishedExecutingScript", 3000);
        log.info("delta time for mqtt localhost execution {} ms", System.currentTimeMillis() - start);
        String test_value = python.get("test_value").toString();
        log.info("test_value: {}", test_value);
        Service.sleep(100);

        mqtt.publish("api/service/python/set/test_value/3");
        test_value = python.get("test_value").toString();
        log.info("test_value: {}", test_value);
      }

      if (mqtt.isConnected()) {
        log.info("worky");
      }

      // broker.start();

    } catch (Throwable e) {
      log.error("wtf", e);
    }
  }

  @Override
  public String[] getKeyNames() {
    String username = getName() + ".username";
    String password = getName() + ".password";
    return new String[] { username, password };
  }

  @Override
  public void setKey(String keyName, String keyValue) {
    Security security = Security.getInstance();
    security.setKey(keyName, keyValue);
    broadcastState();
  }

  @Override
  public ServiceConfig getConfig() {
    MqttBrokerConfig c = (MqttBrokerConfig)super.getConfig();
    // FIXME - remove local fields in favor of just config
    c.address = address;
    c.mqttPort = mqttPort;
    c.wsPort = wsPort;
    c.username = username;
    c.password = password;
    return c;
  }

  @Override
  public ServiceConfig apply(ServiceConfig c) {
    MqttBrokerConfig config = (MqttBrokerConfig) super.apply(c);
    // FIXME - remove local fields in favor of just config
    address = config.address;
    mqttPort = config.mqttPort;
    wsPort = config.wsPort;
    username = config.username;
    password = config.password;
    return config;
  }

}
