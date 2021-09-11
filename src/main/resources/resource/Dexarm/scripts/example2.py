# This Python file uses the following encoding: utf-8
#########################################
# Serial.py
# categories: serial
# more info @: http://myrobotlab.org/service/Serial
#########################################
# uncomment for virtual hardware
virtual = False

#port = "COM3"

# start the services
serial = Runtime.start("serial","Serial")
python = Runtime.start("python","Python")

# connect to a serial port COM4 57600 bitrate 8 data bits 1 stop bit 0 parity
serial.connect("COM3", 115200, 8, 1, 0)

#serial.connect(port) # default rate is 115200

def onByte(code):
 decoded = "".join(chr(code))
 print decoded
#have python listening to serial
#serial.addByteListener("python")

#def onConnect(port):
  #print "connected to port {}".format(port)

#def onDisconnect(port):
  #print "disconnected from port {}".format(port)

serial.writeFile('resource/Dexarm/gcode/test1.gcode')
#serial.write(87)
#serial.write(79)
#serial.write(82)
#serial.write(75)
#serial.write(89)
