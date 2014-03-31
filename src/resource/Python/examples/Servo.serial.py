import time
serial = Runtime.createAndStart("serial","Serial")
# force serial to publish integers (0-255) instead of bytes (-128 - 127)
serial.publishInt()

arduino = Runtime.createAndStart("arduino","Arduino")
servo01 = Runtime.createAndStart("servo01","Servo")
arduino.connect("COM8")
arduino.servoAttach(servo01.getName(), 3)
 
if not serial.isConnected():
    #connect to a serial port COM3 57600 bitrate 8 data bits 1 stop bit 0 parity
    serial.connect("COM1", 9600, 8, 1, 0)
    #have python listening to serial - from publish Int
    serial.addListener("publishInt", python.name, "input") 
 
 
def input():
 # newByte = serial.readInt()
 newByte = msg_serial_publishInt.data[0]
 print newByte
 servo01.moveTo(newByte)