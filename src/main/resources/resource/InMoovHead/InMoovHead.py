#########################################
# InMoovHead.py
# more info @: http://myrobotlab.org/service/InMoovHead
#########################################


# this script is provided as a basic scripting guide
# most parts can be run by uncommenting them
# InMoov now can be started in modular pieces through the .config files from full script
# although this script is very short you can still
# do voice control of a InMoov head
# It uses WebkitSpeechRecognition, so you need to use Chrome as your default browser for this script to work


# Change to the port that you use
leftPort = "COM9"
rightPort = "COM7"


# start optional virtual arduino service, used for internal test and virtual inmoov
# virtual=True
if ('virtual' in globals() and virtual):
    virtualArduinoLeft = Runtime.start("virtualArduinoLeft", "VirtualArduino")
    virtualArduinoLeft.connect(leftPort)
    virtualArduinoRight = Runtime.start("virtualArduinoRight", "VirtualArduino")
    virtualArduinoRight.connect(rightPort)
# end used for internal test


#to tweak the default voice 
Voice="cmu-bdl-hsmm" #Default male for MarySpeech

mouth = Runtime.createAndStart("i01.mouth", "MarySpeech")
#mouth.installComponentsAcceptLicense(Voice)
mouth.setVoice(Voice)
##############
# starting InMoov service
i01 = Runtime.create("i01", "InMoov")
#Force Arduino to connect (fix Todo)
left = Runtime.createAndStart("i01.left", "Arduino")
left.connect(leftPort)
right = Runtime.createAndStart("i01.right", "Arduino")
right.connect(rightPort)
##############
# starting parts
i01.startEar()
# Start the WebGui service without starting the browser
WebGui = Runtime.create("webgui","WebGui")
WebGui.autoStartBrowser(False)
WebGui.startService()
# Then start the browsers and show the WebkitSpeechRecognition service named i01.ear
WebGui.startBrowser("http://localhost:8888/#/service/i01.ear")
# As an alternative you can use the line below to show all services in the browser. In that case you should comment out all lines above that starts with WebGui. 
# WebGui = Runtime.createAndStart("WebGui","WebGui")
##############
i01.startMouth()
##############
head = Runtime.create("i01.head","InMoovHead")
eyelids = Runtime.create("i01.eyelids", "InMoovEyelids")
##############
# tweaking defaults settings of right hand

# Mapping
head.jaw.map(0,180,55,95)
head.eyeY.map(0,180,85,110)
head.eyeX.map(0,180,75,120)
head.neck.map(0,180,75,128)
head.rothead.map(0,180,60,130)
head.rollNeck.map(0,180,65,125)
eyelids.eyelidleft.map(0,180,55,135)
eyelids.eyelidright.map(0,180,55,135)
# Rest position
head.jaw.setRest(0)
head.eyeY.setRest(90)
head.eyeX.setRest(90)
head.neck.setRest(90)
head.rothead.setRest(90)
head.rollNeck.setRest(90)
eyelids.eyelidleft.setRest(90)
eyelids.eyelidright.setRest(90)
#################
i01 = Runtime.start("i01","InMoov")
################# 
i01.startHead(leftPort)
i01.startOpenCV()
i01.startEyelids(right,22,24)
#################
#We attach these three parts to the right Arduino
i01.head.rollNeck.attach(right,12)
i01.eyelids.eyelidleft.attach(right,22)
i01.eyelids.eyelidright.attach(right,24)
#i01.eyelids.autoBlink(True)
#################
head.setAutoDisable(True)
#################
#i01.startEyesTracking()
i01.startHeadTracking()
i01.startMouthControl()
i01.mouthControl.setmouth(0,80)
############################################################
#to tweak the default PID values
#i01.eyesTracking.pid.setPID("eyeX",12.0,1.0,0.1)
#i01.eyesTracking.pid.setPID("eyeY",12.0,1.0,0.1)
i01.headTracking.pid.setPID("rothead",5.0,1.0,0.1)
i01.headTracking.pid.setPID("neck",5.0,1.0,0.1)
#################
# verbal commands
ear = i01.ear
setAutoListen=True
i01.ear.setAutoListen(setAutoListen)
 
ear.addCommand("rest", "i01.head", "rest")#hardcoded gesture
ear.addCommand("full speed", "python", "fullspeed")
ear.addCommand("relax", "python", "relax")

ear.addCommand("attach everything", "i01", "enable")
ear.addCommand("disconnect everything", "i01", "disable")
ear.addCommand("attach head", "i01.head", "enable")
ear.addCommand("disconnect head", "i01.head", "disable")
ear.addCommand("attach eyelids", "i01.eyelids", "enable")
ear.addCommand("disconnect eyelids", "i01.eyelids", "disable")
ear.addCommand("capture gesture", ear.getName(), "captureGesture")
ear.addCommand("manual", ear.getName(), "lockOutAllGrammarExcept", "voice control")
ear.addCommand("voice control", ear.getName(), "clearLock")

ear.addCommand("search humans", "python", "trackHumans")
ear.addCommand("quit search", "python", "stopTracking")
ear.addCommand("track", "python", "trackPoint")
ear.addCommand("freeze track", "python", "stopTracking")
ear.addCommand("face recognizer", "python", "facerecognizer")

ear.addCommand("look on your right side", "python", "lookrightside")
ear.addCommand("look on your left side", "python", "lookleftside")
ear.addCommand("look in the middle", "python", "lookinmiddle")

# Confirmations and Negations are not supported yet in WebkitSpeechRecognition
# So commands will execute immediatley
ear.addComfirmations("yes","correct","yeah","ya")
ear.addNegations("no","wrong","nope","nah")

ear.startListening()

#updated gestures : https://github.com/MyRobotLab/inmoov/tree/develop/InMoov/gestures

def relax():
  i01.setHeadVelocity(30, 30, 30)
  i01.moveHead(90,90,90)
  i01.mouth.speak("I am relaxed")

def fullspeed():
  i01.setHeadVelocity(-1, -1, -1, -1, -1, -1)
  i01.setEyelidsVelocity(-1, -1)
  i01.mouth.speak("All my servos are set to full speed")  

def lookrightside():
  i01.setHeadVelocity(40, 40, 40)
  i01.moveHead(80,40,20)

def lookleftside():
  i01.setHeadVelocity(40, 40, 40)
  i01.moveHead(80,140,160)

def lookinmiddle():
  i01.setHeadVelocity(40, 40, 40)
  i01.moveHead(90,90,90)

def trackHumans():
  i01.trackHumans()
  i01.mouth.speak("I start my tracking")

def trackPoint():
  i01.trackPoint()
  #i01.eyesTracking.startLKTracking()
  i01.mouth.speak("I am tracking the point")
  fullspeed()

def stopTracking():
  i01.stopTracking()
  #i01.eyesTracking.stopTracking()
  i01.mouth.speak("I am stoping my tracking")

def facerecognizer(): 
  #you need to train at least 2 FACES !   
  i01.cameraOn()
  fr=i01.vision.setActiveFilter("FaceRecognizer")
  fr.train()# it takes some time to train and be able to recognize face
