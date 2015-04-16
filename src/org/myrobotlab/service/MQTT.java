package org.myrobotlab.service;

// Much of this code was copied from the sample program provided by the Paho project
//http://git.eclipse.org/c/paho/org.eclipse.paho.mqtt.java.git/tree/org.eclipse.paho.sample.mqttv3app/src/main/java/org/eclipse/paho/sample/mqttv3app/SampleAsyncCallBack.java

//import org.eclipse.paho.client.mqttv3.MqttClient;
import java.sql.Timestamp;
import java.util.Arrays;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
//import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

/**
 * 
 * @author kmcgerald
 *
 */
public class MQTT extends Service implements MqttCallback {
	/**
	 * Disconnect in a non-blocking way and then sit back and wait to be
	 * notified that the action has completed.
	 */
	public class Disconnector {
		public void doDisconnect() {
			// Disconnect the client
			log.info("Disconnecting");

			IMqttActionListener discListener = new IMqttActionListener() {
				public void carryOn() {
					synchronized (waiter) {
						donext = true;
						waiter.notifyAll();
					}
				}

				@Override
				public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
					ex = exception;
					state = ERROR;
					log.info("Disconnect failed" + exception);
					carryOn();
				}

				@Override
				public void onSuccess(IMqttToken asyncActionToken) {
					log.info("Disconnect Completed");
					state = DISCONNECTED;
					carryOn();
				}
			};

			try {
				client.disconnect("Disconnect sample context", discListener);
			} catch (MqttException e) {
				state = ERROR;
				donext = true;
				ex = e;
			}
		}
	}

	/**
	 * Connect in a non-blocking way and then sit back and wait to be notified
	 * that the action has completed.
	 */
	public class MqttConnector {

		public MqttConnector() {
		}

		public void doConnect() {
			// Connect to the server
			// Get a token and setup an asynchronous listener on the token which
			// will be notified once the connect completes
			log.info("Connecting to " + brokerURL + " with client ID " + client.getClientId());

			IMqttActionListener conListener = new IMqttActionListener() {
				public void carryOn() {
					synchronized (waiter) {
						donext = true;
						waiter.notifyAll();
					}
				}

				@Override
				public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
					ex = exception;
					state = ERROR;
					log.info("connect failed" + exception);
					carryOn();
				}

				@Override
				public void onSuccess(IMqttToken asyncActionToken) {
					log.info("Connected");
					state = CONNECTED;
					carryOn();
				}
			};

			try {
				// Connect using a non-blocking connect
				client.connect(conOpt, "Connect sample context", conListener);
			} catch (MqttException e) {
				// If though it is a non-blocking connect an exception can be
				// thrown if validation of parms fails or other checks such
				// as already connected fail.
				state = ERROR;
				donext = true;
				ex = e;
			}
		}
	}

	/**
	 * Publish in a non-blocking way and then sit back and wait to be notified
	 * that the action has completed.
	 */
	public class Publisher {
		public void doPublish(String topicName, int qos, byte[] payload) {
			// Send / publish a message to the server
			// Get a token and setup an asynchronous listener on the token which
			// will be notified once the message has been delivered
			MqttMessage message = new MqttMessage(payload);
			message.setQos(qos);

			String time = new Timestamp(System.currentTimeMillis()).toString();
			log.info("Publishing at: " + time + " to topic \"" + topicName + "\" qos " + qos);

			// Setup a listener object to be notified when the publish
			// completes.
			//
			IMqttActionListener pubListener = new IMqttActionListener() {
				public void carryOn() {
					synchronized (waiter) {
						donext = true;
						waiter.notifyAll();
					}
				}

				@Override
				public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
					ex = exception;
					state = ERROR;
					log.info("Publish failed" + exception);
					carryOn();
				}

				@Override
				public void onSuccess(IMqttToken asyncActionToken) {
					log.info("Publish Completed");
					state = PUBLISHED;
					carryOn();
				}
			};

			try {
				// Publish the message
				client.publish(topicName, message, "Pub sample context", pubListener);
			} catch (MqttException e) {
				state = ERROR;
				donext = true;
				ex = e;
			}
		}
	}

	/**
	 * Subscribe in a non-blocking way and then sit back and wait to be notified
	 * that the action has completed.
	 */
	public class Subscriber {
		public void doSubscribe(String topicName, int qos) {
			// Make a subscription
			// Get a token and setup an asynchronous listener on the token which
			// will be notified once the subscription is in place.
			log.info("Subscribing to topic \"" + topicName + "\" qos " + qos);

			IMqttActionListener subListener = new IMqttActionListener() {
				public void carryOn() {
					synchronized (waiter) {
						donext = true;
						waiter.notifyAll();
					}
				}

				@Override
				public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
					ex = exception;
					state = ERROR;
					log.info("Subscribe failed" + exception);
					carryOn();
				}

				@Override
				public void onSuccess(IMqttToken asyncActionToken) {
					log.info("Subscribe Completed");
					state = SUBSCRIBED;
					carryOn();
				}
			};

			try {
				client.subscribe(topicName, qos, "Subscribe sample context", subListener);
			} catch (MqttException e) {
				state = ERROR;
				donext = true;
				ex = e;
			}
		}
	}

	int state = BEGIN;

	static final int BEGIN = 0;
	static final int CONNECTED = 1;
	static final int PUBLISHED = 2;
	static final int SUBSCRIBED = 3;

	static final int DISCONNECTED = 4;

	static final int FINISH = 5;
	static final int ERROR = 6;
	static final int DISCONNECT = 7;
	private static final long serialVersionUID = 1L;
	transient MqttAsyncClient client;
	boolean quietMode = false;
	String action = "publish";
	String message = "Message from async callback client";
	int qos = 2;
	String brokerURL = "m2m.eclipse.org";
	int port = 1883;
	String clientId = "MRL MQTT client";
	String subTopic = "mrl/#";
	String pubTopic = "mrl";
	boolean cleanSession = true; // Non durable subscriptions
	boolean ssl = false;
	String password = null;
	String userName = null;
	Throwable ex = null;

	Object waiter = new Object();

	boolean donext = false;

	transient MqttConnectOptions conOpt;

	String[] tokens;

	public final static Logger log = LoggerFactory.getLogger(MQTT.class);

	public static void main(String[] args) {
		try {
			LoggingFactory.getInstance().configure();
			LoggingFactory.getInstance().setLevel(Level.INFO);
			Python python = new Python("python");
			python.startService();
			MQTT mqtt = (MQTT) Runtime.start("mqtt", "MQTT");
			Runtime.start("gui", "GUIService");

			mqtt.test();

		} catch (Exception e) {
			Logging.logError(e);
		}

	}

	public MQTT(String n) {
		super(n);
	}

	/**
	 * @see MqttCallback#connectionLost(Throwable)
	 */
	@Override
	public void connectionLost(Throwable cause) {
		// Called when the connection to the server has been lost.
		// An application may choose to implement reconnection
		// logic at this point. This sample simply exits.
		log.info("Connection to " + brokerURL + " lost!" + cause);
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

	@Override
	public String[] getCategories() {
		return new String[] { "data", "cloud" };
	}

	@Override
	public String getDescription() {
		return "This is an MQTT client based on the Paho MQTT client library. MQTT is a machine-to-machine (M2M)/'Internet of Things' connectivity protocol. See http://mqtt.org";
	}

	/**
	 * @see MqttCallback#messageArrived(String, MqttMessage)
	 */
	@Override
	public void messageArrived(String topic, MqttMessage message) throws MqttException {
		// Called when a message arrives from the server that matches any
		// subscription made by the client
		String time = new Timestamp(System.currentTimeMillis()).toString();

		String messageStr = "Time: " + time + "\tTopic: " + topic + "\tMessage: " + new String(message.getPayload()) + "\tQoS: " + message.getQos();

		log.info(messageStr);

		tokens = messageStr.split("\t");
		invoke("publishMQTTMessage");
	}

	public void publish(String content) throws Throwable { // MqttPersistenceException,
															// MqttException {
		// Use a state machine to decide which step to do next. State change
		// occurs
		// when a notification is received that an MQTT action has completed
		while (state != FINISH) {
			switch (state) {
			case BEGIN:
				// Connect using a non-blocking connect
				MqttConnector con = new MqttConnector();
				con.doConnect();
				break;
			case CONNECTED:
				// Publish using a non-blocking publisher
				Publisher pub = new Publisher();
				pub.doPublish(pubTopic, qos, content.getBytes());
				break;
			case PUBLISHED:
				state = DISCONNECT;
				donext = true;
				break;
			case DISCONNECT:
				Disconnector disc = new Disconnector();
				disc.doDisconnect();
				break;
			case ERROR:
				throw ex;
			case DISCONNECTED:
				state = FINISH;
				donext = true;
				break;
			}

			// if (state != FINISH) {
			// Wait until notified about a state change and then perform next
			// action
			waitForStateChange(10000);
			// }
		}
	}

	/****************************************************************/
	/* End of MqttCallback methods */
	/****************************************************************/

	public String[] publishMQTTMessage() {
		// tokens = message.split(",");
		return tokens;
	}

	public void setBroker(String broker) {
		brokerURL = broker;
	}

	/****************************************************************/
	/* Methods to implement the MqttCallback interface */
	/****************************************************************/

	public void setClientId(String cId) {
		clientId = cId;
	}

	public void setPubTopic(String topic) {
		pubTopic = topic;
	}

	public void setQos(int q) {
		qos = q;
	}

	public void setSubTopic(String topic) {
		subTopic = topic;
	}

	public void startClient() throws MqttException {
		MemoryPersistence persistence = new MemoryPersistence();
		try {
			conOpt = new MqttConnectOptions();
			conOpt.setCleanSession(true);
			// Construct the MqttClient instance
			client = new MqttAsyncClient(this.brokerURL, clientId, persistence);

			// Set this wrapper as the callback handler
			client.setCallback(this);

		} catch (MqttException e) {
			log.info("Unable to set up client: " + e.toString());
		}

	}

	/**
	 * Subscribe to a topic on an MQTT server Once subscribed this method waits
	 * for the messages to arrive from the server that match the subscription.
	 * It continues listening for messages until the enter key is pressed.
	 * 
	 * @param topicName
	 *            to subscribe to (can be wild carded)
	 * @param qos
	 *            the maximum quality of service to receive messages at for this
	 *            subscription
	 * @throws MqttException
	 */
	public void subscribe(String topicName, int qos) throws Throwable {
		// Use a state machine to decide which step to do next. State change
		// occurs
		// when a notification is received that an MQTT action has completed
		while (state != FINISH) {
			switch (state) {
			case BEGIN:
				// Connect using a non-blocking connect
				MqttConnector con = new MqttConnector();
				con.doConnect();
				break;
			case CONNECTED:
				// Subscribe using a non-blocking subscribe
				Subscriber sub = new Subscriber();
				sub.doSubscribe(topicName, qos);
				break;
			case SUBSCRIBED:
				// We're not going to do anything extra in this state so the
				// service can keep running
				// state = DISCONNECT;
				donext = true;
				break;
			case DISCONNECT:
				Disconnector disc = new Disconnector();
				disc.doDisconnect();
				break;
			case ERROR:
				throw ex;
			case DISCONNECTED:
				state = FINISH;
				donext = true;
				break;
			}

			// if (state != FINISH && state != DISCONNECT) {
			waitForStateChange(10000);
			// }
		}
	}

	@Override
	public Status test() {

		Status status = super.test();

		try {

			setPubTopic("mrl");
			setQos(2);
			setBroker("tcp://iot.eclipse.org:1883");
			setClientId("MRL_mqtt_client");

			startClient();

			publish("HELLO I'VE JUST BEEN BORGED");

		} catch (Throwable e) {
			status.addError((Exception) e);
		}
		return status;
	}

	/**
	 * Wait for a maximum amount of time for a state change event to occur
	 * 
	 * @param maxTTW
	 *            maximum time to wait in milliseconds
	 * @throws MqttException
	 */
	private void waitForStateChange(int maxTTW) throws MqttException {
		synchronized (waiter) {
			if (!donext) {
				try {
					waiter.wait(maxTTW);
				} catch (InterruptedException e) {
					log.info("timed out");
					e.printStackTrace();
				}

				if (ex != null) {
					throw (MqttException) ex;
				}
			}
			donext = false;
		}
	}
}
