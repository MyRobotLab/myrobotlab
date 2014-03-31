# a basic script for starting the InMoov service
# and attaching the right hand
# an Arduino is required, additionally a computer
# with a microphone and speakers is needed for voice
# control and speech synthesis

inMoov = Runtime.createAndStart("inMoov", "InMoov")

# attach an arduino to InMoov
# possible board types include uno atmega168 atmega328p atmega2560 atmega1280 atmega32u4
# the MRLComm.ino sketch must be loaded into the Arduino for MyRobotLab control
# set COM number according to the com of your Arduino board
inMoov.attachArduino("right","uno","COM13")
inMoov.attachHand("right")

# setting speech's language
# regrettably voice recognition is only in
# English
# inMoov.setLanguage("fr")
# inMoov.setLanguage("it")
inMoov.setLanguage("en")


# system check
inMoov.systemCheck()

inMoov.rest()

# listen for these key words
inMoov.startListening("rest | open hand | close hand | one | two | three | four | five | manual | voice control| capture gesture")

# voice control
def heard():
  data = msg_ear_recognized.data[0]
  print "heard ", data
  #mouth.setLanguage("fr")
  
  mouth.speak("you said " + data)
  
  if (data == "rest"):
    inMoov.rest() 
  elif (data == "open hand"):
    inMoov.handOpen("right")
  elif (data == "close hand"):
    inMoov.handClose("right")
  elif (data == "manual"):
    inMoov.lockOutAllGrammarExcept("voice control")
  elif (data == "voice control"):
    inMoov.clearGrammarLock()
  elif (data == "capture gesture"):
    inMoov.captureGesture();

inMoov.moveHand("right",50,28,30,10,10,90)

def gestureOne():
  inMoov.moveHead(90,90)
  inMoov.moveArm("left",90,64,128,43)
  inMoov.moveArm("right",0,73,29,15)
  inMoov.moveHand("left",50,28,30,10,10,90)
  inMoov.moveHand("right",10,10,10,10,10,90)