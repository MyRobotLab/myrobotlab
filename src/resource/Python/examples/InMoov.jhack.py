# a basic script for starting the InMoov service
# and attaching the right hand
# an Arduino is required, additionally a computer
# with a microphone and speakers is needed for voice
# control and speech synthesis

# ADD SECOND STAGE CONFIRMATION
# instead of saying: you said... it would say: did you say...? and I would confirm with yes or give the voice command again
# face tracking in InMoov ... activated by voice ...

inMoov = Runtime.createAndStart("inMoov", "InMoov")

#rightSerialPort = "COM8"
#leftSerialPort = "COM7"

leftHandComPort = "COM34"
rightHandComPort = "COM35"
headComPort = "COM16"
bodyComPort = "COM22"
cameraIndex = 0

# attach an arduinos to InMoov
# possible board types include uno atmega168 atmega328p atmega2560 atmega1280 atmega32u4
# the MRLComm.ino sketch must be loaded into the Arduino for MyRobotLab control

inMoov.attachArduino("body","atmega1280",bodyComPort)

inMoov.attachArduino("right","atmega328p",rightHandComPort)
#-- attach left hand and set servo pins thumb , index , majeure , ringFinger , pinky and wrist
#-- or can be attached using default pins by inMoov.attachHand("right")
inMoov.attachHand("right",3,5,6,9,10,11)
# -- attach right arm to arduinobody set pin bicep , rotate ,shoulder and omplate
# -- this also can be set to right arduino by inMoov.attachArm("right",3,4,5,6)
# -- and can be done by inMoov.attachArm("right") with all the defaults
inMoov.attachArm("right","body",3,4,5,6)
##------------------------------------- need to move wrist to arduino body---------------------------
sleep(2)
wristright.detach()
wristright.setInverted(True)
arduinobody.servoAttach("wristright",2)

# ------------ inverte all servos needed
thumbright.setInverted(True)
indexright.setInverted(True)
majeureright.setInverted(True)
ringFingerright.setInverted(True)
shoulderright.setInverted(True)
# --------- finsh up and publish state -------
wristright.publishState()

##-------------------- attach left side ----------------------------
inMoov.attachArduino("left","atmega328p", leftHandComPort)
#-- attach left hand and set servo pins thumb , index , majeure , ringFinger , pinky and wrist
inMoov.attachHand("left",3,5,6,9,10,11)
# -- attach left arm to arduinobody set pin bicep , rotate ,shoulder and omplate
inMoov.attachArm("left","body",12,11,10,9)

##------------------------------------- need to move wrist to arduino body ---------------------------
sleep(2)
wristleft.detach()
arduinobody.servoAttach("wristleft",13)

# ------------ invert left boby parts
indexleft.setInverted(True)
ringFingerleft.setInverted(True)
# --------- all right hand fingers have been moved -------
wristleft.publishState()
##-------------------- all done
inMoov.attachArduino("head","atmega328p",headComPort)
## attach to arduino head and set pins for eye x , eye y , neck and rotHead
#-- note that eye x and eye y are not in use at the moment.
inMoov.attachHead("head",3,5,11,6)
#inMoov.attachMouthControl("head",12) to conect servo to pin 12
#inMoov.attachMouthControl("head",12,0 , 20) to set servo to pin 12 and servo to poition 0 for close, poition 20 for open
inMoov.attachMouthControl("head")

# set voice to male
mouth.setGoogleProxy("jarvis","91.243.165.49",8080)
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
inMoov.moveArm("right",0,91,0,17)
inMoov.moveHand("left",126,7,158,140,148,0)
inMoov.moveHand("right",100,128,134,154,166,0)