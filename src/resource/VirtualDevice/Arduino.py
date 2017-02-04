#############################################
# This is a basic script to emulate the hardware of
# an Arduino microcontroller.  The VirtualDevice
# service will execute this script when
# createVirtualArduino(port) is called
import time
import math
import threading
from random import randint
from org.myrobotlab.codec.serial import ArduinoMsgCodec

working = False
worker = None

analogReadPollingPins = []
digitalReadPollingPins = []

def work():
    """thread worker function"""
    global working, analogReadPollingPins
    x = 0
    working = True
    while(working):
        x = x + 0.09
        y = int(math.cos(x) * 100 + 150)
    	# retcmd = "publishPin/" + str(pin) + "/3/"+ str(y) +"\n"
    	# uart.write(codec.encode(retcmd))

    	for pinx in digitalReadPollingPins:
    	  retcmd = "publishPin/" + str(pinx) + "/0/"+str(randint(0,1))+"\n"
    	  uart.write(codec.encode(retcmd))
    	
    	for pinx in analogReadPollingPins:
    	  #retcmd = "publishPin/" + str(pinx) + "/4/"+ str(y) +"\n"
    	  #retcmd = "publishPin/" + str(pinx) + "/" + str(int(pinx)%4) + "/"+ str(y) +"\n"
    	  retcmd = "publishPin/" + str(pinx) + "/1/"+ str(y) +"\n"
    	  uart.write(codec.encode(retcmd))
    	
    	sleep(0.001)
    	#print (y)
    	# TODO -------
    	# if (digitalReadPollingPins.length() == 0 && analogReadPollingPins.length() == 0
    	# working = False
    	
    print("I am done !")

codec = ArduinoMsgCodec()

virtual = Runtime.start("virtual", "VirtualDevice")

logic = virtual.getLogic()

# get uarts and subscribe to them

for uartName in virtual.getUarts().keySet():
  uart = virtual.getUart(uartName)
  logic.subscribe(uart.getName(), "publishRX")
  logic.subscribe(uart.getName(), "onConnect")
  logic.subscribe(uart.getName(), "onPortNames")
  logic.subscribe(uart.getName(), "onDisconnect")


def onRX(b):
  global working, worker, analogReadPollingPins
  print("onByte", b)
  command = codec.decode(b)
  if command != None and len(command) > 0 :
    print("decoded", command)
    # rstrip strips the \n from the record
    command = command.rstrip()
    clist = command.split('/')
    
    if command == "getVersion":
      uart.write(codec.encode("publishVersion/"+ str(ArduinoMsgCodec.MRLCOMM_VERSION) +"\n"))

    elif command.startswith("digitalReadPollingStart"):
      print("digitalReadPollingStart")
      pin = clist[1]
      digitalReadPollingPins.append(pin)
      if worker == None:
        worker = threading.Thread(name='worker', target=work)
        worker.setDaemon(True)
        worker.start()
        
    elif command.startswith("digitalReadPollingStop"):
      print("digitalReadPollingStop")
      pin = clist[1]
      digitalReadPollingPins.remove(pin)
        
    elif command.startswith("analogReadPollingStart"):
      print("analogReadPollingStart")
      pin = clist[1]
      analogReadPollingPins.append(pin)
      if worker == None:
        worker = threading.Thread(name='worker', target=work)
        worker.setDaemon(True)
        worker.start()
        
    elif command.startswith("analogReadPollingStop"):
      print("analogReadPollingStop")
      pin = clist[1]
      analogReadPollingPins.remove(pin)


def off():
  working = False               
  worker = None
      
def onConnect(portName):
  print("onConnect to ", portName)

# FIXME ??? is this bad algorithm to determine callback method name ?
# seems somebody is expecting it this way 
def onOnConnect(portName):
  print("onOnConnect connected to ", portName)

def onPortNames(portName):
  print("onPortNames TODO - list portNames")

def onOnPortNames(portName):
  print("onOnPortNames TODO - list portNames")

def onDisconnect(portName):
  print("onDisconnect from ", portName)

def onOnDisconnect(portName):
  print("onOnDisconnect from ", portName)
  
# WHAT THE HECK IS THIS ABOUT ?
# TODO - find out
def serial1RX(data):
  print("serial1RX ", data)
  
def serial2RX(data):
  print("serial2RX ", data)

def serial3RX(data):
  print("serial3RX ", data)

def serial4RX(data):
  print("serial4RX ", data)

def serial5RX(data):
  print("serial5RX ", data)

def serial6RX(data):
  print("serial6RX ", data)

def serial7RX(data):
  print("serial7RX ", data)

def serial8RX(data):
  print("serial8RX ", data)
  
  