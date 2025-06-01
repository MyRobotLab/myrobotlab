#########################################
# MqttBroker.py
# more info @: http://myrobotlab.org/service/MqttBroker
#########################################
from time import sleep

# start service
mqtt = runtime.start("mqtt", "Mqtt")
broker = runtime.start("broker", "MqttBroker")
python = runtime.start("python", "Mqtt")

# start the local mqtt broker on standard port
broker.listen()

topic = "echoTopic"

mqtt.connect("tcp://localhost:1883")
# authentification mqtt.connect(broker,"guest","guest")
 
mqtt.subscribe(topic)

# qos = 1 # At most once (0), At least once (1), Exactly once (2).
mqtt.publish("echoTopic", "hello myrobotlab world")
python.subscribe("mqtt", "publishMqttMsg")
# or mqtt.addListener("publishMqttMsgString", "python")
 
# publishMqttMsg --> onMqttMsg(msg)
def onMqttMsg(msg):
  print ("message : ", msg)


for i in range(30):
    mqtt.publish(topic, "hello myrobotlab ! " + str(i))
    sleep(0.5)
