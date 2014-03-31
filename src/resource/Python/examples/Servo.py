from org.myrobotlab.service import Arduino
from org.myrobotlab.service import Servo
from org.myrobotlab.service import Runtime

from time import sleep

# create the services

arduino = Runtime.createAndStart("arduino","Arduino")
servo01 = Runtime.createAndStart("servo01","Servo")

# initialize arduino
arduino.connect("/dev/ttyUSB0")

# TODO - set limits

# attach servo
arduino.servoAttach(servo01.getName(), 9)

# fast sweep
servo01.moveTo(179)
sleep(0.5)

servo01.moveTo(10)
sleep(0.5)

servo01.moveTo(179)
sleep(0.5)

servo01.moveTo(10)
sleep(0.5)

servo01.moveTo(179)
sleep(0.5)

servo01.moveTo(10)
sleep(0.5)

# speed changes  
servo01.setSpeed(0.99) # set speed to 99% of full speed
servo01.moveTo(90) 
sleep(0.5)
servo01.setSpeed(0.25) # set speed to 25% of full speed
servo01.moveTo(180)

