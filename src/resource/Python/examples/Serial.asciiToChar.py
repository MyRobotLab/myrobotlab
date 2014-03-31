#file : Serial.asciiToChar.py
import time
 
#create a Serial service named serial
serial = Runtime.createAndStart("serial","Serial")
def input():
 code = msg_serial_publishByte.data[0]
 decoded = ''.join(chr(code))
 print decoded
#have python listening to serial
serial.addListener("publishByte", python.name, "input") 
#connect to a serial port COM4 57600 bitrate 8 data bits 1 stop bit 0 parity
serial.connect("COM9", 57600, 8, 1, 0)
#sometimes its important to wait a little for hardware to get ready
sleep(1)
 
#write a series of bytes to the serial port
serial.write(87) 
serial.write(79) 
serial.write(82) 
serial.write(75) 
serial.write(89)
serial.write(32)