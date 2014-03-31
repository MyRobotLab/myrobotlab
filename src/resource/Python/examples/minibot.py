from org.myrobotlab.service import Arduino
from org.myrobotlab.service import Runtime

from time import sleep

# You should want a differential drive service associated with 
# an Arduino
# as well as anything which can do Position Encoding (interface)

arduino = Runtime.create("arduino","Arduino")

# FIXME - re-entrant and auto-save functionality
arduino.setPort("COM8")
arduino.setBaudBase(115200);

# if not loaded - load with PDE :P

# move

# report back sensor readings

# report back to simulator?

