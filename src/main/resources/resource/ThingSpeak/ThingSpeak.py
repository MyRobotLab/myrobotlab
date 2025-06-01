#########################################
# ThingSpeak.py
# more info @: http://myrobotlab.org/service/ThingSpeak
#########################################

# virtual=1
comPort = "COM12"
# start optional virtual arduino service, used for internal test
if ('virtual' in globals() and virtual):
    virtualArduino = runtime.start("virtualArduino", "VirtualArduino")
    virtualArduino.connect(comPort)
# end used for internal test 

readAnalogPin = 15
 
arduino = runtime.start("arduino","Arduino")
thing = runtime.start("thing","ThingSpeak")
 
arduino.setBoardMega() # setBoardUne | setBoardNano
arduino.connect(comPort)
sleep(1)
# update the gui with configuration changes
arduino.broadcastState()
 
thing.setWriteKey("AO4DMKQZY4RLWNNU")

# start the analog pin sample to display
# in the oscope

# decrease the sample rate so queues won't overrun
# arduino.setSampleRate(8000)

def publishPin(pins):
  for pin in range(0, len(pins)):
    thing.update(pins[pin].value)

arduino.addListener("publishPinArray",python.getName(),"publishPin")

arduino.enablePin(readAnalogPin,1)
