# Initiate the Arduino
arduino = runtime.start("arduino","Arduino")
arduino.connect("COM3")
# Select the Arduino as controller for the IO extender on bus 1 and i2c address 0x38
pcf = runtime.start("pcf","Pcf8574")
# From version 1.0.2316 use attach instead of setController
# pcf.setController(arduino,"1","0x38")
pcf.setBus("0")
pcf.setAddress("0x38")
pcf.attach(arduino,"0","0x38") # When using the Arduino, always use Bus 0, it doesn't have a bus 1
# Set four pins as output. 
pcf.pinMode(0,"OUTPUT")
pcf.pinMode(1,"OUTPUT")
pcf.pinMode(2,"OUTPUT")
pcf.pinMode(3,"OUTPUT")
# Blink a LED on pin 1
pcf.write(1,1)
sleep(1)
pcf.write(1,0)
sleep(1)
pcf.write(1,1)
sleep(1)
pcf.write(1,0)
sleep(1)
pcf.write(1,1)
# Set four pins as output. 
pcf.pinMode(4,"INPUT")
pcf.pinMode(5,"INPUT")
pcf.pinMode(6,"INPUT")
pcf.pinMode(7,"INPUT")
# Read and display digital input
print (pcf.read(4))
print (pcf.read(5))
print (pcf.read(6))
print (pcf.read(7))

# Script to change the volume of the Max9744
# https://github.com/MyRobotLab/pyrobotlab/blob/master/home/Mats/Max9744.py
# It's similar to the pcf8574 in that it only writes a single byte
# The volume is controlled by writing a value between 0 and 63

# volume = 16
# pcf.setController(arduino,"0","0x4B")
# pcf.writeRegister(volume)
