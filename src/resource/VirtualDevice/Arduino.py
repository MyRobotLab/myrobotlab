#############################################
# This is a basic script to emulate the hardware of
# an Arduino microcontroller.  The VirtualDevice
# service will execute this script when
# createVirtualArduino(port) is called
import time
import math
import threading
from random import randint
from org.myrobotlab.codec import ArduinoMsgCodec

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
    	  retcmd = "publishPin/" + str(pinx) + "/" + str(int(pinx)%4) + "/"+ str(y) +"\n"
    	  uart.write(codec.encode(retcmd))
    	
    	sleep(0.001)
    	#print (y)
    	# TODO -------
    	# if (digitalReadPollingPins.length() == 0 && analogReadPollingPins.length() == 0
    	# working = False
    	
    print("I am done !")

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
  global working, worker, analogReadPollingPins
  print("onByte", b)
  command = codec.decode(b)
  if command != None and len(command) > 0 :
    print("decoded", command)
    # rstrip trips the \n from the record
    command = command.rstrip()
    clist = command.split('/')
    
    if command == "getVersion":
      uart.write(codec.encode("publishVersion/21\n"))

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
  print("connected to ", portName)


def onPortNames(portName):
  print("TODO - list portNames")


def onDisconnect(portName):
  print("disconnected from ", portName)
