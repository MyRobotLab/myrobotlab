#file : InMoov3.minimalTorso.py

# this will run with versions of MRL above 1695
# a very minimal script for InMoov
# although this script is very short you can still
# do voice control of a right Arm
# It uses WebkitSpeechRecognition, so you need to use Chrome as your default browser for this script to work

# Start the webgui service without starting the browser
webgui = Runtime.create("WebGui","WebGui")
webgui.autoStartBrowser(False)
webgui.startService()

# As an alternative you can use the line below to show all services in the browser. In that case you should comment out all lines above that starts with webgui. 
# webgui = Runtime.createAndStart("webgui","WebGui")

leftPort = "COM99"  #modify port according to your board
torsoPort = "COM100"

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
torso = i01.startTorso(torsoPort)  #modify port according to your board
# tweaking default torso settings
torso.topStom.setMinMax(0,180)
torso.topStom.map(0,180,67,110)
torso.midStom.setMinMax(0,180)
torso.midStom.map(0,180,60,120)
#torso.lowStom.setMinMax(0,180)
#torso.lowStom.map(0,180,60,110)
#torso.topStom.setRest(90)
#torso.midStom.setRest(90)
#torso.lowStom.setRest(90)

#################
# verbal commands
ear = i01.ear

ear.addCommand("attach everything", "i01", "attach")
ear.addCommand("disconnect everything", "i01", "detach")
ear.addCommand("attach torso", "i01.torso", "attach")
ear.addCommand("disconnect torso", "i01.torso", "detach")
ear.addCommand("rest", "python", "rest")
ear.addCommand("capture gesture", ear.getName(), "captureGesture")
ear.addCommand("manual", ear.getName(), "lockOutAllGrammarExcept", "voice control")
ear.addCommand("voice control", ear.getName(), "clearLock")
ear.addCommand("test your stomach", "python", "teststomach")

 # Confirmations and Negations are not supported yet in WebkitSpeechRecognition
# So commands will execute immediatley
ear.addComfirmations("yes","correct","ya","yeah", "yes please", "yes of course")
ear.addNegations("no","wrong","nope","nah","no thank you", "no thanks")

ear.startListening()

def teststomach():
    i01.setTorsoSpeed(0.75,0.55,0.75)
    i01.moveTorso(90,90,90)
    sleep(2)
    i01.moveTorso(45,90,90)
    sleep(4)
    i01.moveTorso(90,90,90)
    sleep(2)
    i01.moveTorso(135,90,90)
    sleep(4)
    i01.moveTorso(90,90,90)
    sleep(2)
    i01.moveTorso(90,45,90)
    sleep(3)
    i01.moveTorso(90,135,90)
    sleep(3)
    i01.moveTorso(90,90,45)
    sleep(3)
    i01.moveTorso(90,90,135)
    sleep(3)
