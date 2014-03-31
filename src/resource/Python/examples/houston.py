# Houston
# Connects a serial device on Windows this would COMx 
# Sets the board type
# Then starts polling analog pin 17 which is Analog pin 3
# You will need MRLComm.ino loaded on the Arduino
# If all goes well - you should have 2 traces running
# in the arduino->oscope tab - you can at this point connect
# input - for example a 5v line to the lines and see them change
from time import sleep
from org.myrobotlab.service import Arduino
from org.myrobotlab.service import Servo
from org.myrobotlab.service import Motor

# variables dependent on your setup
boardType = "atmega2560"  # atmega168 | atmega328p | atmega2560 | atmega1280 | atmega32u4
#comPort = "/dev/ttyACM0"
#comPort = "COM9"
lfaencoder = 38
analogSensorPin = 67

# create service for Houston
arduino = runtime.createAndStart("arduino","Arduino")

lshoulder = runtime.createAndStart("lshoulder","Servo")
lbicep = runtime.createAndStart("lbicep","Servo")
lelbow = runtime.createAndStart("lelbow","Servo")

rshoulder = runtime.createAndStart("rshoulder","Servo")
rbicep = runtime.createAndStart("rbicep","Servo")
relbow = runtime.createAndStart("relbow","Servo")

# 4 motors 
lfmotor = runtime.createAndStart("lfmotor","Motor") # left front
rfmotor = runtime.createAndStart("rfmotor","Motor") # right front
lbmotor = runtime.createAndStart("lbmotor","Motor") # left back
rbmotor = runtime.createAndStart("rbmotor","Motor") # right back

# set config for the services
arduino.setBoard(boardType) # atmega168 | mega2560 | etc
arduino.connect(comPort,57600,8,1,0)
sleep(1) # give it a second for the serial device to get ready

# attach Servos & Motors to arduino
arduino.servoAttach(lshoulder.getName(), 46)
arduino.servoAttach(lbicep.getName(), 47)
arduino.servoAttach(lelbow.getName(), 48)
arduino.servoAttach(rshoulder.getName(), 50)
arduino.servoAttach(rbicep.getName(), 51)
arduino.servoAttach(relbow.getName(), 52)

arduino.motorAttach(lfmotor.getName(), 4, 30)
arduino.motorAttach(rfmotor.getName(), 5, 31)
arduino.motorAttach(lbmotor.getName(), 6, 32)
arduino.motorAttach(rbmotor.getName(), 7, 33)

# update the gui with configuration changes
arduino.publishState()

lshoulder.publishState()
lbicep.publishState()
lelbow.publishState()
rshoulder.publishState()
rbicep.publishState()
relbow.publishState()

lfmotor.publishState()
rfmotor.publishState()
lbmotor.publishState()
rbmotor.publishState()

# system check - need to do checks to see all systems are go !
# start the analog pin sample to display
# in the oscope
arduino.analogReadPollingStart(analogSensorPin)

# change the pinMode of digital pin 13
arduino.pinMode(lfaencoder, Arduino.OUTPUT)

# begin tracing the digital pin 13 
arduino.digitalReadPollStart(lfaencoder)

# turn off the trace
# arduino.digitalReadPollStop(lfaencoder)
# turn off the analog sampling
# arduino.analogReadPollingStop(analogSensorPin)
