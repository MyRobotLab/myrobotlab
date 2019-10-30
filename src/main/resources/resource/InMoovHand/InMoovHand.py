#########################################
# InMoovHand.py
# more info @: http://myrobotlab.org/service/InMoovHand
#########################################


# this script is provided as a basic scripting guide
# most parts can be run by uncommenting them
# InMoov now can be started in modular pieces through the .config files from full script
# although this script is very short you can still
# do voice control of a right hand or finger box
# It uses WebkitSpeechRecognition, so you need to use Chrome as your default browser for this script to work

# Change to the port that you use
rightPort = "COM9"

# start optional virtual arduino service, used for internal test and virtual inmoov
# virtual=True
if ('virtual' in globals() and virtual):
    virtualArduinoRight = Runtime.start("virtualArduinoRight", "VirtualArduino")
    virtualArduinoRight.connect(rightPort)
# end used for internal test

#to tweak the default voice
#Voice="cmu-slt-hsmm" #Default female for MarySpeech
Voice="cmu-bdl-hsmm" #Male US voice 
#mouth.installComponentsAcceptLicense(Voice)

mouth = Runtime.start("i01.mouth", "MarySpeech")
mouth.setVoice(Voice)

##############
# starting InMoov service
i01 = Runtime.create("i01", "InMoov")
#Force Arduino to connect (fix Todo)
right = Runtime.createAndStart("i01.right", "Arduino")
right.connect(rightPort)
##############
# starting parts
i01.startEar()

# Start the webgui service without starting the browser
webgui = Runtime.create("webgui","WebGui")
webgui.autoStartBrowser(False)
webgui.startService()
# Then start the browsers and show the WebkitSpeechRecognition service named i01.ear
webgui.startBrowser("http://localhost:8888/#/service/i01.ear")
# As an alternative you can use the line below to show all services in the browser. In that case you should comment out all lines above that starts with webgui. 
# webgui = Runtime.createAndStart("webgui","WebGui")

i01.startMouth()
##############
rightHand = Runtime.create("i01.rightHand","InMoovHand")

# tweaking defaults settings of right hand

#Velocity
rightHand.thumb.setVelocity(-1)
rightHand.index.setVelocity(-1)
rightHand.majeure.setVelocity(-1)
rightHand.ringFinger.setVelocity(-1)
rightHand.pinky.setVelocity(-1)
rightHand.wrist.setVelocity(-1)
#Mapping
rightHand.thumb.map(0,180,64,135)
rightHand.index.map(0,180,42,160)
rightHand.majeure.map(0,180,35,165)
rightHand.ringFinger.map(0,180,40,140)
rightHand.pinky.map(0,180,45,130)
rightHand.wrist.map(0,180,30,135)
#Rest position
rightHand.thumb.setRest(0)
rightHand.index.setRest(0)
rightHand.majeure.setRest(0)
rightHand.ringFinger.setRest(0)
rightHand.pinky.setRest(0)
rightHand.wrist.setRest(0)

#################
i01 = Runtime.start("i01","InMoov")
################# 
i01.startRightHand(rightPort)

#auto disable power
i01.rightHand.setAutoDisable(True)

#################
# verbal commands
ear = i01.ear
setAutoListen=True
i01.ear.setAutoListen(setAutoListen)

ear.addCommand("attach your right hand", "i01.rightHand", "enable")
ear.addCommand("disconnect your right hand", "i01.rightHand", "disable")
ear.addCommand("rest", "i01.rightHand", "rest")#hardcoded gesture
ear.addCommand("relax", "python", "relax")
ear.addCommand("open your hand", "python", "handopen")
ear.addCommand("close your hand", "python", "handclose")
ear.addCommand("capture gesture", ear.getName(), "captureGesture")
ear.addCommand("manual", ear.getName(), "lockOutAllGrammarExcept", "voice control")
ear.addCommand("voice control", ear.getName(), "clearLock")

# Confirmations and Negations are not supported yet in WebkitSpeechRecognition
# So commands will execute immediatley
ear.addComfirmations("yes","correct","yeah","ya")
ear.addNegations("no","wrong","nope","nah")

ear.startListening()

def relax():
  i01.setHandVelocity("right", 30, 30, 30, 30, 30, 30)
  i01.moveHand("right",90,90,90,90,90,140)

def handopen():
  i01.setHandVelocity("right", -1.0, -1.0, -1.0, -1.0, -1.0, -1.0)
  i01.moveHand("right",0,0,0,0,0,0)
  i01.mouth.speak("ok I open my hand")

def handclose():
  i01.setHandVelocity("right", -1.0, -1.0, -1.0, -1.0, -1.0, -1.0)
  i01.moveHand("right",180,180,180,180,180,180)
  i01.mouth.speak("a nice and closed hand that is")

#set the hand to relax() at launch
relax() 
