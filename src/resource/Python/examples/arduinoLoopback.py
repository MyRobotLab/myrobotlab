# This demo creates and starts an Arduino service
# Connects a serial device on Windows this would COMx 
# Sets the board type
# It then uses digitalWrite to change the output value of pin 13
from org.myrobotlab.service import Arduino

# create an Arduino service named arduino
runtime.createAndStart("arduino","Arduino")

# set the board type
arduino.setBoard("atmega328") # atmega168 | mega2560 | etc

# set serial device
arduino.connect("/dev/ttyUSB1",57600,8,1,0)
sleep(1) # give it a second for the serial device to get ready

# update the gui with configuration changes
arduino.publishState()

# set the pinMode of pin 13 to output
arduino.pinMode(13, Arduino.OUTPUT)

# turn pin 13 on and off 10 times
for x in range(0 to 10):
	arduino.digitalWrite(13, Arduino.HIGH)
	sleep(0.5) # sleep half a second
	arduino.digitalWrite(13, Arduino.LOW)
	sleep(0.5) # sleep half a second
	