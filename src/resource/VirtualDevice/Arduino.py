#############################################
# This is a basic script to emulate the hardware of
# an Arduino microcontroller.  The VirtualDevice
# service will execute this script when
# createVirtualArduino(port) is called

import threading
from org.myrobotlab.codec import ArduinoMsgCodec

analogThread = None

def worker():
    """thread worker function"""
    print 'Worker'
    return


codec = ArduinoMsgCodec()

virtual = Runtime.getService("virtual")
uart = virtual.getUART()
uart.setCodec("arduino")
logic = virtual.getLogic()

logic.subscribe(uart, "publishRX", "onByte")
logic.subscribe(uart, "publishConnect", "onConnect")
logic.subscribe(uart, "publishPortNames", "onPortNames")
logic.subscribe(uart, "publishDisconnect", "onDisconnect")


def onByte(b):
  print("onByte", b)
  command = codec.decode(b)
  if len(command) > 0 :
    print("decoded", command)
    if command == "getVersion\n":
      uart.write(codec.encode("publishVersion/21\n"))
    elif command.startswith("analogReadPollingStart"):
      print("analogReadPollingStart")


def onConnect(portName):
  print("connected to ", portName)


def onPortNames(portName):
  print("TODO - list portNames")


def onDisconnect(portName):
  print("disconnected from ", portName)
