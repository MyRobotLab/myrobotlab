package org.myrobotlab.service;

import java.net.URI;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.myrobotlab.codec.Codec;
import org.myrobotlab.codec.CodecFactory;
import org.myrobotlab.codec.CodecJson;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

/**
 *
 * Mqtt - Mqtt is a machine-to-machine (M2M)/"Internet of Things" connectivity
 * protocol. It was designed as an extremely lightweight publish/subscribe
 * messaging transport. It is useful for connections with remote locations where
 * a small code footprint is required and/or network bandwidth is at a premium.
 * http://mqtt.org/
 * 
 * Much of this code was copied from the sample program provided by the Paho
 * project
 * http://git.eclipse.org/c/paho/org.eclipse.paho.mqtt.java.git/tree/org.eclipse
 * .paho.sample.mqttv3app/src/main/java/org/eclipse/paho/sample/mqttv3app/
 * SampleAsyncCallBack.java
 * 
 * @author kmcgerald
 *
 */
public class Mqtt extends Service implements MqttCallback, IMqttActionListener {

  public static class MqttMsg {
    public byte[] payload;
    public String topic;

    MqttMsg(String topic, byte[] payload) {
      this.topic = topic;
      this.payload = payload;
    }

    public String toString() {
      return String.format("/%s-%s", topic, payload);
    }
  }

  public final static Logger log = LoggerFactory.getLogger(Mqtt.class);
  private static final long serialVersionUID = 1L;

  boolean cleanSession = true; // Non durable subscriptions
  transient MqttAsyncClient client;
  String id = Platform.getLocalInstance().getId();
  String clientId = String.format("%s@%s", getName(), id);
  transient MqttConnectOptions conOpt;
  boolean isConnected = false;
  String topic = String.format("myrobotlab/%s", id);
  int port = 1883;
  int qos = 2;

  Set<String> subscriptions = new HashSet<String>();

  // String url = "tcp://iot.eclipse.org:1883";

  /**
   * Two types of connection are supported tcp:// for a TCP connection and
   * ssl:// , which is weird, that its not mqtt:// & mqtts:// :P
   */
  String url = "tcp://iot.eclipse.org:1883";

  // mrl specific
  Codec codec;

  public Mqtt(String n) {
    super(n);
    try {
      codec = CodecFactory.getCodec(CodecUtils.MIME_TYPE_JSON);
    } catch (Exception e) {
      error("codec failed to initialize");
    }
  }

  public boolean connect(String url) throws MqttSecurityException, MqttException {

    return connect(url, null, null);
  }

  public boolean connect(String url, String userName, char[] password) throws MqttSecurityException, MqttException {

    if (client == null) {
      // FIXME - should be member ? what is this for ?
      MemoryPersistence persistence = new MemoryPersistence();
      this.url = url;
      conOpt = new MqttConnectOptions();
      conOpt.setCleanSession(true);
      if (userName != null) {
        conOpt.setUserName(userName);
        conOpt.setPassword(password);
      }
      clientId = String.format("%s@%s", getName(), id);
      client = new MqttAsyncClient(url, clientId, persistence);
      client.setCallback(this);

      client.connect(conOpt, "Connect sample context", this);
      int i = 0;
      while (!client.isConnected() || i < 10) {
        sleep(1);
        i += 1;
      }

      if (!client.isConnected()) {
        client.connect(conOpt, "Connect sample context", this);
        isConnected = true;
      } else {
        isConnected = false;
      }
    }
    broadcastState();
    return isConnected;
  }

  /**
   * @see MqttCallback#connectionLost(Throwable)
   */
  @Override
  public void connectionLost(Throwable cause) {
    // Called when the connection to the server has been lost.
    // An application may choose to implement reconnection
    // logic at this point. This sample simply exits.
    error("Connection to " + url + " lost!" + cause);
  }

  /**
   * @see MqttCallback#deliveryComplete(IMqttDeliveryToken)
   */
  @Override
  public void deliveryComplete(IMqttDeliveryToken token) {
    // Called when a message has been delivered to the
    // server. The token passed in here is the same one
    // that was returned from the original call to publish.
    // This allows applications to perform asynchronous
    // delivery without blocking until delivery completes.
    //
    // This sample demonstrates asynchronous deliver, registering
    // a callback to be notified on each call to publish.
    //
    // The deliveryComplete method will also be called if
    // the callback is set on the client
    //
    // note that token.getTopics() returns an array so we convert to a
    // string
    // before printing it on the console
    log.info("Delivery complete callback: Publish Completed " + Arrays.toString(token.getTopics()));
  }

  public void disconnect() throws MqttException {
    client.disconnect();
  }

  public String getTopic() {
    return topic;
  }

  public Set<String> getSubscriptions() {
    return subscriptions;
  }

  public String getUrl() {
    return url;
  }

  /**
   * @see MqttCallback#messageArrived(String, MqttMessage)
   */
  @Override
  public void messageArrived(String topic, MqttMessage message) throws MqttException {
    // Called when a message arrives from the server that matches any
    // subscription made by the client
    String time = new Timestamp(System.currentTimeMillis()).toString();
    // FIXME - new String won't be happy with true binary payloads ..
    String payload = new String(message.getPayload());
    String messageStr = "onMqttMsg Time: " + time + "\tTopic: " + topic + "\tMessage: " + payload + "\tQoS: " + message.getQos();
    log.info(messageStr);

    // serialization needs to be a logical layer - so we can change it to
    // protobuf or native Java
    try {
      Message msg = (Message) codec.decode(payload, Message.class);

      // COMMON GATEWAY REGISTERATION AND X-FORWARDED BEGIN --------------

      // make a mrl key and protocol uri begin ------
      // ADD TCP CLIENT BEGIN ! - probably should not be in RemoteAdapter - as
      // this is a detail for tcp

      // String clientKey = String.format("mqtt://%s:%d", this.url);
      URI uri = new URI(url);
      // HELP PROTOKEY VS MRL KEY ??
      // TcpThread tcp = new TcpThread(myService, uri, clientSocket);
      // tcpClientList.put(uri, tcp);
      // myService.connections.put(uri, tcp.data);

      // make a mrl key and protocol uri end -------

      // registration request - security is applied here
      // x-forwarded is
      // msg.name == runtime;

      // COMMON GATEWAY REGISTERATION AND X-FORWARDED END ----------------

    } catch (Exception e) {
      error("tried decoding [%] into a message", payload);
    }

    // are all necessary ?
    invoke("publishMqttMsg", topic, message.getPayload());
    invoke("publishMqttMsgByte", message.getPayload());
    invoke("publishMqttMsgString", new String(message.getPayload()), topic);
  }

  @Override
  public void onFailure(IMqttToken token, Throwable throwable) {
    log.error("error - {} {}", tokenToString(token), throwable);
  }

  @Override
  public void onSuccess(IMqttToken token) {
    log.info("success - {} ", tokenToString(token));
  }

  // dangerous - as topic is a field value
  public void publish(String msg) throws Throwable {
    publish(topic, qos, msg);
  }

  public void publish(String topic, Integer qos, byte[] payload) throws MqttPersistenceException, MqttException {
    MqttMessage message = null;

    if (client == null || !client.isConnected()) {
      connect(url);
    }

    // Send / publish a message to the server
    // Get a token and setup an asynchronous listener on the token which
    // will be notified once the message has been delivered
    message = new MqttMessage(payload);
    message.setQos(qos);

    String time = new Timestamp(System.currentTimeMillis()).toString();
    log.info("Publishing at: " + time + " to topic \"" + topic + "\" qos " + qos);

    // FIXME - see if user context is like a backend id ...
    client.publish(topic, message, "Pub sample context", this);

  }

  public void publish(String topicName, int qos, String payload) throws MqttPersistenceException, MqttException {
    publish(topicName, qos, payload.getBytes());
  }

  public MqttMsg publishMqttMsg(String topic, byte[] msg) {
    return new MqttMsg(topic, msg);
  }

  public String[] publishMqttMsgString(String msg, String topic) {
    String[] result = { msg, topic };
    return result;
  }

  public byte[] publishMqttMsgByte(byte[] msg) {
    return msg;
  }

  public void setBroker(String broker) {
    url = broker;
  }

  public void setClientId(String cId) {
    clientId = cId;
  }

  public void setPubTopic(String Topic) {
    topic = Topic;
  }

  public void setQos(int q) {
    qos = q;
  }

  public void subscribe(String topic) throws MqttException {
    subscribe(topic, 2);
  }

  /*
   * Subscribe to a topic on an Mqtt server Once subscribed this method waits
   * for the messages to arrive from the server that match the subscription. It
   * continues listening for messages until the enter key is pressed. {
   * 
   * @param topic to subscribe to (can be wild carded)
   * 
   * @param qos the maximum quality of service to receive messages at for this
   * subscription
   */
  public void subscribe(String topic, int qos) throws MqttException {
    if (client == null || !client.isConnected()) {
      connect(url);
    }
    client.subscribe(topic, qos, "Subscribe sample context", this);
    subscriptions.add(topic);
    broadcastState();
  }

  String tokenToString(IMqttToken token) {
    // FIXME - just gson encode it..
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

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(Mqtt.class);
    meta.addDescription(
        "This is an Mqtt client based on the Paho Mqtt client library. Mqtt is a machine-to-machine (M2M)/'Internet of Things' connectivity protocol. See http://mqtt.org");
    meta.addCategory("connectivity", "cloud");
    /*
     * <!--
     * https://mvnrepository.com/artifact/org.eclipse.paho/org.eclipse.paho.
     * client.mqttv3 --> <dependency org="org.eclipse.paho"
     * name="org.eclipse.paho.client.mqttv3" rev="1.2.0"/>
     */
    meta.addDependency("org.eclipse.paho", "org.eclipse.paho.client.mqttv3", "1.2.0");
    meta.setCloudService(true);
    return meta;
  }

  public int getQos() {
    return qos;
  }

  public static void main(String[] args) {
    try {
      LoggingFactory.init();

      Mqtt mqtt01 = (Mqtt) Runtime.start("mqtt01", "Mqtt");

      // | mrl gateway uri | protocol key
      // URI = mrl://{getName()}/tcp://iot.eclipse.org:1883

      // (broadcast)
      // allow-register list of id's

      mqtt01.connect("tcp://iot.eclipse.org:1883");
      mqtt01.subscribe("mrl/broadcast/#");
      Message msg = Message.createMessage(mqtt01.getName(), "runtime", "hello", new Object[] { Runtime.getPlatform() });
      mqtt01.publish("mrl/broadcast", 1, CodecJson.encode(msg).getBytes());

      // Runtime.start("servo", "Servo");
      // Runtime.start("opencv", "OpenCV");
      // Runtime.start("twitter", "Twitter");
      Runtime.start("gui", "SwingGui");

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
      mqtt01.connect("tcp://iot.eclipse.org:1883");

      // mqtt.setBroker("tcp://iot.eclipse.org:1883");
      mqtt01.setQos(2); // this can be defaulted - but its "per"
      // send/subscription...
      // mqtt.setClientId("mrl");

      // iot.eclipse.org - the topic name MUST NOT contain wildcards
      // mqtt.subscribe("mrl/#", 2);
      // mqtt.publish("mrl/#", 2, "Greetings from MRL !!!");

      /*
       * mqtt.subscribe("mrl", 2); mqtt.publish("mrl", 2,
       * "Greetings from MRL !!!");
       */
      mqtt01.publish("Hello and Greetings from MRL !!!!");

    } catch (Throwable e) {
      log.error("wtf", e);
    }

  }

}
