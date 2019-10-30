#########################################
# Serial.py
# categories: serial
# more info @: http://myrobotlab.org/service/Serial
#########################################
# uncomment for virtual hardware
virtual = True

port = "COM99"

# start the services
serial = Runtime.start("serial","Serial")
python = Runtime.start("python","Python")
gui = Runtime.start("gui","SwingGui")
uart = None

# start optional virtual serial service, used for test
if ("virtual" in globals() and virtual):
    uart = serial.connectVirtualUart(port)

# connect to a serial port COM4 57600 bitrate 8 data bits 1 stop bit 0 parity
# serial.connect("COM4", 57600, 8, 1, 0)

serial.connect(port) # default rate is 115200

def onByte(code):
 decoded = "".join(chr(code))
 print decoded
#have python listening to serial
serial.addByteListener("python")

def onConnect(port):
  print "connected to port {}".format(port)

def onDisconnect(port):
  print "disconnected from port {}".format(port)

serial.write(87)
serial.write(79)
serial.write(82)
serial.write(75)
serial.write(89)

# writing back from the uart to the serial service
if ("virtual" in globals() and virtual):
  uart.write(87)
  uart.write(79)
  uart.write(82)
  uart.write(75)
  uart.write(89)
