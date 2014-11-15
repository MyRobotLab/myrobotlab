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
		System.out.println("Connecting to broker: " + broker);
		client.connect(connOpts);

		System.out.println("Message published");
	}

	public void publish(String content) throws MqttPersistenceException, MqttException {
		System.out.println("Connected");
		System.out.println("Publishing message: " + content);
		MqttMessage message = new MqttMessage(content.getBytes());
		message.setQos(qos);
		client.publish(topic, message);
	}

	public void disconnect() {
		try {
			if (client != null) {
				client.disconnect();
			}
		} catch (Exception e) {
			Logging.logException(e);
		}
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

	public Status test() {

		Status status = super.test();

		try {

			String topic = "MQTT Examples";
			int qos = 2;
			String broker = "tcp://iot.eclipse.org:1883";
			String clientId = "JavaSample";

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

		MQTT template = (MQTT) Runtime.start("template", "MQTT");
		Runtime.start("gui", "GUIService");
		template.test();

	}

}
