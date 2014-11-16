package org.myrobotlab.service;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class MQTT extends Service {

	private static final long serialVersionUID = 1L;

	MqttClient client;
	String topic;
	int qos = 2;

	public final static Logger log = LoggerFactory.getLogger(MQTT.class);

	public MQTT(String n) {
		super(n);
	}

	public void startClient(String topic, int qos, String broker, String clientId) throws MqttException {
		this.topic = topic;
		MemoryPersistence persistence = new MemoryPersistence();
		client = new MqttClient(broker, clientId, persistence);
		MqttConnectOptions connOpts = new MqttConnectOptions();
		connOpts.setCleanSession(true);
		log.info("Connecting to broker: " + broker);
		client.connect(connOpts);
		log.info("Connected");
	}

	public void publish(String content) throws MqttPersistenceException, MqttException {
		try {
			log.info("Publishing message: " + content);
			MqttMessage message = new MqttMessage(content.getBytes());
			message.setQos(qos);
			client.publish(topic, message);
			log.info("Message published");
		} catch (MqttException e) {
			log.info("reason "+e.getReasonCode());
			log.info("msg "+e.getMessage());
			log.info("loc "+e.getLocalizedMessage());
			log.info("cause "+e.getCause());
			log.info("excep "+e);
            //e.printStackTrace();
		}
	}

	public void disconnect() {
		try {
			if (client != null) {
				client.disconnect();
				log.info("Disconnected");
			}
		} catch (Exception e) {
			Logging.logException(e);
		}
	}

	@Override
	public String getDescription() {
		return "This is an MQTT client based on the Paho MQTT client library. MQTT is a machine-to-machine (M2M)/'Internet of Things' connectivity protocol. See http://mqtt.org";
	}

	public Status test() {

		Status status = super.test();

		try {

			String topic = "mrl";
			int qos = 2;
			String broker = "tcp://iot.eclipse.org:1883";
			String clientId = "MRL_mqtt_client";

			startClient(topic, qos, broker, clientId);
			publish("HELLO I'VE JUST BEEN BORGED");

		} catch (MqttException e) {
			status.addError(e);
		}
		return status;
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);
	
		MQTT mqtt = (MQTT) Runtime.start("mqtt", "MQTT");
		Runtime.start("gui", "GUIService");
	
		mqtt.test();

	}

}
