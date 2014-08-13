# Robyn still uses MRL 1695
#warning
#warning
# i've put  # on the movearm so my settings dont twists any arms. 
 
#################################################
 
sleep(3)#file : InMoov.full.py
# a basic script for starting the InMoov service 
# and attaching the right hand
# an Arduino is required, additionally a computer
# with a microphone and speakers is needed for voice
# control and speech synthesis
 
from java.lang import String
 
import random
 
# ADD SECOND STAGE CONFIRMATION
#  instead of saying: you said... it would say: did you say...? and I would confirm with yes or give the voice command again
#  face tracking in InMoov ... activated by voice ...
 
inMoov = Runtime.createAndStart("inMoov", "InMoov")
 
rightSerialPort = "COM4"
leftSerialPort = "COM3"
cameraIndex = 1
 
# attach an arduinos to InMoov
# possible board types include uno atmega168 atmega328p atmega2560 atmega1280 atmega32u4
# the MRLComm.ino sketch must be loaded into the Arduino for MyRobotLab control
inMoov.attachArduino("right","uno",rightSerialPort)
inMoov.attachHand("right")
inMoov.attachArm("right")
 
inMoov.attachArduino("left","mega2560", leftSerialPort)
inMoov.attachHand("left")
inMoov.attachArm("left")
inMoov.attachHead("left")
 
# attach the servo for jaw to Arduino right pin12, value position min 40, max 55
inMoov.attachMouthControl("right",12,30,55)
sleep(1)
mouthcontrol.setdelays(200,250,43)
 
helvar = 1
 
# system check
inMoov.systemCheck()
inMoov.setCameraIndex(cameraIndex)
 
 
# new process for verbal commands
ear = inMoov.getEar()
ear.addCommand("open hand", inMoov.getName(), "handOpen", "both")
ear.addCommand("close hand", inMoov.getName(), "handClose", "both")
ear.addCommand("camera on", inMoov.getName(), "cameraOn")
# ear.addCommand("off camera", inMoov.getName(), "cameraOff") - needs fixing
ear.addCommand("capture gesture", inMoov.getName(), "captureGesture")
ear.addCommand("track", inMoov.getName(), "track")
ear.addCommand("freeze track", inMoov.getName(), "clearTrackingPoints")
ear.addCommand("manual", ear.getName(), "lockOutAllGrammarExcept", "voice control")
ear.addCommand("voice control", ear.getName(), "clearLock")
ear.addCommand("stop listening", ear.getName(), "stopListening")
 
ear.addCommand("look forward", "python", "lookforward") 
 
 
ear.addComfirmations("yes","correct","yeah","ya") 
ear.addNegations("no","wrong","nope","nah")
 
ear.startListening("sorry | hello robyn | goodbye | i love you")
 
# set up a message route from the ear --to--> python method "heard"
ear.addListener("recognized", python.name, "heard", String().getClass()); 
 
      
def heard():
    data = msg_ear_recognized.data[0]
 
 
 
    if (data == "sorry"):
        x = (random.randint(1, 3))
        if x == 1:
            mouth.speak("no problems")
        if x == 2:
            mouth.speak("it doesn't matter")
        if x == 3:
            mouth.speak("it's okay")
    
    if (data == "goodbye"):
        mouth.speak("goodbye")
        global helvar
        helvar = 1
        x = (random.randint(1, 4))
        if x == 1:
            mouth.speak("i'm looking forward to see you again")
        if x == 2:
            mouth.speak("see you soon")
    
    
    if (data == "hello robyn"):
        if helvar <= 2:    
            mouth.speak("hello")
            global helvar
            helvar += 1
        elif helvar == 3:
            mouth.speak("hello hello you have already said hello at least twice")
#            inMoov.moveArm("left",20,40,140,45)
#            inMoov.moveArm("right",20,150,164,45)
            inMoov.moveHand("left",0,0,0,0,0,119)
            inMoov.moveHand("right",0,0,0,0,0,119)
            sleep(2)
            armsdown()
            global helvar
            helvar += 1
        elif helvar == 4:
            mouth.speak("what is your problem stop saying hello all the time")
#            inMoov.moveArm("left",20,88,140,73)
#            inMoov.moveArm("right",20,85,164,80)
            inMoov.moveHand("left",130,180,180,180,180,119)
            inMoov.moveHand("right",130,180,180,180,180,119)
            sleep(2)
            armsdown()
            global helvar
            helvar += 1
        elif helvar == 5:
            mouth.speak("i will ignore you if you say hello one more time")
            lookright()
            global helvar
            helvar += 1
    
    if (data == "i love you"):
        mouth.speak("i love you too")
    
 
 
def armsdown():
  inMoov.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
  inMoov.setArmSpeed("right", 1.0, 1.0, 1.0, 1.0)
#  inMoov.moveArm("left",90,88,140,73)
#  inMoov.moveArm("right",77,85,164,80)
 
 
def lookforward():
  inMoov.setHeadSpeed(0.65, 0.75)
  inMoov.moveHead(74,90)
 
def lookright():
  inMoov.setHeadSpeed(0.65, 0.75)
  inMoov.moveHead(60,150)