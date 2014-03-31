# This demo creates and starts an Arduino service
# Connects a serial device on Windows this would COMx 
# Sets the board type
# Then starts polling analog pin 17 which is Analog pin 3
# You will need MRLComm.ino loaded on the Arduino
# If all goes well - you should have 2 traces running
# in the arduino->oscope tab - you can at this point connect
# input - for example a 5v line to the lines and see them change
from time import sleep
from org.myrobotlab.service import Arduino

# variables dependent on your setup
boardType = "atmega2560"  # atmega168 | atmega328p | atmega2560 | atmega1280 | atmega32u4
comPort = "COM9"
readAnalogPin = 17
readDigitalPin = 13

# create an Arduino service named arduino
arduino = runtime.createAndStart("arduino","Arduino")

# set the board type
arduino.setBoard(boardType) # atmega168 | mega2560 | etc

# set serial device
arduino.connect(comPort,57600,8,1,0)
sleep(1) # give it a second for the serial device to get ready

# update the gui with configuration changes
arduino.publishState()

# start the analog pin sample to display
# in the oscope
arduino.analogReadPollingStart(readAnalogPin)

# change the pinMode of digital pin 13
arduino.pinMode(readDigitalPin, Arduino.OUTPUT)

# begin tracing the digital pin 13 
arduino.digitalReadPollStart(readDigitalPin)

# turn off the trace
# arduino.digitalReadPollStop(readDigitalPin)
# turn off the analog sampling
# arduino.analogReadPollingStop(readAnalogPin)
