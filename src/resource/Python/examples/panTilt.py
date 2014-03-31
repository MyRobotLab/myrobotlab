##############################################
# This script creates 2 servos of a pan / tilt kit
# attaches them to an Arduino and attaches
# a joystick to control the servos

from org.myrobotlab.service import Arduino
from org.myrobotlab.service import Servo
from org.myrobotlab.service import Joystick
from org.myrobotlab.service import Runtime
from time import sleep

# create the services
arduino = Runtime.createAndStart("arduino","Arduino")
pan 	= Runtime.createAndStart("pan","Servo")
tilt	= Runtime.createAndStart("tilt","Servo")
joystick = Runtime.createAndStart("joystick","Joystick")

arduino.connect("COM10", 57600, 8, 1, 0)

sleep(2)

# attach servos to Arduino
pan.attach(arduino.getName(), 9)
tilt.attach(arduino.getName(), 10)

# attach joystick to servos
joystick.attach(pan, Joystick.Z_AXIS)
joystick.attach(tilt, Joystick.Z_ROTATION)
joystick.setController(2);
joystick.startPolling();	
