from time import sleep
from org.myrobotlab.service.data import Pin
from org.myrobotlab.service import Arduino
 
# variables dependent on your setup
boardType = "atmega328p"  # atmega168 | atmega328p | atmega2560 | atmega1280 | atmega32u4
comPort = "COM12"
readAnalogPin = 15
 
arduino = runtime.createAndStart("arduino","Arduino")
thing = runtime.createAndStart("thing","ThingSpeak")
 
arduino.setBoard(boardType) # atmega168 | mega2560 | etc
if not arduino.isConnected():
  arduino.connect(comPort)

 
thing.setWriteKey("AO4DMKQZY4RLWNNU")
thing.subscribe("publishPin", arduino.getName(), "update", Pin().getClass())
 
# update the gui with configuration changes
arduino.publishState()
 
# start the analog pin sample to display
# in the oscope

# decrease the sample rate so queues won't overrun
arduino.setSampleRate(8000)
arduino.analogReadPollingStart(readAnalogPin)