#########################################
# Mqtt.py
# more info @: http://myrobotlab.org/service/Mqtt
#########################################

topic = "myrobotlab/test"
qos = 2 # At most once (0), At least once (1), Exactly once (2).
broker = "tcp://broker.mqttdashboard.com:1883"

clientID = "MrlMqttPython1"
mqtt = Runtime.start("mqtt", "Mqtt")
python = Runtime.start("python", "Mqtt")

print mqtt.getDescription()

mqtt.setBroker(broker)
mqtt.setQos(qos)
mqtt.setPubTopic(topic)
mqtt.setClientId(clientID)
mqtt.connect(broker)
# authentification mqtt.connect(broker,"guest","guest")

mqtt.subscribe("myrobotlab/test", 0)
mqtt.publish("hello myrobotlab world")
python.subscribe("mqtt", "publishMqttMsgString")
# or mqtt.addListener("publishMqttMsgString", "python")

#  MQTT call-back
# publishMqttMsgString --> onMqttMsgString(msg)
def onMqttMsgString(msg):
  # print "message : ", msg
  print "message : ",msg[0]
  print "topic : ",msg[1]
