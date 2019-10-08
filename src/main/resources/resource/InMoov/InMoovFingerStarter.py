#file : InMoov.minimalFingerStarter.py
# MRL version >= Nixie
# this script is provided as a basic guide
# most parts can be run by uncommenting them
# InMoov now can be started in modular pieces through the skeleton.config
# although this script is very short you can still
# do voice control of a FingerStarter or hand
# It uses WebkitSpeechRecognition, so you need to use Chrome as your default browser for this script to work

# Change to the port that you use
rightPort = "COM9"
##############
#to tweak the default voice
Voice="Mark" #Male US voice 
#Voice="cmu-slt-hsmm" #Default female for MarySpeech
mouth = Runtime.createAndStart("i01.mouth", "MarySpeech")
#mouth.installComponentsAcceptLicense(Voice)
mouth.setVoice(Voice)
##############
# starting InMoov service
i01 = Runtime.create("i01", "InMoov")
#Force Arduino to connect (fix Todo)
right = Runtime.createAndStart("i01.right", "Arduino")
right.connect(rightPort)
##############
# Starting parts
i01.startEar()
# Start the webgui service without starting the browser
webgui = Runtime.create("WebGui","WebGui")
webgui.autoStartBrowser(False)
webgui.startService()
# Then start the browsers and show the WebkitSpeechRecognition service named i01.ear
webgui.startBrowser("http://localhost:8888/#/service/i01.ear")
# As an alternative you can use the line below to show all services in the browser. In that case you should comment out all lines above that starts with webgui. 
# webgui = Runtime.createAndStart("webgui","WebGui")
##############
i01.startMouth()
##############
rightHand = Runtime.create("i01.rightHand","InMoovHand")
# Tweaking defaults settings of right hand

# Mapping by setting your servo limits
rightHand.index.map(0,180,42,160)
# Rest position
rightHand.index.setRest(0)
##############
i01 = Runtime.start("i01","InMoov")
##############
i01.startRightHand(rightPort)
#i01.rightHand.setAutoEnable(True)
##############
# Verbal commands
ear = i01.ear
#always listen
#ear.setAutoListen(True)

ear.addCommand("attach your finger", "i01.rightHand.index", "enable")
ear.addCommand("disconnect your finger", "i01.rightHand.index", "disable")
ear.addCommand("rest", "i01.rightHand.index", "rest")## Hardcoded gesture
ear.addCommand("open your finger", "python", "fingeropen")
ear.addCommand("close your finger", "python", "fingerclose")
ear.addCommand("finger to the middle", "python", "fingermiddle")
ear.addCommand("capture gesture", ear.getName(), "captureGesture")
ear.addCommand("manual", ear.getName(), "lockOutAllGrammarExcept", "voice control")
ear.addCommand("voice control", ear.getName(), "clearLock")

# Confirmations and Negations are not supported yet in WebkitSpeechRecognition
# So commands will execute immediatley
# ear.addComfirmations("yes","correct","yeah","ya")
# ear.addNegations("no","wrong","nope","nah")

ear.startListening()

def fingeropen():
  i01.rightHand.index.setVelocity(20)## Low velocity
  i01.moveHand("right",0,0,0,0,0,0)## Thumb,index,majeure,ringfinger,pinky,wrist
  i01.mouth.speak("ok I open my finger")

def fingerclose():
  i01.rightHand.index.setVelocity(50)## Medium velocity
  i01.moveHand("right",180,180,180,180,180,90)
  i01.mouth.speak("my finger is closed")

def fingermiddle():
  i01.rightHand.index.setVelocity(-1)## Maximum velocity
  i01.moveHand("right",90,90,90,90,90,90)
  i01.mouth.speak("ok you have my attention")
