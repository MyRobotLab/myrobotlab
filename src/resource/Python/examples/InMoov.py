# a larger script for InMoov
inMoov = Runtime.createAndStart("inMoov", "InMoov")

# attach an arduino to InMoov
# possible board types include uno atmega168 atmega328p atmega2560 atmega1280 atmega32u4
# the MRLComm.ino sketch must be loaded into the Arduino for MyRobotLab control !
# set COM number according to the com of your Arduino board
inMoov.attachArduino("right","uno","COM12")
inMoov.attachArduino("left","atmega1280","COM10")

# attaching body parts 
inMoov.attachHand("right")
inMoov.attachHand("left")
inMoov.attachArm("right")
inMoov.attachArm("left")
inMoov.attachHead("right")

inMoov.systemCheck()
  
def heard():
  data = msg_ear_recognized.data[0]
  print "heard ", data
  #mouth.setLanguage("fr")
  mouth.setLanguage("en")
  mouth.speak("you said " + data)
  
  if (data == "rest"):
    rest() 
  elif (data == "one"):
    takeball()
  elif (data == "one ball"):
    ball()
  elif (data == "two"):
    keepball()
  elif (data == "three"):
    goestotake1()
  elif (data == "four"):
    goestotake2()
  elif (data == "five"):
    take()
  elif (data == "six"):
    takefinal1()
  elif (data == "seven"):
    takefinal2()
  elif (data == "eight"):
    takefinal3()
  elif (data == "nine"):
    takefinal4()
  elif (data == "ten"):
    davinciarm1()
  elif (data == "look one"):
    lookatthing2()
  elif (data == "down one"):
    putdown1()
  elif (data == "down two"):
    putdown2()
  elif (data == "point"):
    pointfinger()
  elif (data == "scared"):
    scared()
  elif (data == "ballet"):
    ballet()
  elif (data == "surrender"):
    surrender()
  elif (data == "surrender two"):
    surrender2()
  elif (data == "what"):
    what()
  elif (data == "welcome"):
    welcome()
  elif (data == "protect"):
    protectface()
  elif (data == "start tracking"):
    inMoov.startTracking()
  elif (data == "stop tracking"):
    inMoov.stopTracking()


inMoov.startListening("rest | one ball | one | two | three | four | five | six | seven | eight | nine | ten | look one | down one | down two | point | scared | ballet | surrender | surrender two | what | welcome | protect |  start tracking | stop tracking")

def rest():
  inMoov.setHandSpeed("left", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  inMoov.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  inMoov.setArmSpeed("left", 0.75, 0.75, 0.75, 0.75)
  inMoov.setArmSpeed("right", 0.75, 0.75, 0.75, 0.75)
  inMoov.setHeadSpeed( 0.75, 0.75)
  inMoov.moveHead(90,90)
  inMoov.moveArm("left",0,85,16,15)
  inMoov.moveArm("right",0,73,29,15)
  inMoov.moveHand("left",50,28,30,10,10,90)
  inMoov.moveHand("right",10,10,10,10,10,90)
  inMoov.broadcastState()
  sleep(5)

  
def ball():
  inMoov.setHandSpeed("right", 0.75, 0.75, 0.75, 0.75, 0.75, 0.75)
  inMoov.setArmSpeed("right", 0.85, 0.85, 0.85, 0.85)
  inMoov.moveHead(52,81)
  inMoov.moveArm("left",0,84,16,15)
  inMoov.moveArm("right",0,85,58,15)
  inMoov.moveHand("left",50,28,30,10,10,90)
  inMoov.moveHand("right",10,111,103,19,11,90)

def takeball():
  inMoov.setHandSpeed("right", 0.75, 0.75, 0.75, 0.75, 0.75, 0.75)
  inMoov.setArmSpeed("right", 0.85, 0.85, 0.85, 0.85)
  inMoov.moveHead(52,81)
  inMoov.moveArm("left",0,84,16,15)
  inMoov.moveArm("right",6,73,65,16)
  inMoov.moveHand("left",50,28,30,0,0,90)
  inMoov.moveHand("right",85,131,104,106,139,129)
  sleep(5)

def keepball():
  inMoov.moveHead(0,80)
  inMoov.moveArm("left",0,84,16,15)
  inMoov.moveArm("right",70,62,62,16)
  inMoov.moveHand("left",50,28,30,10,10,90)
  inMoov.moveHand("right",85,131,104,106,139,75)
  sleep(4)

def goestotake1():
  inMoov.setHandSpeed("right", 0.75, 0.75, 0.75, 0.75, 0.75, 0.65)
  inMoov.setArmSpeed("left", 0.75, 0.75, 0.75, 0.95)
  inMoov.setArmSpeed("right", 0.95, 0.95, 0.95, 0.85)
  inMoov.moveHead(15,84)
  inMoov.moveArm("left",90,91,37,15)
  inMoov.moveArm("right",63,50,45,15)
  inMoov.moveHand("left",50,28,30,10,10,0)
  inMoov.moveHand("right",85,85,75,72,81,22)
  sleep(1)

def goestotake2():
  inMoov.setArmSpeed("left", 0.75, 0.75, 0.75, 0.95)
  inMoov.setArmSpeed("right", 0.95, 0.95, 0.95, 0.85)
  inMoov.moveHead(12,80)
  inMoov.moveArm("left",71,51,37,15)
  inMoov.moveArm("right",63,50,45,15)
  inMoov.moveHand("left",50,28,30,10,10,0)
  inMoov.moveHand("right",77,85,75,72,81,22)
  sleep(4)


def take():
  inMoov.setHandSpeed("left", 0.75, 0.75, 0.75, 0.75, 0.75, 0.75)
  inMoov.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  inMoov.moveHead(10,74)
  inMoov.moveArm("left",71,51,37,15)
  inMoov.moveArm("right",63,50,45,15)
  inMoov.moveHand("left",50,28,30,10,10,0)
  inMoov.moveHand("right",60,85,75,72,81,22)
  sleep(2)

def takefinal1():
  inMoov.setHandSpeed("right", 0.65, 0.65, 0.65, 0.65, 0.65, 0.65)
  inMoov.moveHead(5,74)
  inMoov.moveArm("left",71,51,37,15)
  inMoov.moveArm("right",63,50,45,15)
  inMoov.moveHand("left",50,28,30,10,10,0)
  inMoov.moveHand("right",20,75,74,72,81,22)
  sleep(1)

def takefinal2():
  inMoov.setHandSpeed("left", 0.75, 0.65, 0.65, 0.65, 0.65, 0.65)
  inMoov.setHandSpeed("right", 0.75, 0.65, 0.65, 0.65, 0.65, 0.65)
  inMoov.moveHead(10,74)
  inMoov.moveArm("left",68,51,37,15)
  inMoov.moveArm("right",63,50,45,15)
  inMoov.moveHand("left",155,110,118,10,10,0)
  inMoov.moveHand("right",20,64,72,72,81,22)
  sleep(4)

def takefinal3():
  inMoov.setHandSpeed("left", 0.75, 0.65, 0.65, 0.65, 0.65, 0.65)
  inMoov.setHandSpeed("right", 0.65, 0.65, 0.65, 0.65, 0.65, 0.65)
  inMoov.moveHead(10,74)
  inMoov.moveArm("left",68,51,37,15)
  inMoov.moveArm("right",63,50,45,15)
  inMoov.moveHand("left",170,110,118,10,10,0)
  inMoov.moveHand("right",20,30,40,30,30,22)
  sleep(3)

def takefinal4():
  inMoov.setHandSpeed("left", 1.0, 0.65, 0.65, 0.65, 0.65, 0.65)
  inMoov.setHandSpeed("right", 0.75, 0.75, 0.75, 0.75, 0.75, 0.75)
  inMoov.setArmSpeed("right", 0.75, 0.85, 0.95, 0.85)
  inMoov.moveHead(10,74)
  inMoov.moveArm("left",71,51,37,15)
  inMoov.setArmSpeed("right", 0.65, 0.65, 0.75, 0.85)
  inMoov.moveArm("right",0,82,33,15)
  inMoov.moveHand("left",140,125,125,34,34,0)
  inMoov.moveHand("right",20,20,40,30,30,20)
  sleep(2)

def davinciarm1():
  inMoov.setHandSpeed("left", 1.0, 1.0, 1.0, 1.0, 1.0, 0.65)
  inMoov.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 0.65)
  inMoov.setArmSpeed("left", 0.75, 0.75, 0.75, 0.75)
  inMoov.setArmSpeed("right", 0.75, 0.75, 0.75, 0.75)
  inMoov.setHeadSpeed( 0.75, 0.75)
  inMoov.moveHead(80,90)
  inMoov.moveArm("left",0,118,13,74)
  inMoov.moveArm("right",0,118,29,74)
  inMoov.moveHand("left",50,28,30,10,10,47)
  inMoov.moveHand("right",10,10,10,10,10,137)
  sleep(4)

def lookatthing2():
  inMoov.setHeadSpeed(0.65, 0.75)
  inMoov.moveHead(73,74)
  inMoov.moveArm("left",70,64,83,15)
  inMoov.moveArm("right",0,82,33,15)
  inMoov.moveHand("left",147,130,140,34,34,164)
  inMoov.moveHand("right",20,40,40,30,30,10)
  sleep(2)

def putdown1():
  inMoov.moveHead(0,99)
  inMoov.moveArm("left",1,45,57,31)
  inMoov.moveArm("right",0,82,33,15)
  inMoov.moveHand("left",147,130,135,34,34,35)
  inMoov.moveHand("right",20,40,40,30,30,22)
  sleep(2)

def putdown2():
  inMoov.moveHead(0,99)
  inMoov.moveArm("left",1,45,53,31)
  inMoov.moveArm("right",0,82,33,15)
  sleep(3)
  inMoov.moveHand("left",147,61,67,34,34,35)
  inMoov.moveHand("right",20,40,40,30,30,22)
  inMoov.broadcastState()
  sleep(2)

def pointfinger():
  inMoov.moveHead(90,90)
  inMoov.moveArm("left",0,84,16,15)
  inMoov.moveArm("right",26,73,88,15)
  inMoov.moveHand("left",50,28,30,10,10,90)
  inMoov.moveHand("right",10,10,142,156,148,180)

def scared():
  inMoov.moveHead(90,90)
  inMoov.moveArm("left",90,40,24,15)
  inMoov.moveArm("right",90,40,139,10)
  inMoov.moveHand("left",68,85,56,27,26,52)
  inMoov.moveHand("right",10,10,20,34,19,156)

def ballet():
  inMoov.moveHead(90,90)
  inMoov.moveArm("left",0,40,95,29)
  inMoov.moveArm("right",50,40,164,10)
  inMoov.moveHand("left",68,0,56,27,26,52)
  inMoov.moveHand("right",10,10,20,34,19,156)

def surrender():
  inMoov.moveHead(90,90)
  inMoov.moveArm("left",90,139,15,80)
  inMoov.moveArm("right",90,145,37,80)
  inMoov.moveHand("left",50,28,30,10,10,76)
  inMoov.moveHand("right",10,10,10,10,10,139)

def surrender2():
  inMoov.moveHead(90,112)
  inMoov.moveArm("left",90,139,48,80)
  inMoov.moveArm("right",90,145,77,80)
  inMoov.moveHand("left",50,28,30,10,10,76)
  inMoov.moveHand("right",10,10,10,10,10,139)

def what():
  inMoov.moveHead(38,90)
  inMoov.moveArm("left",0,140,0,15)
  inMoov.moveArm("right",0,140,2,15)
  inMoov.moveHand("left",50,28,30,10,10,158)
  inMoov.moveHand("right",10,10,10,10,10,90)

def welcome():
  inMoov.moveHead(38,90)
  inMoov.moveArm("left",0,140,0,49)
  inMoov.moveArm("right",0,140,2,40)
  inMoov.moveHand("left",50,28,30,10,10,158)
  inMoov.moveHand("right",10,10,10,10,10,90)

def protectface():
  inMoov.moveHead(90,90)
  inMoov.moveArm("left",90,64,128,43)
  inMoov.moveArm("right",0,73,29,15)
  inMoov.moveHand("left",50,28,30,10,10,90)
  inMoov.moveHand("right",10,10,10,10,10,90)