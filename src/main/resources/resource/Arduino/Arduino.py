#########################################
# Arduino.py
# description: service to connect and control to an Arduino
# categories: microcontroller, servo, control, motor
# more info @: http://myrobotlab.org/service/Arduino
#########################################
Platform.setVirtual(True) # uncomment this to connect to a virtual arduino
#
# Connects a serial device on Windows this would COMx
# You will need MRLComm.ino loaded on the Arduino
# http://www.myrobotlab.org/content/uploading-mrlcomm-arduino

port="COM3"
outputPin = 8
inputPin = 13

# start the services
arduino = runtime.start("arduino","Arduino")

# you have to replace COMX with your arduino serial port number
# arduino.connect("/dev/ttyUSB0") - Linux way
arduino.connect(port)

# give it a second for the serial device to get ready
sleep(1)

# update the GUI with configuration changes
arduino.broadcastState()

# set the pinMode of pin 8 to output (you can change the pin number if you want)
arduino.pinMode(outputPin, Arduino.OUTPUT)

# turn pin 8 on and off 5 times
print "start to play with pin output"
for x in range(0, 5):
     print('digitalWrite pin {} high'.format(outputPin))
     arduino.digitalWrite(outputPin,1)
     sleep(1) # sleep a second
     arduino.digitalWrite(outputPin,0)
     print('digitalWrite pin {} low'.format(outputPin))
     sleep(1) # sleep a second

print "stop to play with pin output"

# analog input pins - you can see input
# on the oscope
# analog pin range are 14-18 on uno, 54-70 on mega
# rate is the number of polling / sec
arduino.setBoardMega()
arduino.setAref("DEFAULT")
def publishPin(pins):
	for pin in range(0, len(pins)):print(pins[pin].value)
arduino.addListener("publishPinArray","python","publishPin")

print "start to poll pin input"
arduino.enablePin(inputPin, 1)
sleep(5)
print "stop to poll pin input"
arduino.disablePin(inputPin)
