package org.myrobotlab.service;

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
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
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
  }

  public final static Logger log = LoggerFactory.getLogger(Mqtt.class);
  private static final long serialVersionUID = 1L;

  boolean cleanSession = true; // Non durable subscriptions
  transient MqttAsyncClient client;
  String clientId = String.format("%s@%s", getName(), Runtime.getInstance().getId());
  transient MqttConnectOptions conOpt;
  boolean isConnected = false;
  String topic = String.format("myrobotlab/%s", Runtime.getInstance().getId());
  int port = 1883;
  int qos = 2;

  Set<String> subscriptions = new HashSet<String>();

  String url = "tcp://iot.eclipse.org:1883";

  String userName = null;

  public Mqtt(String n) {
    super(n);
  }

  // FIXME - catch throw - set field to isConnected = false;
  public void connect(String url) {

    try {

      if (client == null) {
        // FIXME - should be member ? what is this for ?
        MemoryPersistence persistence = new MemoryPersistence();
        this.url = url;
        conOpt = new MqttConnectOptions();
        conOpt.setCleanSession(true);
        clientId = String.format("%s@%s", getName(), Runtime.getInstance().getId());
        client = new MqttAsyncClient(url, clientId, persistence);

        // Set this wrapper as the callback handler
        client.setCallback(this);
      }

      if (!client.isConnected()) {
        client.connect(conOpt, "Connect sample context", this);
        isConnected = true;
      }

    } catch (MqttException e) {
      isConnected = false;
    }
    broadcastState();
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
    String messageStr = "onMqttMsg Time: " + time + "\tTopic: " + topic + "\tMessage: " + new String(message.getPayload()) + "\tQoS: " + message.getQos();
    log.info(messageStr);
    invoke("publishMqttMsg", topic, message.getPayload());
  }

  @Override
  public void onFailure(IMqttToken token, Throwable throwable) {
    log.error("error - {} {}", tokenToString(token), throwable);
  }

  @Override
  public void onSuccess(IMqttToken token) {
    log.info("success - {} ", tokenToString(token));
  }

  public void publish(String msg) throws Throwable {
    publish(topic, qos, msg);
  }

  public void publish(String topic, int qos, byte[] payload) throws Throwable {
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

    client.publish(topic, message, "Pub sample context", this);

  }

  public void publish(String topicName, int qos, String payload) throws Throwable {
    publish(topicName, qos, payload.getBytes());
  }

  public MqttMsg publishMqttMsg(String topic, byte[] msg) {
    return new MqttMsg(topic, msg);
  }

  public void setBroker(String broker) {
    url = broker;
  }

  public void setClientId(String cId) {
    clientId = cId;
  }

  public void setQos(int q) {
    qos = q;
  }

  public void subscribe(String topic) throws Throwable {
    subscribe(topic, 2);
  }

  /**
   * Subscribe to a topic on an Mqtt server Once subscribed this method waits
   * for the messages to arrive from the server that match the subscription. It
   * continues listening for messages until the enter key is pressed. {
   * 
   * @param topic
   *          to subscribe to (can be wild carded)
   * @param qos
   *          the maximum quality of service to receive messages at for this
   *          subscription
   * @throws MqttException
   */
  public void subscribe(String topic, int qos) throws Throwable {
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
    sb.append(" Qos:").append(token.getGrantedQos());
    sb.append(" Sessionpresent:").append(token.getSessionPresent());
    sb.append(" Topics:").append(token.getTopics());
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

    ServiceType meta = new ServiceType(Mqtt.class.getCanonicalName());
    meta.addDescription(
        "This is an Mqtt client based on the Paho Mqtt client library. Mqtt is a machine-to-machine (M2M)/'Internet of Things' connectivity protocol. See http://mqtt.org");
    meta.addCategory("connectivity", "cloud");
    meta.addDependency("org.eclipse.paho", "1.0");
    return meta;
  }

  public int getQos() {
    return qos;
  }

  public static void main(String[] args) {
    try {
      LoggingFactory.init();

      Mqtt mqtt = (Mqtt) Runtime.start("mqtt", "Mqtt");

      // Runtime.start("servo", "Servo");
      // Runtime.start("opencv", "OpenCV");
      //Runtime.start("twitter", "Twitter");
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
      mqtt.connect("tcp://iot.eclipse.org:1883");

      // mqtt.setBroker("tcp://iot.eclipse.org:1883");
      mqtt.setQos(2); // this can be defaulted - but its "per"
                      // send/subscription...
      // mqtt.setClientId("mrl");

      // iot.eclipse.org - the topic name MUST NOT contain wildcards
      // mqtt.subscribe("mrl/#", 2);
      // mqtt.publish("mrl/#", 2, "Greetings from MRL !!!");

      /*
       * mqtt.subscribe("mrl", 2); mqtt.publish("mrl", 2,
       * "Greetings from MRL !!!");
       */
      mqtt.publish("Hello and Greetings from MRL !!!!");

    } catch (Throwable e) {
      log.error("wtf", e);
    }

  }

}
