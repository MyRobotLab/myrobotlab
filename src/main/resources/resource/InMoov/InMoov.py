#########################################
# InMoov.py
# more info @: http://myrobotlab.org/service/InMoov
#########################################
# a very minimal script for InMoov
# this script is provided as a basic guide for InMoov service
# InMoov now can be started in modular pieces through the skeleton.config from full script
# although this script is very short you can still
# do voice control of a FingerStarter or hand
# It uses WebkitSpeechRecognition, so you need to use Chrome as your default browser for this script to work

# set the system to "virtual" - this will simulate actual hardware
Platform.setVirtual(True)

# set the ports of the micro-controllers
rightPort = "COM8"
leftPort = "COM10"

# start the main service - it will start others
i01 = Runtime.start("i01", "InMoov")

# starting parts
i01.setLanguage("en-US")
i01.startAll(leftPort,rightPort)

# load gestures, uncomment :
##i01.loadGestures("InMoov/gestures");

# chatbot, uncomment :
##i01.chatBot=Runtime.start("i01.chatBot", "ProgramAB")
##htmlFilter=Runtime.start("htmlFilter", "HtmlFilter")
##i01.chatBot.addTextListener(htmlFilter)
##htmlFilter.addListener("publishText", "i01", "speak")
##i01.chatBot.attach(i01.ear)
##i01.startBrain();

i01.startVinMoov()

# Start the WebGui service without starting the browser
WebGui = Runtime.create("webgui","WebGui")
WebGui.autoStartBrowser(False)
WebGui.startService()
# Then start the browsers and show the WebkitSpeechRecognition service named i01.ear
WebGui.startBrowser("http://localhost:8888/#/service/i01.ear")

# verbal commands
i01.ear.setAutoListen(True)

ear.addCommand("attach right hand", "i01.rightHand", "attach")
ear.addCommand("disconnect right hand", "i01.rightHand", "detach")
ear.addCommand("rest", i01.getName(), "rest")
ear.addCommand("open hand", "python", "handopen")
ear.addCommand("close hand", "python", "handclose")
ear.addCommand("capture gesture", ear.getName(), "captureGesture")
ear.addCommand("manual", ear.getName(), "lockOutAllGrammarExcept", "voice control")
ear.addCommand("voice control", ear.getName(), "clearLock")

ear.addComfirmations("yes","correct","yeah","ya")
ear.addNegations("no","wrong","nope","nah")

ear.startListening()

def handopen():
  i01.moveHand("left",0,0,0,0,0)
  i01.moveHand("right",0,0,0,0,0)

def handclose():
  i01.moveHand("left",180,180,180,180,180)
  i01.moveHand("right",180,180,180,180,180)
