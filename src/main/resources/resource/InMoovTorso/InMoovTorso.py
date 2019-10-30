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
torso = Runtime.create("i01.torso","InMoovTorso")
# tweaking default torso settings
#Velocity
torso.topStom.setVelocity(-1)
torso.midStom.setVelocity(-1)
#torso.lowStom.setVelocity(-1)
#Mapping
torso.topStom.map(0,180,65,120)
torso.midStom.map(0,180,70,110)
#torso.lowStom.map(0,180,60,110)
#Rest position
torso.topStom.setRest(90)
torso.midStom.setRest(90)
#torso.lowStom.setRest(90)
#################
i01 = Runtime.start("i01","InMoov")
################# 
i01.startTorso(leftPort)
if ('virtual' in globals() and virtual):i01.startVinMoov()
i01.torso.setAutoDisable(True)
#################
# verbal commands
ear = i01.ear
setAutoListen=True
i01.ear.setAutoListen(setAutoListen)

ear.addCommand("attach everything", "i01", "enable")
ear.addCommand("disconnect everything", "i01", "disable")
ear.addCommand("attach torso", "i01.torso", "enable")
ear.addCommand("disconnect torso", "i01.torso", "disable")
ear.addCommand("rest", "i01.torso", "rest")#Hardcoded gesture
ear.addCommand("relax", "python", "relax")
ear.addCommand("full speed", "python", "fullspeed")
ear.addCommand("capture gesture", ear.getName(), "captureGesture")
ear.addCommand("manual", ear.getName(), "lockOutAllGrammarExcept", "voice control")
ear.addCommand("voice control", ear.getName(), "clearLock")
ear.addCommand("test your stomach", "python", "teststomach")
ear.addCommand("to the right", "python", "totheright")
ear.addCommand("to the left", "python", "totheleft")
ear.addCommand("turn to the right side", "python", "turnrightside")
ear.addCommand("turn to the left side", "python", "turnleftside")

ear.startListening()

def relax():
  i01.setTorsoVelocity(30,20,30)
  i01.moveTorso(90,90,90)


def fullspeed():
  i01.setTorsoVelocity( -1, -1, -1)
  i01.mouth.speak("All my servos are set to full speed")    

def totheright():
  i01.setTorsoVelocity(30,20,30)
  i01.moveTorso(135,90,90)

def totheleft():
  i01.setTorsoVelocity(30,20,30)
  i01.moveTorso(45,90,90)

def turnrightside():
  i01.setTorsoVelocity(30,20,30)
  i01.moveTorso(90,45,90)

def turnleftside():
  i01.setTorsoVelocity(30,20,30)
  i01.moveTorso(90,135,90)    

def teststomach():
  i01.setTorsoVelocity(30,20,30)
  i01.moveTorsoBlocking(90,90,90)
  i01.moveTorsoBlocking(45,90,90)
  i01.moveTorsoBlocking(90,90,90)
  i01.moveTorsoBlocking(135,90,90)
  i01.moveTorsoBlocking(90,90,90)
  i01.moveTorsoBlocking(90,45,90)
  sleep(3)
  i01.moveTorsoBlocking(90,135,90)
  i01.moveTorsoBlocking(90,90,45)
  i01.moveTorsoBlocking(90,90,135)
