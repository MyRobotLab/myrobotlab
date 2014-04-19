# a head script for InMoov
inMoov = Runtime.createAndStart("inMoov", "InMoov")

# variables to adjust
leftSerialPort = "COM7"
cameraIndex = 1

# attach an arduino to InMoov
# possible board types include uno atmega168 atmega328p atmega2560 atmega1280 atmega32u4
# the MRLComm.ino sketch must be loaded into the Arduino for MyRobotLab control !
# set COM number according to the com of your Arduino board
inMoov.attachArduino("left","uno", leftSerialPort)
inMoov.attachHead("left")

# system check
inMoov.systemCheck()

# if you have a laptop with a camera the one in InMoov is likely to be index #1
inMoov.setCameraIndex(cameraIndex)
 
# listen for these key words
# to get voice to work - you must be attached to the internet for
# at least the first time
ear = inMoov.getEar()
ear.addCommand("rest", inMoov.getName(), "rest")
ear.addCommand("track", inMoov.getName(), "track")
ear.addCommand("freeze track", inMoov.getName(), "clearTrackingPoints")
ear.addCommand("capture gesture", inMoov.getName(), "captureGesture")
ear.addCommand("manual", ear.getName(), "lockOutAllGrammarExcept", "voice control")
ear.addCommand("voice control", ear.getName(), "clearLock")
ear.addCommand("camera on", inMoov.getName(), "cameraOn")
# ear.addCommand("off camera", inMoov.getName(), "cameraOff") FIXME


ear.addComfirmations("yes","correct","yeah","ya") 
ear.addNegations("no","wrong","nope","nah")

ear.startListening() 
