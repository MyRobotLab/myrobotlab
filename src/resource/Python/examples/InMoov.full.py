# a basic script for starting the InMoov service
# and attaching the right hand
# an Arduino is required, additionally a computer
# with a microphone and speakers is needed for voice
# control and speech synthesis

# ADD SECOND STAGE CONFIRMATION
#  instead of saying: you said... it would say: did you say...? and I would confirm with yes or give the voice command again
#  face tracking in InMoov ... activated by voice ...

inMoov = Runtime.createAndStart("inMoov", "InMoov")

rightSerialPort = "COM8"
leftSerialPort = "COM7"
cameraIndex = 1

# attach an arduinos to InMoov
# possible board types include uno atmega168 atmega328p atmega2560 atmega1280 atmega32u4
# the MRLComm.ino sketch must be loaded into the Arduino for MyRobotLab control
inMoov.attachArduino("right","uno",rightSerialPort)
inMoov.attachHand("right")
inMoov.attachArm("right")

inMoov.attachArduino("left","atmega1280", leftSerialPort)
inMoov.attachHand("left")
inMoov.attachArm("left")
inMoov.attachHead("left")

# system check
inMoov.systemCheck()
inMoov.rest()
inMoov.setCameraIndex(cameraIndex)

# new process for verbal commands
ear = inMoov.getEar()
ear.addCommand("rest", inMoov.getName(), "rest")
ear.addCommand("open hand", inMoov.getName(), "handOpen", "both")
ear.addCommand("close hand", inMoov.getName(), "handClose", "both")
ear.addCommand("camera on", inMoov.getName(), "cameraOn")
# ear.addCommand("off camera", inMoov.getName(), "cameraOff") - needs fixing
ear.addCommand("capture gesture", inMoov.getName(), "captureGesture")
ear.addCommand("track", inMoov.getName(), "track")
ear.addCommand("freeze track", inMoov.getName(), "clearTrackingPoints")
ear.addCommand("hello", inMoov.getName(), "hello")
ear.addCommand("giving", inMoov.getName(), "giving")
ear.addCommand("fighter", inMoov.getName(), "fighter")
ear.addCommand("fist hips", inMoov.getName(), "fistHips")
ear.addCommand("look at this", inMoov.getName(), "lookAtThis")
ear.addCommand("victory", inMoov.getName(), "victory")
ear.addCommand("arms up", inMoov.getName(), "armsUp")
ear.addCommand("arms front", inMoov.getName(), "armsFront")
ear.addCommand("da vinci", inMoov.getName(), "daVinci")

ear.addCommand("manual", ear.getName(), "lockOutAllGrammarExcept", "voice control")
ear.addCommand("voice control", ear.getName(), "clearLock")
ear.addCommand("stop listening", ear.getName(), "stopListening")

ear.addCommand("ok", "python", "ok")

ear.addComfirmations("yes","correct","yeah","ya") 
ear.addNegations("no","wrong","nope","nah")

ear.startListening()

def ok():
  inMoov.setHandSpeed("left", 0.30, 0.30, 1.0, 1.0, 1.0, 1.0)
  inMoov.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  inMoov.setArmSpeed("left", 0.75, 0.75, 0.75, 0.95)
  inMoov.setArmSpeed("right", 1.0, 1.0, 1.0, 1.0)
  inMoov.setHeadSpeed(0.65, 0.75)
  inMoov.moveHead(88,79)
  inMoov.moveArm("left",89,75,93,11)
  inMoov.moveArm("right",0,91,28,17)
  inMoov.moveHand("left",92,106,4,0,0,34)
  inMoov.moveHand("right",86,51,133,162,153,180)
