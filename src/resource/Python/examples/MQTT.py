from time import sleep

topic = "mrl"
qos = 2
broker = "tcp://iot.eclipse.org:1883" //if you have your own just change the hostname/IP
clientID = "MRL MQTT python"

mqtt1 = Runtime.createAndStart("mqtt", "MQTT")
mqtt1.startService()
print mqtt1.getDescription()

mqtt1.setBroker(broker)
mqtt1.setQos(qos)
mqtt1.setPubTopic(topic)
mqtt1.setClientId(clientID)
mqtt1.startClient()

sleep(1)

mqtt1.subscribe("mrl/#", 2)
mqtt1.publish("Greetings from MRL python")
