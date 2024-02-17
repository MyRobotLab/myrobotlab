package org.myrobotlab.service;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLSocketFactory;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.DescribeQuery;
import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.MethodCache;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.mqtt.MqttMsg;
import org.myrobotlab.net.Connection;
import org.myrobotlab.net.SslUtil;
import org.myrobotlab.service.config.MqttConfig;
import org.myrobotlab.service.interfaces.Gateway;
import org.myrobotlab.service.interfaces.KeyConsumer;
import org.slf4j.Logger;

/**
 * <pre>
 * Mqtt - Mqtt is a machine-to-machine (M2M)/"Internet of Things" connectivity
 * protocol. It was designed as an extremely lightweight publish/subscribe
 * messaging transport. It is useful for connections with remote locations where
 * a small code footprint is required and/or network bandwidth is at a premium.
 * http://mqtt.org/
 * 
 * AWS IoT Core - differs in spec from MQTT 3.1.1
 * https://docs.aws.amazon.com/iot/latest/developerguide/mqtt.html "AWS IoT
 * doesn't support retained messages. If a request is made to retain messages,
 * the connection is disconnected"
 * 
 * Two or more instances of mrl can connect to one another on a Mqtt Broker.
 * They first subscribe to broadcast topics onConnect and onDisconnnect
 * Then they will send an onConnect msg at a recurring interval to annouce
 * to all connected instances they are "still" connected.
 * 
 * A Mrl Connection is created for each mrl instance connected this way. Routing 
 * information is updated from each connection since an instance can determine where
 * the incoming message came from by the topic 
 * 
 *  The recv topic between two instances c1 and c2 are
 *  mrl/gw/c1/rx&lt;-c2  for c1
 *  mrl/gw/c2/rx&lt;-c1  for c2
 *  
 *  a general recv topic exist
 *  mrl/gw/c1/rx for c1
 *  mrl/gw/c2/rx for c2
 *  
 * &#64;author kmcgerald and GroG
 * </pre>
 */
public class Mqtt extends Service<MqttConfig> implements MqttCallback, IMqttActionListener, Gateway, KeyConsumer {

  public final static Logger log = LoggerFactory.getLogger(Mqtt.class);

  private static final long serialVersionUID = 1L;

  protected boolean autoConnect = true;

  protected boolean autoReconnect = true;

  protected Long broadcastConnectPingIntervalMs = null;

  protected boolean cleanSession = true; // Non durable subscriptions

  protected transient MqttAsyncClient client;

  protected String clientId = String.format("%s@%s", getName(), getId());

  boolean connected = false;

  boolean connecting = false;

  protected transient MqttConnectOptions conOpt = new MqttConnectOptions();

  /**
   * Prefix under which the mrl pub/sub tree will be published If null there
   * will be defaulted and the topic structure will be
   * mrl/{serviceName}/{method} &lt;-- data array json parameters [ p0, p1, p2,
   * ...]
   */
  protected String mrlTopicApiPrefix = "mrl";
  /**
   * an instance connecting will send a connect message to this topic
   */
  protected String onConnectTopic = "mrl/onConnect";

  /**
   * an instance disconnecting will send a disconnect message to this topic
   * consider leveraging LWT
   */
  protected String onDisconnectTopic = "mrl/onDisconnect";

  protected String password = null;

  /**
   * for persistence of message if not sent immediately types include memory and
   * file persistence
   */
  protected transient MemoryPersistence persistence;

  /**
   * <pre>
   * 0 - at most once (fastest most unreliable) 
   * 1 - at least once (fast - guaranteed at least 1 - possible dupes - most common)
   * 2 - exactly once (slowest - one and only one)
   * </pre>
   */
  protected int qos = 1;

  boolean retain = false;

  /**
   * reference to runtime route table
   */
  // protected transient RouteTable routeTable = null;

  protected transient Runtime runtime = null;

  protected int rxCount;

  protected String sslCaFilePath = null;

  protected String sslCertFilePath = null;

  protected String sslKeyFilePath = null;

  protected String sslPassword = null;

  /**
   * registry of merged instances of the form
   * 
   * <pre>
   * { 
   *   "{fullname}": "{type}",
   *    "web01@admin":"WebGui",
   *    ...   *
   * </pre>
   */
  // protected String gatewayTopic = "mrl/gateway";

  protected Set<String> subscriptions = new HashSet<String>();

  protected int txCount;

  /**
   * Two types of connection are supported tcp:// for a TCP connection and
   * ssl:// , which is weird, that its not mqtt:// and mqtts:// :P
   * tcp://broker.hivemq.com:1883 is valid
   */
  protected String url = "mqtt://localhost:1883";

  /**
   * not sure what this is supposed to be - perhaps a back-end id to identify
   * the transaction on the listener
   */
  protected String userContext = "context";

  protected String username = null;

  public Mqtt(String n, String id) {
    super(n, id);
    try {
      runtime = Runtime.getInstance();
      // routeTable = runtime.getRouteTable();
      conOpt.setCleanSession(cleanSession);
      clientId = String.format("%s@%s", getName(), getId());
    } catch (Exception e) {
      error("codec failed to initialize");
      log.error("codec failed to initialize", e);
    }
    // connector = new MqttConnector(this);
  }

  public void autoSubscribe(boolean b) {
    autoConnect = b;
  }

  public void broadcastConnect() {
    try {

      Message onConnect = Message.createMessage(String.format("%s@%s", getName(), getId()), null, "onConnect", new Object[] { Platform.getLocalInstance() });
      onConnect.sendingMethod = "onConnect";
      sendRemote(onConnect);

    } catch (MqttException ex) {
      try {
        if (ex.getReasonCode() == 32104) {
          log.warn("client is not connected - attempting to reconnect");
          client.reconnect();
        } else {
          log.error("unknown error", ex);
        }
      } catch (Exception e) {
        log.error("reconnect threw", e);
      }
    } catch (Exception e) {
      error(e);
    }
  }

  @Override
  public void connect(String url) {
    connect(url, null, null, null);
  }

  public void connect() {
    connect(null, null, null, null);
  }

  synchronized public void connect(String inUrl, String inClientId, String inUsername, String inPassword) {
    try {

      if (inUrl != null) {
        url = inUrl;
      }
      if (inClientId != null) {
        clientId = inClientId;
      }
      if (inUsername != null) {
        username = inUsername;
      }
      if (inPassword != null) {
        password = inPassword;
      }

      persistence = new MemoryPersistence();

      URI uri = new URI(url);
      String scheme = uri.getScheme();
      if (scheme.equalsIgnoreCase("mqtts")) {
        url = "ssl" + url.substring(5);
      } else if (scheme.equalsIgnoreCase("mqtt")) {
        url = "tcp" + url.substring(4);
      }
      client = new MqttAsyncClient(url, clientId, persistence);

      if (username != null && username.length() > 0) {
        conOpt.setUserName(username);
        conOpt.setPassword(password.toCharArray());
      }

      client.setCallback(this);

      // onOpt.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
      // conOpt.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);

      if (sslCertFilePath != null && sslKeyFilePath != null) {
        SSLSocketFactory socketFactory = SslUtil.getSocketFactory(sslCaFilePath, sslCertFilePath, sslKeyFilePath, sslPassword);
        conOpt.setSocketFactory(socketFactory);
      }

      // default is 10 - which drops messages on a fast local network :(
      conOpt.setMaxInflight(1000);

      client.connect(conOpt, userContext, this);

      int i = 0;

      // wait for a CONNACK !
      while (!client.isConnected() && i < 10 /* (autoReconnect || i < 10) */) {
        sleep(500);
        i += 1;
      }

      // DO THIS ON CONNECTED EVENT !!!- does it exist
      if (client.isConnected()) {
        connected = true;
        info("connected to %s", url);

        // subscribe now
        if (autoConnect) {
          // announce to all we have connected
          broadcastConnect();
          // broadcast every 10 seconds
          if (broadcastConnectPingIntervalMs != null) {
            addTask(broadcastConnectPingIntervalMs, "broadcastConnect");
          }

          // subscribe to the announce connect topic
          subscribe(onConnectTopic); // TODO - use LWT topics !
          subscribe(onDisconnectTopic);

          // subscribe to "general" rx topic
          String rxTopic = String.format("mrl/gw/%s/rx", getId());
          info("creating general rx topic %s", rxTopic);
          subscribe(rxTopic); // TODO - use LWT topics !
        }

        broadcastState();
        return;
      } else {

        error("could not connect to %s", url);
        connected = false;
        if (!autoReconnect) {
          broadcastState();
          return;
        }
      }

      broadcastState();

    } catch (Exception e) {
      log.error("connect failed", e);
    }

    log.info("stopping mqtt connection to {}", url);
    connected = false;
    connecting = false;
  }

  /**
   * @see MqttCallback#connectionLost(Throwable)
   */
  @Override
  public void connectionLost(Throwable cause) {
    try {
      info("diconnect from " + url + " " + cause.toString());
      // Called when the connection to the server has been lost.
      // An application may choose to implement reconnection
      // logic at this point. This sample simply exits.
      if (autoReconnect) {
        client.reconnect();
      }
    } catch (Exception e) {
      error(e);
    }
  }

  /**
   * @see MqttCallback#deliveryComplete(IMqttDeliveryToken)
   */
  @Override
  public void deliveryComplete(IMqttDeliveryToken token) {
    // log.debug("Delivery complete callback: Publish Completed " +
    // Arrays.toString(token.getTopics()));
  }

  public void disconnect() {
    try {
      // user requested disconnect
      autoReconnect = false;
      connected = false;
      if (client.isConnected()) {
        client.disconnect();
      }
      broadcastState();
    } catch (Exception e) {
      error(e);
    }
  }

  public Long getBroadcastConnectPingInterval() {
    return broadcastConnectPingIntervalMs;
  }

  @Override
  public List<String> getClientIds() {
    return Runtime.getInstance().getConnectionUuids(getName());
  }

  @Override
  public Map<String, Connection> getClients() {
    return Runtime.getInstance().getConnections(getName());
  }

  public String getPassword() {
    return password;
  }

  public int getQos() {
    return qos;
  }

  public String getSslCaFilePath() {
    return sslCaFilePath;
  }

  public String getSslCertFilePath() {
    return sslCertFilePath;
  }

  public String getSslKeyFilePath() {
    return sslKeyFilePath;
  }

  public String getSslPassword() {
    return sslPassword;
  }

  public Set<String> getSubscriptions() {
    return subscriptions;
  }

  public String getUrl() {
    return url;
  }

  public String getUserContext() {
    return userContext;
  }

  public String getUsername() {
    return username;
  }

  public boolean isAutoConnect() {
    return autoConnect;
  }

  public boolean isConnected() {
    return connected;
  }

  @Override
  public boolean isLocal(Message msg) {
    return Runtime.getInstance().isLocal(msg);
  }

  /**
   * @see MqttCallback#messageArrived(String, MqttMessage) process inbound mqtt
   *      message TODO - integration with mrlPrefixTopic and auto registration !
   */
  @Override
  public void messageArrived(String topic, MqttMessage message) throws MqttException {

    try {

      rxCount++;

      // convert to standard mrl pojo mqtt msg
      MqttMsg mqtt = new MqttMsg();
      byte[] bits = message.getPayload();
      if (bits != null) {
        mqtt.setPayload(new String(message.getPayload()));
      }
      mqtt.setTopicName(topic);
      mqtt.setMessageType("PUBLISH");
      mqtt.setQosLevel(message.getQos());
      mqtt.setPacketId(message.getId());

      // publish it
      invoke("publishMqttMsg", mqtt);

      // FIXME - new String won't be happy with true binary payloads ..
      String payload = new String(message.getPayload());

      Message msg = null;
      if (topic.startsWith(mrlTopicApiPrefix)) {
        msg = CodecUtils.fromJson(payload, Message.class);

        // I never want to see a msg again which I have sent away !
        if (msg.containsHop(getId())) {
          log.warn("got msg I sent away - loopback - dumping");
          return;
        }

        String logMsg = String.format("mqtt <--rx-- %s (%s.%s <--invoke-- %s.%s) qos %d id %d", topic, msg.getFullName(), msg.getMethod(), msg.getSrcFullName(), msg.sendingMethod,
            message.getQos(), message.getId());
        /*
         * if (logMsg.
         * equals("mqtt <--rx-- mrl/gw/c1/rx<-w1 (runtime@c1.onDescribe <--invoke-- runtime@w1.describe) qos 1 id 3"
         * )) { log.info("here"); }
         */
        log.warn(logMsg);
      } else {
        log.warn(String.format("mqtt <--rx-- %s qos %d id %d", topic, message.getQos(), message.getId()));
      }

      if (msg != null) {
        /**
         * A new "connection" is made from this topic, because the broker is a
         * dumb "hub".. and we want to support non-mrl mqtt brokers. This means
         * a "connection" is really establishing a channel over the broker to
         * other mrl instances. The broker nor the connection to the broker need
         * any "special" logic. Mrl instances establish connections and routes
         * based on unique receive topics (rx&lt;-{service}@{id}) they subscribe
         * to.
         */
        if (topic.equals(onConnectTopic)) {

          String remoteId = msg.getSrcId();
          // includes gateway {service}@{id}
          String remoteFullName = msg.getSrcFullName();

          if (runtime.getConnectionFromId(remoteId) != null) {
            log.warn("already have channel to {} - dumping channel request", remoteId);
            return;
          }

          // dynamic routing
          log.warn("found new sender {} - adding adding new connection and route", remoteFullName);

          // FIXME combine Connection, runtime.AddTable, all the stuffs - put in
          // routeTable
          // FIXME combine again put in AbstractGateway !

          String uuid = java.util.UUID.randomUUID().toString();
          // String id = CodecUtils.getId(msg.sender);
          Connection connection = new Connection(uuid, remoteId, getName());
          connection.put("type", getSimpleName());
          connection.put("c-type", getSimpleName());
          connection.put("remote-gateway", remoteFullName);

          // create a unique channel for the remote connecting instance
          String rxTopic = String.format("mrl/gw/%s/rx<-%s", getFullName(), remoteFullName);
          subscribe(rxTopic);
          log.warn("subscribed to topic {}", rxTopic);

          connection.put("recv-topic", rxTopic);

          // local gateway key is a key which this gateway can use to get
          // meta-data or connection data outside the actual message
          // important when sending messages via egress to us it with the
          // routingTable
          // to select the correct "interface/connection" to begin sending the
          // msg
          // routeTable.addLocalGatewayKey(getName() + " " + rxTopic, uuid);
          runtime.addLocalGatewayKey(getName() + " " + rxTopic, uuid);
          runtime.addConnection(uuid, remoteId, connection);

          // something is listening - i need to let them know I'm alive -
          // broadcast to onConnect
          broadcastConnect();
          // wait for them to get setup with appropriate subscription
          sleep(1500);

          // The following are partially Registry/Shadow related

          // 1. subscribe to describe
          MRLListener listener = new MRLListener("describe", runtime.getFullName(), "onDescribe");
          Message subscribe = Message.createMessage(getFullName(), "runtime@" + remoteId, "addListener", listener);
          subscribe.sendingMethod = "onConnect";
          sendRemote(subscribe);

          // 2. subscribe to registered
          listener = new MRLListener("registered", runtime.getFullName(), "onRegistered");
          subscribe = Message.createMessage(getFullName(), "runtime@" + remoteId, "addListener", listener);
          subscribe.sendingMethod = "onConnect";
          sendRemote(subscribe);

          // 3. subscribe to released
          listener = new MRLListener("released", runtime.getFullName(), "onReleased");
          subscribe = Message.createMessage(getFullName(), "runtime@" + remoteId, "addListener", listener);
          subscribe.sendingMethod = "onConnect";
          sendRemote(subscribe);

          // 4. describe new instance for me
          // FIXME why isn't this using Gateway.getDescribeMessage()?
          Message describe = Message.createMessage(String.format("%s@%s", getName(), getId()), "runtime@" + remoteId, "describe",
              new Object[] { Gateway.FILL_UUID_MAGIC_VAL, new DescribeQuery(Runtime.getInstance().getId(), uuid) });
          describe.sendingMethod = "onConnect";
          sendRemote(describe);

        } else if (topic.startsWith(mrlTopicApiPrefix + "/")) {

          // check to see if route table needs updating
          String remoteId = msg.getSrcId();

          // check if the route exists
          if (!runtime.containsRoute(remoteId)) {
            // FIXME implement 1st in routing table, then in AbstractGateway
            // add new route to the routeTable - we found a new "id" - it came
            // over interface x - we'll add
            // a x -> id route entry, in order to do so we need to pull back the
            // uuid of the connection/interface
            String uuid = runtime.getConnectionUuidFromGatewayKey((getName() + " " + topic));
            runtime.addRoute(remoteId, uuid, 10);
          }

          if (isLocal(msg)) {

            //////////////// BEGIN RUNTIME CODEBLOCK////////////////////
            String serviceName = msg.getName();
            // to decode fully we need class name, method name, and an array of
            // json
            // encoded parameters
            MethodCache cache = MethodCache.getInstance();
            Class<?> clazz = Runtime.getClass(serviceName);
            if (clazz == null) {
              log.error("local msg but no Class for requested service {}", serviceName);
              return;
            }
            Object[] params = cache.getDecodedJsonParameters(clazz, msg.method, msg.data);

            Method method = cache.getMethod(clazz, msg.method, params);
            ServiceInterface si = Runtime.getService(serviceName);
            if (method == null) {
              log.error("cannot find {}", cache.makeKey(clazz, msg.method, cache.getParamTypes(params)));
              return;
            }
            if (si == null) {
              log.error("si null for serviceName {}", serviceName);
              return;
            }

            Object ret = method.invoke(si, params);

            // propagate return data to subscribers
            si.out(msg.method, ret);

            //////////////// END RUNTIME CODEBLOCK//////////////////////
          } else {
            log.info("GATEWAY {} RELAY {} --to--> {}.{}", getName(), msg.sender, msg.name, msg.method);
            send(msg);
          }

        } // else if (topic.startsWith(mrlTopicApiPrefix + "/"))
      }
    } catch (Exception e) {
      // error("tried processing msg from topic [%s] into a message", topic, e);
      error("could not process mqtt msg");
      log.error("processing mqtt msg threw", e);
    }
  }

  @Override
  public void onFailure(IMqttToken token, Throwable throwable) {
    log.error("error - {} {}", tokenToString(token), throwable);
  }

  @Override
  public void onSuccess(IMqttToken token) {
    // log.info("success - {} ", tokenToString(token));
  }

  @Override
  public boolean preProcessHook(Message m) {
    if (methodSet.contains(m.method)) {
      // process the message like a regular service
      return true;
    }
    /*
     * NOT READY YET !!! try { publish(outTopic, 2, CodecUtils.toJson(m)); }
     * catch (Exception e) { log.error("publish threw", e); }
     */
    return false;
  }

  /**
   * publish a null message to a topic - used for "events"
   * 
   * @param topic
   *          t
   * 
   */
  public void publish(String topic) {
    publish(topic, null);
  }

  public void publish(String topicName, String payload) {
    publish(topicName, payload, qos, false);
  }

  public void publish(String topicName, String payload, int qos) {
    publish(topicName, payload, qos, false);
  }

  public void publish(String topicName, String payload, int qos, boolean retain) {
    byte[] bytes = (payload != null) ? payload.getBytes() : null;
    publishBytes(topicName, qos, bytes, retain);
  }

  public void publishBytes(String topic, Integer qos, byte[] payload, boolean retain) {
    try {
      if (!connected) {
        error("not connected");
        return;
      }

      log.info("{} mqtt --publish--> {} qos {} retain {}", getName(), topic, qos, retain);
      // FIXME - see if user context is like a backend id ...
      if (payload == null) {
        payload = new byte[0];
      }
      client.publish(topic, payload, qos, retain, userContext, this);
      txCount++;
    } catch (Exception e) {
      log.error("publishBytes threw", e);
    }
  }

  public MqttMsg publishMqttMsg(MqttMsg msg) {
    return msg;
  }

  @Override
  public void sendRemote(Message msg) throws Exception {

    String remoteRxTopic = null;

    // FIXME put all stuff (isLocal) in routeTable
    // FIXME put this in AbstractGateway

    if (msg.getId() == null && msg.getMethod().equals("onConnect")) {
      remoteRxTopic = onConnectTopic;
    } else {
      // sent to specific connection on rx channel
      // remote id may use "connected" route of different remote id
      // multiplex to the appropriate channel

      Connection conn = runtime.getRoute(msg.getId());
      String rxId = conn.getId();
      Connection connection = runtime.getConnectionFromId(rxId);
      String remoteFullName = (String) connection.get("remote-gateway");
      remoteRxTopic = String.format("mrl/gw/%s/rx<-%s", remoteFullName, getFullName());
    }

    // I never want to see a msg again which I have sent away !
    msg.addHop(getId());

    log.warn("mqtt --tx--> {} ({}.{}) qos {} retain {}", remoteRxTopic, msg.getFullName(), msg.getMethod(), qos, retain);
    publish(remoteRxTopic, CodecUtils.toJsonMsg(msg), qos, retain);

  }

  public void setAutoConnect(boolean autoConnect) {
    this.autoConnect = autoConnect;
  }

  public boolean setAutoReconnect(boolean b) {
    this.autoReconnect = b;
    return b;
  }

  public void setBroadcastConnectPingInterval(Long broadcastConnectPingIntervalMs) {
    this.broadcastConnectPingIntervalMs = broadcastConnectPingIntervalMs;
  }

  public void setCert(String sslCaFilePath, String sslCertFilePath, String sslKeyFilePath) {
    setCert(sslCaFilePath, sslCertFilePath, sslKeyFilePath, null);
  }

  public void setCert(String sslCaFilePath, String sslCertFilePath, String sslKeyFilePath, String sslPassword) {
    this.sslCaFilePath = sslCaFilePath;
    this.sslCertFilePath = sslCertFilePath;
    this.sslKeyFilePath = sslKeyFilePath;
    this.sslPassword = sslPassword;

  }

  public void setClientId(String cId) {
    clientId = cId;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setQos(int q) {
    qos = q;
  }

  public void setSslCaFilePath(String sslCaFilePath) {
    this.sslCaFilePath = sslCaFilePath;
  }

  public void setSslCertFilePath(String sslCertFilePath) {
    this.sslCertFilePath = sslCertFilePath;
  }

  public void setSslKeyFilePath(String sslKeyFilePath) {
    this.sslKeyFilePath = sslKeyFilePath;
  }

  public void setSslPassword(String sslPassword) {
    this.sslPassword = sslPassword;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public void setUserContext(String userContext) {
    this.userContext = userContext;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void subscribe(String topic) throws MqttException {
    subscribe(topic, qos);
  }

  /**
   * Subscribe to a topic on an Mqtt server Once subscribed this method waits
   * for the messages to arrive from the server that match the subscription. It
   * continues listening for messages until the enter key is pressed. {
   * 
   * @param topic
   *          to subscribe to (can be wild carded)
   * 
   * @param qos
   *          the maximum quality of service to receive messages at for this
   *          subscription
   * @throws MqttException
   *           boom
   */
  public void subscribe(String topic, int qos) throws MqttException {
    if (!connected) {
      error("could not subscribe not connected");
      return;
    }
    log.warn("mqtt --subscribe-- to {}", topic);
    client.subscribe(topic, qos, userContext, this);
    subscriptions.add(topic);
    broadcastState();
  }

  String tokenToString(IMqttToken token) {

    StringBuffer sb = new StringBuffer();
    sb.append(" MessageId:").append(token.getMessageId());
    sb.append(" Response:").append(token.getResponse());
    sb.append(" UserContext:").append(token.getUserContext());
    sb.append(" Qos:").append(Arrays.toString(token.getGrantedQos()));
    sb.append(" Sessionpresent:").append(token.getSessionPresent());
    sb.append(" Topics:").append(Arrays.toString(token.getTopics()));
    sb.append(" isComplete:").append(token.isComplete());
    return sb.toString();
  }

  public static void main(String[] args) {
    try {
      LoggingFactory.init("info");
      Runtime.main(new String[] { "--id", "m1"});
      /*
       * WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
       * webgui.setPort(8888); webgui.autoStartBrowser(false);
       * webgui.startService();
       * 
       * Runtime.start("clock01", "Clock");
       */
      // Runtime.start("python", "Python");
      // Runtime.start("audio", "AudioFile");
      Mqtt mqtt01 = (Mqtt) Runtime.start("mqtt01", "Mqtt");
      // mqtt01.connect("mqtt://localhost:1884");
      // mqtt01.connect(fs, fs, fs, fs);
      // mqtt.inquire("r1")
      // mqtt.inquire("mqtt02@c2")
      // MqttBroker broker = (MqttBroker) Runtime.start("broker", "MqttBroker");
      // broker.start();

      mqtt01.setCert("certs/home-client/rootCA.pem", "certs/home-client/cert.pem.crt", "certs/home-client/private.key");
      mqtt01.connect("mqtts://a22mowsnlyfeb6-ats.iot.us-west-2.amazonaws.com:8883");
      // mqtt01.connect("mqtt://broker.emqx.io:1883");
      // mqtt01.connect("tcp://iot.eclipse.org:1883");
      // mqtt01.connect("tcp://broker.hivemq.com:1883");
      // mqtt01.connect("mqtt://127.0.0.1:1883");
      // mqtt01.connect("mqtt://127.0.0.1:1883", "client",
      // "mqttpass".toCharArray());

      // Message msg = Message.createMessage("mqtt", "servo01", "moveTo", new
      // Object[] { 20.0 });
      // log.info("json [{}]", CodecUtils.toJson(msg));

      // | mrl gateway uri | protocol key
      // URI = mrl://{getName()}/tcp://iot.eclipse.org:1883

      // (broadcast)
      // allow-register list of id's

      // mqtt01.connect("tcp://broker.hivemq.com:1883");
      // mqtt01.subscribe("mrl/broadcast/#");
      // mqtt01.publish("mrl/broadcast", 1, CodecJson.encode(msg).getBytes());

      // msg = Message.createMessage(mqtt01.getName(), "runtime", "hello", new
      // Object[] { Runtime.getPlatform() });
      // mqtt01.publish("mrl/broadcast", 1, CodecJson.encode(msg).getBytes());

      // Runtime.start("servo", "Servo");
      // Runtime.start("opencv", "OpenCV");
      // Runtime.start("twitter", "Twitter");

      boolean done = true;
      if (done) {
        return;
      }

      // mqtt.startClient();
      // mqtt.publish("Hello");

      // hivemq
      // broker.hivemq.com:1883 - websocket port

      // test.mosquitto.org
      // 1883 : MQTT, unencrypted
      // 8883 : MQTT, encrypted
      // 8884 : MQTT, encrypted, client certificate required
      // 8080 : MQTT over WebSockets, unencrypted
      // 8081 : MQTT over WebSockets, encrypted

      // iot.eclipse.org:1883 encrypted - 8883
      // websockets - ws://iot.eclipse.org:80/ws and
      // wss://iot.eclipse.org:443/ws

      // - public brokers -
      // https://github.com/mqtt/mqtt.github.io/wiki/public_brokers
      // mqtt01.connect("tcp://iot.eclipse.org:1883");

      // mqtt.setBroker("tcp://iot.eclipse.org:1883");
      // mqtt01.setQos(2); // this can be defaulted - but its "per"
      // send/subscription...
      // mqtt.setClientId("mrl");

      // iot.eclipse.org - the topic name MUST NOT contain wildcards
      // mqtt.subscribe("mrl/#", 2);
      // mqtt.publish("mrl/#", 2, "Greetings from MRL !!!");

      /*
       * mqtt.subscribe("mrl", 2); mqtt.publish("mrl", 2,
       * "Greetings from MRL !!!");
       */
      // mqtt01.publish("Hello and Greetings from MRL !!!!");

    } catch (Throwable e) {
      log.error("wtf", e);
    }
  }

  // FIXME - SSUtil needs a function getFactory(String root, ..) of strings
  // inputs
  // FIXME - and needs to update Security Store !! - this is half implemented !
  @Override
  public String[] getKeyNames() {
    String caRoot = getName() + ".rootCA.pem";
    String cert = getName() + ".cert.pem";
    String privateKey = getName() + ".private.key";
    String password = getName() + ".password";
    return new String[] { caRoot, cert, privateKey, password };
  }

  @Override
  public void setKey(String keyName, String keyValue) {
    Security security = Security.getInstance();
    security.setKey(keyName, keyValue);
    broadcastState();
  }

}
