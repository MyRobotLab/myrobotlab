# dynamicallyLoadProgram.py
# This script will show how to dynamically load a PDE into Arduino
# Reasons to do this would be :
# 	1. You may want an Arduino to swap programs based on some other input.
#	   e.g. exploring program, search program, chargin program, etc.
#	2. If you have large PDEs you could break them apart and load the 
#          appropriate one depending on current task.  This could make the effective
#	   programming size (if modularized in 16K blocks) of a running robot limitless

from org.myrobotlab.service import Runtime
from org.myrobotlab.service import Arduino

# create and start the Arduino Service
arduino = Runtime.create("arduino","Arduino")
arduino.startService()

arduino.setPort("/dev/ttyUSB0")

arduino.loadFile("/resource/Arduino/StandardFirmata.pde")
