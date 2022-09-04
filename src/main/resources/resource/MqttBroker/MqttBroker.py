#########################################
# Mqtt.py
# more info @: http://myrobotlab.org/service/MqttBroker
#########################################

topic = "myrobotlab/test"
qos = 1 # At most once (0), At least once (1), Exactly once (2).

broker = runtime.start("broker", "MqttBroker")
mqtt01 = runtime.start("mqtt01", "Mqtt")
python = runtime.start("python", "Python")

broker.listen()
broker.publish('')





mqtt01.connect("mqtt://localhost:1883")

# broker.listen(1884)
# mqtt01.setQos(qos)
# mqtt01.setPubTopic(topic)
# mqtt01.setClientId(clientID)
# mqtt01.connect(broker)
# authentification mqtt01.connect(broker,"guest","guest")
# check setting clientid 


broker.releaseService()

mqtt01.subscribe("myrobotlab/test")
mqtt01.publish("hello myrobotlab world")

python.subscribe("mqtt01", "publishMqttMsg")

# subscribe to arbitrary topic

# invoke  


# or mqtt01.addListener("publishMqttMsgString", "python")

#  MQTT call-back
# publishMqttMsgString --> onMqttMsgString(msg)
def onMqttMsg(msg):
  print(msg)
  # print "message : ", msg
  # print "message : ",msg[0]
  # print "topic : ",msg[1]
