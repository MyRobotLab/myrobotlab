#file : InMoov3.minimal.py

# Change to the port that you use (Update this to your actual com ports.
rightPort = "COM99"


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
# After the ear is started, you can launch a browser (uncomment the following to do that.)
# webgui.startBrowser("http://localhost:8888/#/service/i01.ear")
i01.startMouth()
##############
i01.startRightHand(rightPort)
# tweaking defaults settings of right hand
#i01.rightHand.thumb.setMinMax(55,135)
#i01.rightHand.index.setMinMax(0,160)
#i01.rightHand.majeure.setMinMax(0,140)
#i01.rightHand.ringFinger.setMinMax(48,145)
#i01.rightHand.pinky.setMinMax(45,146)
#i01.rightHand.thumb.map(0,180,55,135)
#i01.rightHand.index.map(0,180,0,160)
#i01.rightHand.majeure.map(0,180,0,140)
#i01.rightHand.ringFinger.map(0,180,48,145)
#i01.rightHand.pinky.map(0,180,45,146)
#################

# verbal commands
ear = i01.ear

ear.addCommand("attach your right hand", "i01.rightHand", "attach")
ear.addCommand("disconnect your right hand", "i01.rightHand", "detach")
ear.addCommand("rest", i01.getName(), "rest")
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


def handopen():
  i01.moveHand("right",0,0,0,0,0)
  i01.mouth.speak("ok I open my hand")

def handclose():
  i01.moveHand("right",180,180,180,180,180)
  i01.mouth.speak("a nice and wide open hand that is")
