#file : InMoov3.minimalHead.py

# this will run with versions of MRL above 1695
# a very minimal script for InMoov
# although this script is very short you can still
# do voice control of a right hand or finger box
# It uses WebkitSpeechRecognition, so you need to use Chrome as your default browser for this script to work

# Start the webgui service without starting the browser
webgui = Runtime.create("WebGui","WebGui")
webgui.autoStartBrowser(False)
webgui.startService()

# As an alternative you can use the line below to show all services in the browser. In that case you should comment out all lines above that starts with webgui. 
# webgui = Runtime.createAndStart("webgui","WebGui")

# Change to the port that you use
leftPort  = "COM99"
rightPort = "COM100"

#to tweak the default voice
Voice="cmu-slt-hsmm" # Default female for MarySpeech 
#Voice="cmu-bdl" #Male US voice.You need to add the necessary file.jar to myrobotlab.1.0.XXXX/library/jar
#https://github.com/MyRobotLab/pyrobotlab/blob/ff6e2cef4d0642e47ee15e353ef934ac6701e713/home/hairygael/voice-cmu-bdl-5.2.jar
voiceType = Voice
mouth = Runtime.createAndStart("i01.mouth", "MarySpeech")
mouth.setVoice(voiceType)
##############
# starting parts
i01 = Runtime.createAndStart("i01", "InMoov")
# Turn off the spoken status updates from the inmoov starting. (you will want to comment this out when you use this script for real.)
# The inmoov is set to be mute for the unit tests in MRL.
i01.setMute(True)

i01.startEar()
# Then start the browsers and show the WebkitSpeechRecognition service named i01.ear
# webgui.startBrowser("http://localhost:8888/#/service/i01.ear")

i01.startMouth()

##############
i01.startHead(leftPort)
##############
i01.head.jaw.setMinMax(42,101)
i01.head.jaw.map(0,180,42,101)
i01.head.jaw.setRest(42)
# tweaking default settings of eyes
i01.head.eyeY.map(0,180,85,110)
i01.head.eyeY.setMinMax(0,180)
i01.head.eyeY.setRest(90)
i01.head.eyeX.map(0,180,75,120)
i01.head.eyeX.setMinMax(0,180)
i01.head.eyeX.setRest(90)
i01.head.neck.map(0,180,75,128)
i01.head.neck.setMinMax(0,180)
i01.head.neck.setRest(90)
i01.head.rothead.map(0,180,60,130)
i01.head.rothead.setMinMax(0,180)
i01.head.rothead.setRest(90)
#################
# start eye tracking with the eyex and the eyey pins
i01.startEyesTracking(leftPort,22,24)
# start head tracking with the neck and rothead pins
i01.startHeadTracking(leftPort,12,13)
############################################################
#to tweak the default PID values
i01.eyesTracking.pid.setPID("eyeX",12.0,1.0,0.1)
i01.eyesTracking.pid.setPID("eyeY",12.0,1.0,0.1)
i01.headTracking.pid.setPID("rothead",5.0,1.0,0.1)
i01.headTracking.pid.setPID("neck",5.0,1.0,0.1)
############################################################
# the "ear" of the inmoov TODO: replace this with just base inmoov ear?
ear = Runtime.createAndStart("i01.ear", "WebkitSpeechRecognition")
ear.addListener("publishText", python.name, "heard");
ear.addMouth(mouth)
############################################################
def heard(data):
  print "Speech Recognition Data:"+str(data)
######################################################################
 
ear.addCommand("rest", "python", "rest")

ear.addCommand("attach head", "i01.head", "attach")
ear.addCommand("disconnect head", "i01.head", "detach")
ear.addCommand("attach eyes", "i01.head.eyeY", "attach")
ear.addCommand("disconnect eyes", "i01.head.eyeY", "detach")
ear.addCommand("capture gesture", ear.getName(), "captureGesture")
ear.addCommand("manual", ear.getName(), "lockOutAllGrammarExcept", "voice control")
ear.addCommand("voice control", ear.getName(), "clearLock")

ear.addCommand("search humans", "python", "trackHumans")
ear.addCommand("quit search", "python", "stopTracking")
ear.addCommand("track", "python", "trackPoint")
ear.addCommand("freeze track", "python", "stopTracking")

ear.addCommand("look on your right side", "python", "lookrightside")
ear.addCommand("look on your left side", "python", "lookleftside")
ear.addCommand("look in the middle", "python", "lookinmiddle")

# Confirmations and Negations are not supported yet in WebkitSpeechRecognition
# So commands will execute immediatley
ear.addComfirmations("yes","correct","yeah","ya")
ear.addNegations("no","wrong","nope","nah")

ear.startListening()

def fullspeed():
  i01.setHeadVelocity(-1, -1, -1, -1, -1, -1)
  
def lookrightside():
  i01.setHeadSpeed(0.70, 0.70)
  i01.moveHead(90,40)

def lookleftside():
  i01.setHeadSpeed(0.70, 0.70)
  i01.moveHead(90,130)

def lookinmiddle():
  i01.setHeadSpeed(0.70, 0.70)
  i01.moveHead(90,90)

def trackHumans():
  i01.headTracking.faceDetect()
  i01.eyesTracking.faceDetect()
  fullspeed()

def trackPoint():
  i01.headTracking.startLKTracking()
  i01.eyesTracking.startLKTracking()
  fullspeed()

def stopTracking():
  i01.headTracking.stopTracking()
  i01.eyesTracking.stopTracking()




