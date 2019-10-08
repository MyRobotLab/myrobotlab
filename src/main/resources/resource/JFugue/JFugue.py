import time
 
#create a Serial service named serial
jf = Runtime.createAndStart("jf","JFugue")
serial = Runtime.createAndStart("serial","Serial")
count = 0
 
if not serial.isConnected():
    #connect to a serial port COM3 57600 bitrate 8 data bits 1 stop bit 0 parity
    serial.connect("COM3", 9600, 8, 1, 0)
    #have python listening to serial
    serial.addListener("publishByte", python.name, "input") 
 
 
def input():
 global count
 newByte = int(serial.readByte())
 #we have reached the end of a new line
 if (newByte == 10) :
    distanceString = ""
    while (newByte != 13):
        newByte = serial.readByte()
        distanceString += chr(newByte)
 
    distance = int(distanceString)
    print distance
    # count is used to avoid loop : one note couldn't be repeated
    if (distance < 20 and count != 1) :
        jf.play('C')
        count = 1
    elif (distance > 30 and distance < 50 and count != 2):
        jf.play('D')
        count = 2
    elif (distance > 60 and distance < 80 and count != 3):
        jf.play('E')
        count = 3
