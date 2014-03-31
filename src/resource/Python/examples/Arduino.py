# Connects a serial device on Windows this would COMx 
# You will need MRLComm.ino loaded on the Arduino
from time import sleep
from org.myrobotlab.service import Arduino

# create an Arduino service named arduino
arduino = Runtime.createAndStart("arduino","Arduino")

#you have to replace COMX with your arduino serial port number
# arduino.connect("/dev/ttyUSB0",57600,8,1,0) - Linux way
arduino.connect("COM3",57600,8,1,0)

# give it a second for the serial device to get ready
sleep(1)

# update the gui with configuration changes
arduino.publishState()

# set the pinMode of pin 8 to output (you can change the pin number if you want)
arduino.pinMode(8, Arduino.OUTPUT)

# turn pin 8 on and off 10 times
for x in range(0, 10):
	arduino.digitalWrite(8,1)
	sleep(1) # sleep a second
	arduino.digitalWrite(8,0)
	sleep(1) # sleep a second

# analog input pins - you can see input
# on the oscope 
# pin # = 13 + analog pin#  
# (in this case pin 16 is analog pin 3)
arduino.pinMode(16,0)
arduino.analogReadPollingStart(16)
sleep(2) # read the analog value of pin 3 for 2 seconds
arduino.pinMode(16,0)
arduino.analogReadPollingStop(16) # stop polling
