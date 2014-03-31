# This demo creates and starts an Arduino service
# Connects a serial device on Windows this would COMx 
# Sets the board type
# It then uses digitalWrite to change the output value of pin 13
# You will need MRLComm.ino loaded on the Arduino
from time import sleep
from org.myrobotlab.service import Arduino

# create an Arduino service named arduino
arduino = runtime.createAndStart("arduino","Arduino")

# set the board type
arduino.setBoard("atmega168") # atmega168 | mega2560 | etc

# set serial device
arduino.connect("/dev/ttyUSB0",57600,8,1,0)
sleep(1) # give it a second for the serial device to get ready

# update the gui with configuration changes
arduino.publishState()

# set the pinMode of pin 13 to output
arduino.pinMode(13, Arduino.OUTPUT)

# turn pin 13 on and off 10 times
for x in range(0, 10):
	arduino.digitalWrite(13, Arduino.HIGH)
	sleep(0.5) # sleep half a second
	arduino.digitalWrite(13, Arduino.LOW)
	sleep(0.5) # sleep half a second
	

# set the pinMode of pwm pin 9 to output
arduino.pinMode(9, Arduino.OUTPUT)

# run through pin 9's pwm frequency 5 times
# in increments of 10 - dim to bright
for y in range(0, 5):
  for x in range(0, 255, 10):
	  arduino.analogWrite(9, x)
	  sleep(0.1) # sleep a 1 tenth of a second

# turn out the lights :)
arduino.analogWrite(9, 0)

