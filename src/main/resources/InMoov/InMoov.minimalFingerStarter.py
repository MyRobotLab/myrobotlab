#file : InMoov3.minimalFingerStarter.py

# this will run with versions of MRL above 1695
# a very minimal script for InMoov
# although this script is very short you can still
# do voice control of a finger starter
# It uses WebkitSpeechRecognition, so you need to use Chrome as your default browser for this script to work
#The Finger Starter is considered here to be right index, 
#so make sure your servo is connected to pin3 of you Arduino

# Start the webgui service without starting the browser
webgui = Runtime.create("WebGui","WebGui")
webgui.autoStartBrowser(False)
webgui.startService()

# As an alternative you can use the line below to show all services in the browser. In that case you should comment out all lines above that starts with webgui. 
# webgui = Runtime.createAndStart("webgui","WebGui")

# Change to the port that you use
rightPort = "COM99"

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
# after the ear is started, you can launch a browser to use webkit with it.
# Then start the browsers and show the WebkitSpeechRecognition service named i01.ear
# webgui.startBrowser("http://localhost:8888/#/service/i01.ear")

i01.startMouth()
##############

# verbal commands
ear = i01.ear

ear.addCommand("attach your finger", "i01.rightHand.index", "attach")
ear.addCommand("disconnect your finger", "i01.rightHand.index", "detach")
ear.addCommand("rest", i01.getName(), "rest")
ear.addCommand("open your finger", "python", "fingeropen")
ear.addCommand("close your finger", "python", "fingerclose")
ear.addCommand("finger to the middle", "python", "fingermiddle")
ear.addCommand("capture gesture", ear.getName(), "captureGesture")
ear.addCommand("manual", ear.getName(), "lockOutAllGrammarExcept", "voice control")
ear.addCommand("voice control", ear.getName(), "clearLock")

# Confirmations and Negations are not supported yet in WebkitSpeechRecognition
# So commands will execute immediatley 
ear.addComfirmations("yes","correct","yeah","ya") 
ear.addNegations("no","wrong","nope","nah")

ear.startListening()
i01.startRightHand(rightPort)

def fingeropen():
  i01.moveHand("right",0,0,0,0,0)
  i01.mouth.speak("ok I open my finger")

def fingerclose():
  i01.moveHand("right",180,180,180,180,180)
  i01.mouth.speak("my finger is closed")

def fingermiddle():
  i01.moveHand("right",90,90,90,90,90)
  i01.mouth.speak("ok you have my attention")
