#file : InMoov2.full3.byGael.Langevin.1.py

# this script is provided as a basic guide
# most parts can be run by uncommenting them
# InMoov now can be started in modular pieces
 
leftPort = "COM7"
rightPort = "COM8"
 
i01 = Runtime.createAndStart("i01", "InMoov")
#cleverbot = Runtime.createAndStart("cleverbot","CleverBot")
 
# starts everything
##i01.startAll(leftPort, rightPort)
 
# starting parts
i01.startMouth()
#to tweak the default voice
i01.mouth.setGoogleURI("http://thehackettfamily.org/Voice_api/api2.php?voice=Ryan&txt=")


i01.startHead(leftPort)
# tweaking default settings of eyes
i01.head.eyeY.setMinMax(75,95)
i01.head.eyeY.map(0,180,75,95)
i01.head.eyeY.setRest(85)
i01.head.eyeX.setMinMax(70,100)
i01.head.eyeX.map(0,180,70,100)
i01.head.eyeX.setRest(85)
i01.head.neck.setRest(80)
i01.head.rothead.setRest(86)
##############
i01.startMouthControl(leftPort)
# tweaking default settings of jaw
i01.head.jaw.setMinMax(8,55)
i01.mouthControl.setmouth(8,38)
##############
i01.startEar()
##############
torso = i01.startTorso("COM7")
# tweaking the torso settings
#torso.topStom.setMinMax(60,120)
torso.midStom.setMinMax(60,120)
#torso.lowStom.setMinMax(0,180)
#torso.topStom.setRest(90)
#torso.midStom.setRest(90)
#torso.lowStom.setRest(90)
##############
i01.startLeftHand(leftPort)
# tweaking default settings of left hand
i01.leftHand.thumb.setMinMax(45,140)
i01.leftHand.index.setMinMax(40,140)
i01.leftHand.majeure.setMinMax(30,176)
i01.leftHand.ringFinger.setMinMax(25,175)
i01.leftHand.pinky.setMinMax(35,130)
i01.leftHand.thumb.map(0,180,45,140)
i01.leftHand.index.map(0,180,40,140)
i01.leftHand.majeure.map(0,180,30,176)
i01.leftHand.ringFinger.map(0,180,25,175)
i01.leftHand.pinky.map(0,180,35,130)
################
i01.startLeftArm(leftPort)
#tweak defaults LeftArm
#i01.leftArm.bicep.setMinMax(0,90)
#i01.leftArm.rotate.setMinMax(46,160)
#i01.leftArm.shoulder.setMinMax(30,100)
#i01.leftArm.omoplate.setMinMax(10,75)
################
i01.startRightHand(rightPort)
# tweaking defaults settings of right hand
i01.rightHand.thumb.setMinMax(55,135)
i01.rightHand.index.setMinMax(0,160)
i01.rightHand.majeure.setMinMax(50,170)
i01.rightHand.ringFinger.setMinMax(48,145)
i01.rightHand.pinky.setMinMax(30,168)
i01.rightHand.thumb.map(0,180,55,135)
i01.rightHand.index.map(0,180,0,160)
i01.rightHand.majeure.map(0,180,50,170)
i01.rightHand.ringFinger.map(0,180,48,145)
i01.rightHand.pinky.map(0,180,45,146)
#################
i01.startRightArm(rightPort)
# tweak default RightArm
#i01.rightArm.bicep.setMinMax(0,90)
#i01.rightArm.rotate.setMinMax(46,160)
#i01.rightArm.shoulder.setMinMax(30,100)
#i01.rightArm.omoplate.setMinMax(10,75)
#################
i01.startHeadTracking(leftPort)
i01.startEyesTracking(leftPort)
################

# starting part with a reference, with a reference
# you can interact further
#opencv = i01.startOpenCV()
#opencv.startCapture()
# or you can use i01's reference
#i01.opencv.startCapture()

#i01.headTracking.faceDetect()
#i01.eyesTracking.faceDetect()
#i01.headTracking.pyramidDown()
############################################################
#to tweak the default PID values
i01.headTracking.xpid.setPID(10.0,5.0,0.1)
i01.headTracking.ypid.setPID(10.0,5.0,0.1)
i01.eyesTracking.xpid.setPID(15.0,5.0,0.1)
i01.eyesTracking.ypid.setPID(15.0,5.0,0.1)
############################################################

i01.startPIR("COM8",12)
 
 
 
def input():
    print 'python object is ', msg_clock_pulse
    pin = msg_i01_right_publishPin.data[0]
    print 'pin data is ', pin.pin, pin.value
    if (pin.value == 1):
      i01.powerUp()
      mouth.speak("who's there")
      head.neck.moveTo(80)
      sleep(2)
      head.neck.moveTo(86)

 
# after a start you may call detach to detach all
# currently attached servos
#i01.detach()
#i01.attach()
 
# auto detaches any attached servos after 120 seconds of inactivity
#i01.autoPowerDownOnInactivity(120)


#i01.speakErrors(false)
# purges any "auto" methods
#i01.purgeAllTasks()
 
# remote control services
# WebGUI - for more information see
# http://myrobotlab.org/service/WebGUI
 
# XMPP - for more information see
# http://myrobotlab.org/service/XMPP
 
# system check - called at anytime
#i01.systemCheck()
 
# take the current position of all attached servos <- FIXME
# and create a new method named "newGesture"
#i01.captureGesture("newGesture")
 
# all ear associations are done python startEar() only starts
# the peer service
# After ear.startListening(), the ear will listen for commands
 
# i01.systemCheck()

#i01.mouth.speakBlocking(cleverbot.chat("hi"))
#i01.mouth.speakBlocking(cleverbot.chat("how are you"))
 
# verbal commands
ear = i01.ear
 
ear.addCommand("rest", "python", "rest")

ear.addCommand("attach head", "i01.head", "attach")
ear.addCommand("disconnect head", "i01.head", "detach")
ear.addCommand("attach eyes", "i01.head.eyeY", "attach")
ear.addCommand("disconnect eyes", "i01.head.eyeY", "detach")
ear.addCommand("attach right hand", "i01.rightHand", "attach")
ear.addCommand("disconnect right hand", "i01.rightHand", "detach")
ear.addCommand("attach left hand", "i01.leftHand", "attach")
ear.addCommand("disconnect left hand", "i01.leftHand", "detach")
ear.addCommand("attach everything", "i01", "attach")
ear.addCommand("disconnect everything", "i01", "detach")
ear.addCommand("attach left arm", "i01.leftArm", "attach")
ear.addCommand("disconnect left arm", "i01.leftArm", "detach")
ear.addCommand("attach right arm", "i01.rightArm", "attach")
ear.addCommand("disconnect right arm", "i01.rightArm", "detach")
ear.addCommand("search humans", "python", "trackHumans")
ear.addCommand("quit search", "python", "stopTracking")
ear.addCommand("track", "python", "trackPoint")
ear.addCommand("freeze track", "python", "stopTracking")
 
ear.addCommand("open hand", "python", "handopen")
ear.addCommand("close hand", "python", "handclose")
ear.addCommand("camera on", i01.getName(), "cameraOn")
ear.addCommand("off camera", i01.getName(), "cameraOff")
ear.addCommand("capture gesture", i01.getName(), "captureGesture")
# FIXME - lk tracking setpoint
#ear.addCommand("track", i01.getName(), "track")
#ear.addCommand("freeze track", i01.getName(), "clearTrackingPoints")
#ear.addCommand("hello", i01.getName(), "hello")
ear.addCommand("hello", "python", "hello")
ear.addCommand("giving", i01.getName(), "giving")
ear.addCommand("fighter", i01.getName(), "fighter")
ear.addCommand("fist hips", "python", "fistHips")
ear.addCommand("look at this", i01.getName(), "lookAtThis")
ear.addCommand("victory", i01.getName(), "victory")
ear.addCommand("arms up", i01.getName(), "armsUp")
ear.addCommand("arms front", i01.getName(), "armsFront")
ear.addCommand("da vinci", i01.getName(), "daVinci")
# FIXME -  
ear.addCommand("manual", ear.getName(), "lockOutAllGrammarExcept", "voice control")
ear.addCommand("voice control", ear.getName(), "clearLock")
ear.addCommand("stop listening", ear.getName(), "stopListening")
 
##sets the servos back to full speed, anywhere in sequence or gestures
ear.addCommand("full speed", "python", "fullspeed")
##sequence1
ear.addCommand("grab the bottle", "python", "grabthebottle")
ear.addCommand("take the glass", "python", "grabtheglass")
ear.addCommand("poor bottle", "python", "poorbottle")
ear.addCommand("give the glass", "python", "givetheglass")
##sequence2
ear.addCommand("take the ball", "python", "takeball")
ear.addCommand("keep the ball", "python", "keepball")
ear.addCommand("approach the left hand", "python", "approachlefthand")
ear.addCommand("use the left hand", "python", "uselefthand")
ear.addCommand("more", "python", "more")
ear.addCommand("hand down", "python", "handdown")
ear.addCommand("is it a ball", "python", "isitaball")
ear.addCommand("put it down", "python", "putitdown")
ear.addCommand("drop it", "python", "dropit")
ear.addCommand("remove your left arm", "python", "removeleftarm")
ear.addCommand("relax", "python", "relax")
##sequence2 in one command
ear.addCommand("what is it", "python", "studyball")
##extras
ear.addCommand("perfect", "python", "perfect")
ear.addCommand("delicate grab", "python", "delicategrab")
ear.addCommand("release delicate", "python", "releasedelicate")
ear.addCommand("open your right hand", "python", "openrighthand")
ear.addCommand("open your left hand", "python", "openlefthand")
ear.addCommand("close your right hand", "python", "closerighthand")
ear.addCommand("close your left hand", "python", "closelefthand")
ear.addCommand("surrender", "python", "surrender")
ear.addCommand("picture on the right side", "python", "picturerightside")
ear.addCommand("picture on the left side", "python", "pictureleftside")
ear.addCommand("picture on both sides", "python", "picturebothside")
ear.addCommand("look on your right side", "python", "lookrightside")
ear.addCommand("look on your left side", "python", "lookleftside")
ear.addCommand("look in the middle", "python", "lookinmiddle")
ear.addCommand("before happy", "python", "beforehappy")
ear.addCommand("happy birthday", "python", "happy")
#ear.addCommand("photo", "python", "photo")
ear.addCommand("about", "python", "about")
ear.addCommand("power down", "python", "powerdown")
ear.addCommand("power up", "python", "powerup")
ear.addCommand("servo", "python", "servos")
ear.addCommand("how many fingers do you have", "python", "howmanyfingersdoihave")
ear.addCommand("who's there", "python", "welcome")
ear.addCommand("start gesture", "python", "startkinect")
ear.addCommand("off gesture", "python", "offkinect")
ear.addCommand("cycle gesture one", "python", "cyclegesture1")
ear.addCommand("cycle gesture two", "python", "cyclegesture2")
ear.addCommand("show your muscles", "python", "muscle")
ear.addCommand("shake hand", "python", "shakehand")
 
ear.addComfirmations("yes","correct","ya","yeah")
ear.addNegations("no","wrong","nope","nah")
 
ear.startListening()
def startkinect():
     i01.copyGesture(True)

def offkinect():
     i01.copyGesture(False)
     
def trackHumans():
     i01.headTracking.faceDetect()
     i01.eyesTracking.faceDetect()

def trackPoint():
     i01.headTracking.startLKTracking()
     i01.eyesTracking.startLKTracking()

def stopTracking():
     i01.headTracking.stopTracking()
     i01.eyesTracking.stopTracking()

def fistHips():
  i01.setHandSpeed("left", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("right", 1.0, 1.0, 1.0, 1.0)
  i01.setHeadSpeed(1.0, 1.0)
  i01.setTorsoSpeed(1.0, 1.0, 1.0)
  i01.moveHead(138,80,82,78,8)
  i01.moveArm("left",79,42,23,41)
  i01.moveArm("right",71,40,14,39)
  i01.moveHand("left",180,180,180,180,180,47)
  i01.moveHand("right",99,130,152,154,145,180)
  i01.moveTorso(90,90,90)

def rest():
  i01.setHandSpeed("left", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("right", 1.0, 1.0, 1.0, 1.0)
  i01.setHeadSpeed(1.0, 1.0)
  i01.setTorsoSpeed(1.0, 1.0, 1.0)
  i01.moveHead(80,86,82,78,8)
  i01.moveArm("left",5,90,30,10)
  i01.moveArm("right",5,90,30,10)
  i01.moveHand("left",2,2,2,2,2,90)
  i01.moveHand("right",2,2,2,2,2,90)
  i01.moveTorso(90,90,90)
  
def fullspeed():
  i01.setHandSpeed("left", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("right", 1.0, 1.0, 1.0, 1.0)
  i01.setHeadSpeed(1.0, 1.0)
  i01.setTorsoSpeed(1.0, 1.0, 1.0)
 
def delicategrab():
  i01.setHandSpeed("left", 0.70, 0.60, 1.0, 1.0, 1.0, 1.0)
  i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("right", 1.0, 1.0, 1.0, 1.0)
  i01.setHeadSpeed(0.65, 0.75)
  i01.moveHead(21,98)
  i01.moveArm("left",30,72,77,10)
  i01.moveArm("right",0,91,28,17)
  i01.moveHand("left",180,130,4,0,0,180)
  i01.moveHand("right",86,51,133,162,153,180)
 
def perfect():
  i01.setHandSpeed("left", 0.80, 0.80, 1.0, 1.0, 1.0, 1.0)
  i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("left", 0.85, 0.85, 0.85, 0.95)
  i01.setArmSpeed("right", 1.0, 1.0, 1.0, 1.0)
  i01.setHeadSpeed(0.65, 0.75)
  i01.moveHead(88,79)
  i01.moveArm("left",89,75,93,11)
  i01.moveArm("right",0,91,28,17)
  i01.moveHand("left",130,160,83,40,0,34)
  i01.moveHand("right",86,51,133,162,153,180)
 
def releasedelicate():
  i01.setHandSpeed("left", 0.60, 0.60, 1.0, 1.0, 1.0, 1.0)
  i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("left", 0.75, 0.75, 0.75, 0.95)
  i01.setArmSpeed("right", 1.0, 1.0, 1.0, 1.0)
  i01.setHeadSpeed(0.65, 0.75)
  i01.moveHead(20,98)
  i01.moveArm("left",30,72,64,10)
  i01.moveArm("right",0,91,28,17)
  i01.moveHand("left",101,74,66,58,44,180)
  i01.moveHand("right",86,51,133,162,153,180)
 
def grabthebottle():
  i01.setHandSpeed("left", 1.0, 0.80, 0.80, 0.80, 1.0, 0.80)
  i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("right", 1.0, 1.0, 1.0, 1.0)
  i01.setHeadSpeed(0.90, 0.80)
  i01.moveHead(20,88)
  i01.moveArm("left",77,85,45,15)
  i01.moveArm("right",5,90,30,10)
  i01.moveHand("left",180,138,140,164,180,60)
  i01.moveHand("right",0,0,0,0,0,90)
 
def grabtheglass():
  i01.setHandSpeed("left", 0.60, 0.60, 1.0, 1.0, 1.0, 1.0)
  i01.setHandSpeed("right", 1.0, 0.60, 0.60, 1.0, 1.0, 0.70)
  i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("right", 1.0, 1.0, 1.0, 1.0)
  i01.setHeadSpeed(0.65, 0.65)
  i01.moveHead(20,68)
  i01.moveArm("left",77,85,45,15)
  i01.moveArm("right",48,91,72,10)
  i01.moveHand("left",180,138,140,164,180,60)
  i01.moveHand("right",140,112,127,105,143,133)
 
def poorbottle():
  i01.setHandSpeed("left", 0.60, 0.60, 0.60, 0.60, 0.60, 0.60)
  i01.setHandSpeed("right", 0.60, 0.80, 0.60, 0.60, 0.60, 0.60)
  i01.setArmSpeed("left", 0.60, 0.60, 0.60, 0.60)
  i01.setArmSpeed("right", 0.60, 0.60, 0.60, 0.60)
  i01.setHeadSpeed(0.65, 0.65)
  i01.moveHead(0,92)
  i01.moveArm("left",53,40,95,55)
  i01.moveArm("right",80,59,43,10)
  i01.moveHand("left",180,140,150,164,180,0)
  i01.moveHand("right",145,112,127,105,143,133)
 
def givetheglass():
  i01.headTracking.faceDetect()
  i01.eyesTracking.faceDetect()
  sleep(2)
  i01.setHandSpeed("left", 0.60, 0.60, 0.60, 0.60, 0.60, 0.60)
  i01.setHandSpeed("right", 0.60, 0.80, 0.60, 0.60, 0.60, 0.60)
  i01.setArmSpeed("left", 0.60, 0.60, 0.60, 0.60)
  i01.setArmSpeed("right", 0.60, 0.60, 0.60, 0.60)
  i01.setHeadSpeed(0.65, 0.65)
  i01.moveHead(84,79)
  i01.moveArm("left",77,75,45,17)
  i01.moveArm("right",21,80,77,10)
  i01.moveHand("left",109,138,180,164,180,60)
  i01.moveHand("right",102,86,105,105,143,133)
  i01.mouth.speakBlocking("Hello please take the glass")
  sleep(1)
 
def takeball():
  i01.setHandSpeed("right", 0.85, 0.75, 0.75, 0.75, 0.85, 0.75)
  i01.setArmSpeed("right", 0.85, 0.85, 0.85, 0.85)
  i01.setHeadSpeed(0.65, 0.65)
  i01.setTorsoSpeed(0.75, 0.55, 1.0)
  i01.moveHead(30,70,78,80,13)
  i01.moveArm("left",5,84,16,15)
  i01.moveArm("right",6,73,76,16)
  i01.moveHand("left",50,50,40,20,20,90)
  i01.moveHand("right",180,114,120,3,0,11)
  i01.moveTorso(101,100,90)
 
 
def keepball():
  i01.setHandSpeed("left", 0.65, 0.65, 0.65, 0.65, 0.65, 1.0)
  i01.setHandSpeed("right", 0.65, 0.65, 0.65, 0.65, 0.65, 1.0)
  i01.setArmSpeed("right", 0.75, 0.85, 0.95, 0.85)
  i01.setArmSpeed("left", 0.75, 0.85, 0.95, 0.85)
  i01.setHeadSpeed(0.65, 0.65)
  i01.setTorsoSpeed(0.75, 0.55, 1.0)
  i01.moveHead(20,70)
  i01.moveArm("left",5,84,16,15)
  i01.moveArm("right",54,77,55,16)
  i01.moveHand("left",50,50,40,20,20,90)
  i01.moveHand("right",180,114,120,3,0,11)
  i01.moveTorso(90,90,90)
 
def approachlefthand():
  i01.setHandSpeed("right", 0.75, 0.75, 0.75, 0.75, 0.75, 0.65)
  i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("right", 0.25, 0.25, 0.25, 0.25)
  i01.setHeadSpeed(0.65, 0.65)
  i01.setTorsoSpeed(0.75, 0.55, 1.0)
  i01.moveHead(20,84,78,80,13)
  i01.moveArm("left",67,52,59,23)
  i01.moveArm("right",55,55,45,16)
  i01.moveHand("left",130,50,40,180,180,0)
  i01.moveHand("right",180,114,120,3,0,11)
  i01.moveTorso(90,85,90)
 
def uselefthand():
  i01.setHandSpeed("right", 0.75, 0.75, 0.75, 0.75, 0.75, 0.65)
  i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("right", 0.25, 0.25, 0.25, 0.25)
  i01.setHeadSpeed(0.65, 0.65)
  i01.moveHead(20,84,78,80,13)
  i01.moveArm("left",65,52,59,23)
  i01.moveArm("right",82,55,50,16)
  i01.moveHand("left",140,50,40,180,180,0)
  i01.moveHand("right",140,114,120,3,0,11)
 
 
def more():
  i01.setHandSpeed("right", 0.75, 0.75, 0.75, 0.75, 0.75, 0.65)
  i01.setArmSpeed("left", 0.85, 0.85, 0.85, 0.95)
  i01.setArmSpeed("right", 0.75, 0.65, 0.65, 0.65)
  i01.setHeadSpeed(0.65, 0.65)
  i01.moveHead(16,84,78,80,13)
  i01.moveArm("left",63,52,59,23)
  i01.moveArm("right",82,55,50,16)
  i01.moveHand("left",140,148,180,180,180,0)
  i01.moveHand("right",80,114,88,3,0,11)
 
 
 
def handdown():
  i01.setHandSpeed("left", 0.75, 0.75, 0.75, 0.75, 0.75, 0.75)
  i01.setHandSpeed("right", 0.70, 0.70, 0.70, 0.70, 0.70, 1.0)
  i01.moveHead(16,84,78,80,13)
  i01.moveArm("left",66,52,59,23)
  i01.moveArm("right",69,51,50,16)
  i01.moveHand("left",140,148,180,180,180,0)
  i01.moveHand("right",54,95,66,3,0,11)
 
def isitaball():
  i01.setHandSpeed("left", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("right", 0.75, 0.85, 0.95, 0.85)
  i01.setArmSpeed("left", 0.75, 0.85, 0.90, 0.85)
  i01.setHeadSpeed(0.65, 0.75)
  i01.moveHead(90,83,78,80,13)
  i01.moveArm("left",70,59,95,15)
  i01.moveArm("right",12,74,33,15)
  i01.moveHand("left",170,150,180,180,180,164)
  i01.moveHand("right",105,81,78,57,62,105)
 
def putitdown():
  i01.setHandSpeed("left", 0.90, 0.90, 0.90, 0.90, 0.90, 0.90)
  i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("right", 0.75, 0.85, 0.95, 0.85)
  i01.setArmSpeed("left", 0.75, 0.85, 0.95, 0.85)
  i01.setHeadSpeed(0.75, 0.75)
  i01.moveHead(20,99)
  i01.moveArm("left",5,45,87,31)
  i01.moveArm("right",5,82,33,15)
  i01.moveHand("left",147,130,135,34,34,35)
  i01.moveHand("right",20,40,40,30,30,72)
 
def dropit():
  i01.setHandSpeed("left", 0.85, 0.85, 0.85, 0.85, 0.85, 0.85)
  i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("right", 0.75, 0.85, 0.95, 0.85)
  i01.setArmSpeed("left", 0.75, 0.85, 1.0, 0.85)
  i01.setHeadSpeed(0.75, 0.75)
  i01.moveHead(20,99)
  i01.moveArm("left",5,45,87,31)
  i01.moveArm("right",5,82,33,15)
  sleep(3)
  i01.moveHand("left",60,61,67,34,34,35)
  i01.moveHand("right",20,40,40,30,30,72)
 
 
def removeleftarm():
  i01.setHandSpeed("left", 0.85, 0.85, 0.85, 0.85, 0.85, 0.85)
  i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("right", 0.75, 0.85, 0.95, 0.85)
  i01.setArmSpeed("left", 0.95, 0.65, 0.75, 0.75)
  i01.setHeadSpeed(0.75, 0.75)
  i01.moveHead(20,100)
  i01.moveArm("left",71,94,41,31)
  i01.moveArm("right",5,82,28,15)
  i01.moveHand("left",60,43,45,34,34,35)
  i01.moveHand("right",20,40,40,30,30,72)
  

def relax():
  i01.setHandSpeed("left", 0.85, 0.85, 0.85, 0.85, 0.85, 0.85)
  i01.setHandSpeed("right", 0.85, 0.85, 0.85, 0.85, 0.85, 0.85)
  i01.setArmSpeed("right", 0.75, 0.85, 0.65, 0.85)
  i01.setArmSpeed("left", 0.95, 0.65, 0.75, 0.75)
  i01.setHeadSpeed(0.75, 0.75)
  i01.setTorsoSpeed(0.75, 0.55, 1.0)
  i01.moveHead(79,100,80,90,13)
  i01.moveArm("left",5,84,28,15)
  i01.moveArm("right",5,82,28,15)
  i01.moveHand("left",92,33,37,71,66,25)
  i01.moveHand("right",81,66,82,60,105,113)
  i01.moveTorso(90,90,90)
 
def handopen():
  i01.moveHand("left",0,0,0,0,0)
  i01.moveHand("right",0,0,0,0,0)

def handclose():
  i01.moveHand("left",180,180,180,180,180)
  i01.moveHand("right",180,180,180,180,180)
 
def openlefthand():
  i01.moveHand("left",0,0,0,0,0)
 
 
def openrighthand():
  i01.moveHand("right",0,0,0,0,0)

def closelefthand():
  i01.moveHand("left",180,180,180,180,180)
 
 
def closerighthand():
  i01.moveHand("right",180,180,180,180,180)
 
 
def surrender():
  i01.setHandSpeed("left", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("right", 0.75, 0.85, 0.95, 0.85)
  i01.setArmSpeed("left", 0.75, 0.85, 0.95, 0.85)
  i01.setHeadSpeed(0.65, 0.65)
  i01.moveHead(90,90,78,100,38)
  i01.moveArm("left",90,139,15,79)
  i01.moveArm("right",90,145,37,79)
  i01.moveHand("left",50,28,30,10,10,76)
  i01.moveHand("right",10,10,10,10,10,139)
 
def pictureleftside():
  i01.setHandSpeed("left", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  i01.setHandSpeed("right", 0.85, 0.85, 0.85, 0.85, 0.85, 0.85)
  i01.setArmSpeed("right", 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("left", 0.75, 0.85, 0.95, 0.85)
  i01.setHeadSpeed(0.65, 0.65)
  i01.moveHead(109,90)
  i01.moveArm("left",90,105,24,75)
  i01.moveArm("right",5,82,28,15)
  i01.moveHand("left",50,86,97,74,106,119)
  i01.moveHand("right",81,65,82,60,105,113)
 
def picturerightside():
  i01.setHandSpeed("left", 0.85, 0.85, 0.85, 0.85, 0.85, 0.85)
  i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("right", 0.85, 0.85, 0.85, 0.85)
  i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
  i01.setHeadSpeed(0.65, 0.65)
  i01.moveHead(109,90)
  i01.moveArm("left",5,94,28,15)
  i01.moveArm("right",90,115,23,68)
  i01.moveHand("left",42,58,87,55,71,35)
  i01.moveHand("right",10,112,95,91,125,45)
 
def picturebothside():
  i01.setHandSpeed("left", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("right", 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
  i01.setHeadSpeed(0.65, 0.65)
  i01.moveHead(109,90)
  i01.moveJaw(50)
  i01.moveArm("left",90,105,24,75)
  i01.moveArm("right",90,115,23,68)
  i01.moveHand("left",50,86,97,74,106,119)
  i01.moveHand("right",10,112,95,91,125,45)

def lookrightside():
  i01.setHeadSpeed(0.70, 0.70)
  i01.moveHead(85,40)

def lookleftside():
  i01.setHeadSpeed(0.70, 0.70)
  i01.moveHead(85,140)

def lookinmiddle():
  i01.setHeadSpeed(0.70, 0.70)
  i01.moveHead(85,86)

def muscle():
  i01.setHandSpeed("left", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("right", 0.75, 0.85, 0.95, 0.85)
  i01.setArmSpeed("left", 0.75, 0.85, 0.95, 0.85)
  i01.setHeadSpeed(0.65, 0.65)
  i01.setTorsoSpeed(0.75, 0.55, 1.0)
  i01.moveHead(90,129,78,100,8)
  i01.moveArm("left",90,139,48,75)
  i01.moveArm("right",71,40,14,39)
  i01.moveHand("left",180,180,180,180,180,148)
  i01.moveHand("right",99,130,152,154,145,180)
  i01.moveTorso(101,100,90)
  sleep(4)
  i01.mouth.speakBlocking("Looks good, doesn't it")
  sleep(2)
  i01.setHandSpeed("left", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("right", 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
  i01.setHeadSpeed(0.85, 0.85)
  i01.setTorsoSpeed(0.75, 0.55, 1.0)
  i01.moveHead(90,45,78,100,10)
  i01.moveArm("left",44,46,20,39)
  i01.moveArm("right",90,145,58,74)
  i01.moveHand("left",180,180,180,180,180,83)
  i01.moveHand("right",99,130,152,154,145,21)
  i01.moveTorso(74,80,90)
  sleep(3)
  i01.mouth.speakBlocking("not bad either, don't you think")
  sleep(4)
  relax()
  sleep(1)

def shakehand():
##rest
  i01.setHandSpeed("left", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("right", 1.0, 1.0, 1.0, 1.0)
  i01.setHeadSpeed(1.0, 1.0)
  i01.setTorsoSpeed(1.0, 1.0, 1.0)
  i01.moveHead(80,86,82,78,8)
  i01.moveArm("left",5,90,30,10)
  i01.moveArm("right",5,90,30,10)
  i01.moveHand("left",2,2,2,2,2,90)
  i01.moveHand("right",2,2,2,2,2,90)
  i01.moveTorso(90,90,90)
  sleep(1)
##move arm and hand
  i01.setHandSpeed("left", 0.65, 0.65, 0.65, 0.65, 0.65, 1.0)
  i01.setHandSpeed("right", 0.65, 0.65, 0.65, 0.65, 0.65, 1.0)
  i01.setArmSpeed("right", 0.75, 0.85, 0.95, 0.85)
  i01.setArmSpeed("left", 0.75, 0.85, 0.95, 0.85)
  i01.setHeadSpeed(0.65, 0.65)
  i01.setTorsoSpeed(0.75, 0.55, 1.0)
  i01.moveHead(39,70,78,80,8)
  i01.moveArm("left",5,84,16,15)
  i01.moveArm("right",6,73,65,16)
  i01.moveHand("left",50,50,40,20,20,90)
  i01.moveHand("right",50,50,40,20,20,90)
  i01.moveTorso(101,100,90)
  sleep(1)
##close the hand
  i01.setHandSpeed("left", 0.65, 0.65, 0.65, 0.65, 0.65, 1.0)
  i01.setHandSpeed("right", 0.85, 0.85, 0.85, 0.85, 0.85, 1.0)
  i01.setArmSpeed("right", 0.75, 0.85, 0.95, 0.85)
  i01.setArmSpeed("left", 0.75, 0.85, 0.75, 0.85)
  i01.setHeadSpeed(0.65, 0.65)
  i01.setTorsoSpeed(0.75, 0.55, 1.0)
  i01.moveHead(39,70,78,80,8)
  i01.moveArm("left",5,84,16,15)
  i01.moveArm("right",6,73,62,16)
  i01.moveHand("left",50,50,40,20,20,90)
  i01.moveHand("right",180,126,120,145,168,77)
  i01.moveTorso(101,100,90)
  sleep(3)
##shake hand up
  i01.setHandSpeed("left", 0.65, 0.65, 0.65, 0.65, 0.65, 1.0)
  i01.setHandSpeed("right", 0.85, 0.85, 0.85, 0.85, 0.85, 1.0)
  i01.setArmSpeed("right", 0.75, 0.85, 0.95, 0.85)
  i01.setArmSpeed("left", 0.75, 0.85, 0.95, 0.85)
  i01.setHeadSpeed(0.45, 0.45)
  i01.setTorsoSpeed(0.75, 0.55, 1.0)
  i01.moveHead(85,90,78,80,8)
  i01.moveArm("left",5,84,16,15)
  i01.moveArm("right",6,73,70,16)
  i01.moveHand("left",50,50,40,20,20,90)
  i01.moveHand("right",180,126,120,145,168,77)
  i01.moveTorso(101,100,90)
  sleep(1)
##shake hand down
  i01.mouth.speakBlocking("please to meet you")
  i01.setHandSpeed("left", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("right", 0.75, 0.75, 0.95, 0.85)
  i01.setHeadSpeed(1.0, 1.0)
  i01.setTorsoSpeed(1.0, 1.0, 1.0)
  i01.moveHead(85,90,78,80,8)
  i01.moveArm("left",5,84,16,15)
  i01.moveArm("right",6,73,65,16)
  i01.moveHand("left",50,50,40,20,20,90)
  i01.moveHand("right",180,126,120,145,168,77)
  i01.moveTorso(101,100,90)
  sleep(1)
##shake hand up
  i01.setHandSpeed("left", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("right", 0.75, 0.75, 0.95, 0.85)
  i01.setHeadSpeed(1.0, 1.0)
  i01.setTorsoSpeed(1.0, 1.0, 1.0)
  i01.moveHead(85,90,78,80,8)
  i01.moveArm("left",5,84,16,15)
  i01.moveArm("right",6,73,75,16)
  i01.moveHand("left",50,50,40,20,20,90)
  i01.moveHand("right",180,126,120,145,168,77)
  i01.moveTorso(101,100,90)
  sleep(1)
##shake hand down
  i01.setHandSpeed("left", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("right", 0.75, 0.75, 0.95, 0.85)
  i01.setHeadSpeed(0.45, 0.45)
  i01.setTorsoSpeed(1.0, 1.0, 1.0)
  i01.moveHead(82,88,78,80,8)
  i01.moveArm("left",5,84,16,15)
  i01.moveArm("right",6,73,62,16)
  i01.moveHand("left",50,50,40,20,20,90)
  i01.moveHand("right",180,126,120,145,168,77)
  i01.moveTorso(101,100,90)
  sleep(2)
## release hand  
  i01.setHandSpeed("left", 0.65, 0.65, 0.65, 0.65, 0.65, 1.0)
  i01.setHandSpeed("right", 0.85, 0.85, 0.85, 0.85, 0.85, 1.0)
  i01.setArmSpeed("right", 0.75, 0.75, 0.95, 0.85)
  i01.setArmSpeed("left", 0.75, 0.85, 0.95, 0.85)
  i01.setHeadSpeed(0.45, 0.45)
  i01.setTorsoSpeed(0.75, 0.55, 1.0)
  i01.moveHead(39,70,78,80,8)
  i01.moveArm("left",5,84,16,15)
  i01.moveArm("right",6,73,62,16)
  i01.moveHand("left",50,50,40,20,20,77)
  i01.moveHand("right",20,50,40,20,20,90)
  i01.moveTorso(101,100,90)
  sleep(4)
##relax
  i01.setHandSpeed("left", 0.85, 0.85, 0.85, 0.85, 0.85, 0.85)
  i01.setHandSpeed("right", 0.85, 0.85, 0.85, 0.85, 0.85, 0.85)
  i01.setArmSpeed("right", 0.75, 0.85, 0.65, 0.85)
  i01.setArmSpeed("left", 0.95, 0.65, 0.75, 0.75)
  i01.setHeadSpeed(0.75, 0.75)
  i01.setTorsoSpeed(0.75, 0.55, 1.0)
  i01.moveHead(79,100,80,90,13)
  i01.moveArm("left",5,84,28,15)
  i01.moveArm("right",5,90,30,10)
  i01.moveHand("left",92,33,37,71,66,25)
  i01.moveHand("right",10,50,40,20,20,113)
  i01.moveTorso(90,90,90)  

  
def powerdown():
        i01.powerDown()
        sleep(2)       
        ear.pauseListening()
        relax()
        i01.mouth.speakBlocking()
        sleep(2)
        i01.moveHead(40, 85);
        sleep(4)
        rightSerialPort.digitalWrite(53, Arduino.LOW)
        leftSerialPort.digitalWrite(53, Arduino.LOW)
        ear.lockOutAllGrammarExcept("power up")
        sleep(2)
        ear.resumeListening()
 
def powerup():
        i01.powerUp()
        sleep(2)        
        ear.pauseListening()
        rightSerialPort.digitalWrite(53, Arduino.HIGH)
        leftSerialPort.digitalWrite(53, Arduino.HIGH)
        i01.mouth.speakBlocking("hello")
        relax()
        ear.clearLock()
        sleep(2)
        ear.resumeListening()
 
def hello():
     i01.setHandSpeed("left", 0.60, 0.60, 1.0, 1.0, 1.0, 1.0)
     i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
     i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
     i01.setArmSpeed("right", 1.0, 1.0, 1.0, 1.0)
     i01.setHeadSpeed(0.65, 0.75)
     i01.moveHead(105,78)
     i01.moveArm("left",78,48,37,11)
     i01.moveArm("right",90,144,60,75)
     i01.moveHand("left",112,111,105,102,81,10)
     i01.moveHand("right",0,0,0,50,82,180)
     ear.pauseListening()
     sleep(1)
 
     for w in range(0,3):
          i01.setHandSpeed("left", 0.60, 0.60, 1.0, 1.0, 1.0, 1.0)
          i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 0.60)
          i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
          i01.setArmSpeed("right", 0.60, 1.0, 1.0, 1.0)
          i01.setHeadSpeed(0.65, 0.75)
          i01.moveHead(83,98)
          i01.moveArm("left",78,48,37,11)
          i01.moveArm("right",90,157,47,75)
          i01.moveHand("left",112,111,105,102,81,10)
          i01.moveHand("right",3,0,62,41,117,94)
          sleep(1)
 
          if w==1:
                     i01.setHandSpeed("left", 0.60, 0.60, 1.0, 1.0, 1.0, 1.0)
                     i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 0.60)
                     i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
                     i01.setArmSpeed("right", 0.65, 1.0, 1.0, 1.0)
                     i01.setHeadSpeed(0.65, 0.75)
                     i01.moveHead(83,70)
                     i01.mouth.speakBlocking("hello, my name is inmov")
                     i01.moveArm("left",78,48,37,11)
                     i01.moveArm("right",57,145,50,68)
                     i01.moveHand("left",100,90,85,80,71,15)
                     i01.moveHand("right",3,0,31,12,26,45)
                     sleep(1)
                     i01.moveHead(83,98)
                     i01.moveArm("left",78,48,37,11)
                     i01.moveArm("right",90,157,47,75)
                     i01.moveHand("left",112,111,105,102,81,10)
                     i01.moveHand("right",3,0,62,41,117,94)
                     sleep(1)
                     i01.setHandSpeed("left", 0.85, 0.85, 0.85, 0.85, 0.85, 0.85)
                     i01.setHandSpeed("right", 0.85, 0.85, 0.85, 0.85, 0.85, 0.85)
                     i01.setArmSpeed("right", 0.75, 0.85, 0.95, 0.85)
                     i01.setArmSpeed("left", 0.95, 0.65, 0.75, 0.75)
                     i01.setHeadSpeed(0.75, 0.75)
                     i01.moveHead(79,100)
                     i01.moveArm("left",5,94,28,15)
                     i01.moveArm("right",5,82,28,15)
                     i01.moveHand("left",42,58,42,55,71,35)
                     i01.moveHand("right",81,50,82,60,105,113)
                     ear.resumeListening()
 
def photo():    
        i01.moveHead(87,60)
        i01.moveArm("left",78,48,37,11)
        i01.moveArm("right",46,147,5,75)
        i01.moveHand("left",138,52,159,106,120,90)
        i01.moveHand("right",80,65,94,63,70,140)
 
def beforehappy():
        i01.setHandSpeed("left", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
        i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
        i01.setArmSpeed("right", 0.85, 0.85, 0.85, 1.0)
        i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
        i01.setHeadSpeed(0.65, 0.65)
        i01.moveHead(84,88)
        i01.moveArm("left",5,82,36,11)
        i01.moveArm("right",74,112,61,29)
        i01.moveHand("left",0,88,135,94,96,90)
        i01.moveHand("right",81,79,118,47,0,90)
 
def happy():
     for w in range(0,3):
         sleep(1)
         i01.setHandSpeed("left", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
         i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
         i01.setArmSpeed("right", 0.85, 0.85, 0.85, 1.0)
         i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
         i01.setHeadSpeed(0.65, 0.65)
         i01.moveHead(84,88)
         i01.moveArm("left",5,82,36,10)
         i01.moveArm("right",74,112,61,29)
         i01.moveHand("left",0,88,135,94,96,90)
         i01.moveHand("right",81,79,118,47,0,90)
         sleep(1)
         if w==1:
                     i01.mouth.speakBlocking("happy birthday grog")
                     i01.setHandSpeed("left", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
                     i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
                     i01.setArmSpeed("right", 0.85, 0.85, 0.85, 1.0)
                     i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
                     i01.setHeadSpeed(0.65, 0.65)
                     i01.moveHead(42,76)
                     i01.moveArm("left",5,90,30,10)
                     i01.moveArm("right",74,70,61,10)
                     i01.moveHand("left",0,0,0,0,0,90)
                     i01.moveHand("right",81,79,118,47,0,90)
                     sleep(5)
                     ear.resumeListening()
 
def about():
        sleep(2)        
        ear.pauseListening()
        sleep(2)
        i01.setArmSpeed("right", 0.1, 0.1, 0.2, 0.2);
        i01.setArmSpeed("left", 0.1, 0.1, 0.2, 0.2);
        i01.setHeadSpeed(0.2,0.2)
        i01.moveArm("right", 64, 94, 10, 10);
 
 
        i01.mouth.speakBlocking("I am the first life size humanoid robot you can 3D print and animate")
        i01.moveHead(65,66)
        i01.moveArm("left", 64, 104, 10, 11);
        i01.moveArm("right", 44, 84, 10, 11);
        i01.mouth.speakBlocking("my designer creator is Gael Langevin a French sculptor, model maker")
        i01.moveHead(75,86)
        i01.moveArm("left", 54, 104, 10, 11);
        i01.moveArm("right", 64, 84, 10, 20);
        i01.mouth.speakBlocking("who has released my files  to the opensource 3D world.")
        i01.moveHead(65,96)
        i01.moveArm("left", 44, 94, 10, 20);
        i01.moveArm("right", 54, 94, 20, 11);
        i01.mouth.speakBlocking("this is where my builder downloaded my files.")
 
        i01.moveHead(75,76)
        i01.moveArm("left", 64, 94, 20, 11);
        i01.moveArm("right", 34, 94, 10, 11);
        i01.mouth.speakBlocking("after five hundred hours of printing, four kilos of plastic, twenty five hobby servos, blood and sweat.I was brought to life") # should be " i was borght to life."
        i01.moveHead(65,86)
        i01.moveArm("left", 24, 94, 10, 11);
        i01.moveArm("right", 24, 94, 10, 11);  
        i01.mouth.speakBlocking("so if You have a 3D printer, some building skills, then you can build your own version of me") # mabe add in " alot of money"
        i01.moveHead(85,86)
        i01.moveArm("left", 5, 94, 20, 30);
        i01.moveArm("right", 24, 124, 10, 20);
        i01.mouth.speakBlocking("and if enough people build me, some day my kind could take over the world") # mabe add in " alot of money"
        i01.moveHead(75,96)
        i01.moveArm("left", 24, 104, 10, 11);
        i01.moveArm("right", 5, 94, 20, 30);
        i01.mouth.speakBlocking("I'm just kidding. i need some legs to get around, and i have to over come my  pyro-phobia, a fear of fire") # mabe add in " alot of money"
        i01.moveHead(75,96)
        i01.moveArm("left", 5, 94, 10, 11)
        i01.moveArm("right", 4, 94, 10, 11);
        i01.mouth.speakBlocking("so, until then. i will be humankind's humble servant")
 
        i01.rest()
        i01.setArmSpeed("right", 1, 1, 1, 1);
        i01.setArmSpeed("left", 1, 1, 1, 1);
        i01.setHeadSpeed(1,1)
        sleep(2)
        ear.resumeListening()
 
def servos():  
        ear.pauseListening()
        sleep(2)
        i01.setHandSpeed("left", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
        i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
        i01.setArmSpeed("right", 0.85, 0.85, 0.85, 0.85)
        i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
        i01.setHeadSpeed(0.65, 0.65)
        i01.moveHead(79,100)
        i01.moveArm("left",5,119,28,15)
        i01.moveArm("right",5,111,28,15)
        i01.moveHand("left",42,58,87,55,71,35)
        i01.moveHand("right",81,20,82,60,105,113)
        i01.mouth.speakBlocking("I currently have twenty five  hobby servos installed in my body to give me life")
        i01.setHandSpeed("left", 0.85, 0.85, 0.85, 0.85, 0.85, 0.85)
        i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
        i01.setArmSpeed("right", 0.85, 0.85, 0.85, 0.85)
        i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
        i01.setHeadSpeed(0.65, 0.65)
        i01.moveHead(124,90)
        i01.moveArm("left",89,94,91,35)
        i01.moveArm("right",20,67,31,22)
        i01.moveHand("left",106,41,161,147,138,90)
        i01.moveHand("right",0,0,0,54,91,90)
        i01.mouth.speakBlocking("there's one servo  for moving my mouth up and down")
        sleep(1)
        i01.setHandSpeed("left", 0.85, 0.85, 1.0, 0.85, 0.85, 0.85)
        i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
        i01.setArmSpeed("right", 0.85, 0.85, 0.85, 0.85)
        i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
        i01.setHeadSpeed(0.65, 0.65)
        i01.moveHead(105,76);
        i01.moveArm("left",89,106,103,35);
        i01.moveArm("right",35,67,31,22);
        i01.moveHand("left",106,0,0,147,138,7);
        i01.moveHand("right",0,0,0,54,91,90);
        i01.mouth.speakBlocking("two for my eyes")
        sleep(0.2)
        i01.setHandSpeed("left", 0.85, 0.85, 1.0, 1.0, 1.0, 0.85)
        i01.moveHand("left",106,0,0,0,0,7);
        i01.mouth.speakBlocking("and two more for my head")
        sleep(0.5)
        i01.setHandSpeed("left", 0.85, 0.9, 0.9, 0.9, 0.9, 0.85)
        i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
        i01.setArmSpeed("right", 0.85, 0.85, 0.85, 0.85)
        i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
        i01.setHeadSpeed(0.65, 0.65)
        i01.moveHead(90,40);
        i01.moveArm("left",89,106,103,35);
        i01.moveArm("right",35,67,31,20);
        i01.moveHand("left",106,140,140,140,140,7);
        i01.moveHand("right",0,0,0,54,91,90);
        i01.mouth.speakBlocking("so i can look around")
        sleep(0.5)
        i01.setHeadSpeed(0.65, 0.65)
        i01.moveHead(105,125);
        i01.setArmSpeed("left", 0.9, 0.9, 0.9, 0.9)
        i01.moveArm("left",60,100,85,30);
        i01.mouth.speakBlocking("and see who's there")
        i01.setHeadSpeed(0.65, 0.65)
        i01.moveHead(40,56);
        sleep(0.5)
        i01.setArmSpeed("right", 1.0, 1.0, 1.0, 1.0)
        i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
        i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0);
        i01.setArmSpeed("right", 0.5, 0.6, 0.5, 0.6);
        i01.moveArm("left",87,41,64,11)
        i01.moveArm("right",5,95,40,11)
        i01.moveHand("left",98,150,160,160,160,104)
        i01.moveHand("right",0,0,50,54,91,90);
        i01.mouth.speakBlocking("there's three servos  in each shoulder")
        i01.moveHead(40,67);
        sleep(2)
        i01.setHandSpeed("left", 0.8, 0.9, 0.8, 0.8, 0.8, 0.8)
        i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
        i01.setArmSpeed("right", 0.85, 0.85, 0.85, 0.85)
        i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
        i01.setHeadSpeed(0.8, 0.8)
        i01.moveHead(43,69)
        i01.moveArm("left",87,41,64,11)
        i01.moveArm("right",5,95,40,42)
        i01.moveHand("left",42,0,100,80,113,35)
        i01.moveHand("left",42,10,160,160,160,35)
        i01.moveHand("right",81,20,82,60,105,113)
        i01.mouth.speakBlocking("here is the first servo movement")
        sleep(1)
        i01.moveHead(37,60);
        i01.setHandSpeed("left", 1.0, 1.0, 0.9, 0.9, 1.0, 0.8)
        i01.setArmSpeed("right", 1.0, 1.0, 1.0, 1.0)
        i01.moveArm("right",5,95,67,42)
        i01.moveHand("left",42,10,10,160,160,30)
        i01.mouth.speakBlocking("this is the second one")
        sleep(1)
        i01.moveHead(43,69);
        i01.setArmSpeed("right", 1.0, 1.0, 1.0, 1.0)
        i01.moveArm("right",5,134,67,42)
        i01.moveHand("left",42,10,10,10,160,35)
        i01.mouth.speakBlocking("now you see the third")
        sleep(1)
        i01.setArmSpeed("right", 0.8, 0.8, 0.8, 0.8)
        i01.moveArm("right",20,90,45,16)
        i01.mouth.speakBlocking("they give me a more human like movement")
        sleep(1)
        i01.setHandSpeed("left", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
        i01.setArmSpeed("right", 1.0, 1.0, 1.0, 1.0);
        i01.moveHead(43,72)
        i01.moveArm("left",90,44,66,11)
        i01.moveArm("right",90,100,67,26)
        i01.moveHand("left",42,80,100,80,113,35)
        i01.moveHand("right",81,0,82,60,105,69)
        i01.mouth.speakBlocking("but, i have only  one servo, to move each elbow")
        i01.setHandSpeed("left", 0.85, 0.85, 0.85, 0.85, 0.85, 0.85)
        i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
        i01.setArmSpeed("right", 0.85, 0.85, 0.85, 0.85)
        i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
        i01.setHeadSpeed(0.8, 0.8)
        i01.moveHead(45,62)
        i01.moveArm("left",72,44,90,11)
        i01.moveArm("right",90,95,68,15)
        i01.moveHand("left",42,0,100,80,113,35)
        i01.moveHand("right",81,0,82,60,105,0)
        i01.mouth.speakBlocking("that, leaves me, with one servo per wrist")
        i01.moveHead(40,60)
        i01.setHandSpeed("left", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
        i01.setHandSpeed("right", 0.9, 0.9, 0.9, 0.9, 0.9, 0.9)
        i01.moveArm("left",72,44,90,9)
        i01.moveArm("right",90,95,68,15)
        i01.moveHand("left",42,0,100,80,113,35)
        i01.moveHand("right", 10, 140,82,60,105,10)
        i01.mouth.speakBlocking("and one servo for each finger.")
        sleep(0.5)
        i01.moveHand("left",42,0,100,80,113,35)
        i01.moveHand("right", 50, 51, 15,23, 30,140);
        i01.mouth.speakBlocking("these servos are located in my forearms")
        i01.setHandSpeed("left", 0.8, 0.8, 0.8, 0.8,0.8, 0.8)
        i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
        i01.moveHand("left", 36, 52, 8,22, 20);
        i01.moveHand("right", 120, 147, 130,110, 125);
        further()
        sleep(1)
        i01.mouth.speakBlocking("they are hooked up, by the use of tendons")
        i01.moveHand("left",10,20,30,40,60,150);
        i01.moveHand("right",110,137,120,100,105,130);
        i01.setHeadSpeed(1,1)
        i01.setArmSpeed("right", 1.0,1.0, 1.0, 1.0);
        i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0);
        relax()
        sleep(2)
        ear.resumeListening()
 
def howmanyfingersdoihave():
     ear.pauseListening()
     sleep(1)
     i01.moveHead(49,74)
     i01.moveArm("left",75,83,79,24)
     i01.moveArm("right",65,82,71,24)
     i01.moveHand("left",74,140,150,157,168,92)
     i01.moveHand("right",89,80,98,120,114,0)
     sleep(2)
     i01.moveHand("right",0,80,98,120,114,0)
     i01.mouth.speakBlocking("ten")
 
     sleep(.1)
     i01.moveHand("right",0,0,98,120,114,0)
     i01.mouth.speakBlocking("nine")
 
     sleep(.1)
     i01.moveHand("right",0,0,0,120,114,0)
     i01.mouth.speakBlocking("eight")
 
     sleep(.1)
     i01.moveHand("right",0,0,0,0,114,0)
     i01.mouth.speakBlocking("seven")
 
     sleep(.1)
     i01.moveHand("right",0,0,0,0,0,0)
     i01.mouth.speakBlocking("six")
 
     sleep(.5)
     i01.setHeadSpeed(.70,.70)
     i01.moveHead(40,105)
     i01.moveArm("left",75,83,79,24)
     i01.moveArm("right",65,82,71,24)
     i01.moveHand("left",0,0,0,0,0,180)
     i01.moveHand("right",0,0,0,0,0,0)
     sleep(0.1)
     i01.mouth.speakBlocking("and five makes eleven")
 
     sleep(0.7)
     i01.setHeadSpeed(0.7,0.7)
     i01.moveHead(40,50)
     sleep(0.5)
     i01.setHeadSpeed(0.7,0.7)
     i01.moveHead(49,105)
     sleep(0.7)
     i01.setHeadSpeed(0.7,0.8)
     i01.moveHead(40,50)
     sleep(0.7)
     i01.setHeadSpeed(0.7,0.8)
     i01.moveHead(49,105)
     sleep(0.7)
     i01.setHeadSpeed(0.7,0.7)
     i01.moveHead(90,85)
     sleep(0.7)
     i01.mouth.speakBlocking("eleven")
     i01.moveArm("left",70,75,70,20)
     i01.moveArm("right",60,75,65,20)
     sleep(1)
     i01.mouth.speakBlocking("that doesn't seem right")
     sleep(2)
     i01.mouth.speakBlocking("I think I better try that again")
 
     i01.moveHead(40,105)
     i01.moveArm("left",75,83,79,24)
     i01.moveArm("right",65,82,71,24)
     i01.moveHand("left",140,168,168,168,158,90)
     i01.moveHand("right",87,138,109,168,158,25)
     sleep(2)
 
     i01.moveHand("left",10,140,168,168,158,90)
     i01.mouth.speakBlocking("one")
     sleep(.1)
 
 
     i01.moveHand("left",10,10,168,168,158,90)
     i01.mouth.speakBlocking("two")
     sleep(.1)
 
     i01.moveHand("left",10,10,10,168,158,90)
     i01.mouth.speakBlocking("three")
     sleep(.1)
     i01.moveHand("left",10,10,10,10,158,90)
 
     i01.mouth.speakBlocking("four")
     sleep(.1)
     i01.moveHand("left",10,10,10,10,10,90)
 
     i01.mouth.speakBlocking("five")
     sleep(.1)
     i01.setHeadSpeed(0.65,0.65)
     i01.moveHead(53,65)
     i01.moveArm("right",48,80,78,11)
     i01.setHandSpeed("left", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
     i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
     i01.moveHand("left",10,10,10,10,10,90)
     i01.moveHand("right",10,10,10,10,10,25)
     sleep(1)
     i01.mouth.speakBlocking("and five makes ten")
     sleep(.5)
     i01.mouth.speakBlocking("there that's better")
     i01.moveHead(95,85)
     i01.moveArm("left",75,83,79,24)
     i01.moveArm("right",40,70,70,10)
     sleep(0.5)
     i01.mouth.speakBlocking("inmoov i01 has ten fingers")
     sleep(0.5)
     i01.moveHead(90,90)
     i01.setHandSpeed("left", 0.8, 0.8, 0.8, 0.8, 0.8, 0.8)
     i01.setHandSpeed("right", 0.8, 0.8, 0.8, 0.8, 0.8, 0.8)
     i01.moveHand("left",140,140,140,140,140,60)
     i01.moveHand("right",140,140,140,140,140,60)
     sleep(1.0)
     i01.setArmSpeed("right", 1.0, 1.0, 1.0, 1.0)
     i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
     i01.moveArm("left",5,90,30,11)
     i01.moveArm("right",5,90,30,11)
     sleep(0.5)
     relax()
     sleep(0.5)
 
     ear.resumeListening()


def studyball():
##keepball():
  i01.setHandSpeed("left", 0.65, 0.65, 0.65, 0.65, 0.65, 1.0)
  i01.setHandSpeed("right", 0.65, 0.65, 0.65, 0.65, 0.65, 1.0)
  i01.setArmSpeed("right", 0.75, 0.85, 0.95, 0.85)
  i01.setArmSpeed("left", 0.75, 0.85, 0.95, 0.85)
  i01.setHeadSpeed(0.65, 0.65)
  i01.setTorsoSpeed(1.0, 0.65, 1.0)
  i01.moveHead(20,70)
  i01.moveArm("left",5,84,16,15)
  i01.moveArm("right",54,77,55,16)
  i01.moveHand("left",50,50,40,20,20,90)
  i01.moveHand("right",180,114,120,3,0,11)
  i01.moveTorso(90,90,90)
  sleep(3)
##approachlefthand():
  i01.setHandSpeed("right", 0.75, 0.75, 0.75, 0.75, 0.75, 0.65)
  i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("right", 0.25, 0.25, 0.25, 0.25)
  i01.setHeadSpeed(0.65, 0.65)
  i01.moveHead(20,84,78,80,13)
  i01.moveArm("left",67,52,59,23)
  i01.moveArm("right",55,55,45,16)
  i01.moveHand("left",130,50,40,180,180,0)
  i01.moveHand("right",180,114,120,3,0,11)
  sleep(4)
##uselefthand():
  i01.setHandSpeed("right", 0.75, 0.75, 0.75, 0.75, 0.75, 0.65)
  i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("right", 0.25, 0.25, 0.25, 0.25)
  i01.setHeadSpeed(0.65, 0.65)
  i01.moveHead(20,84,78,80,13)
  i01.moveArm("left",65,52,59,23)
  i01.moveArm("right",82,55,50,16)
  i01.moveHand("left",140,50,40,180,180,0)
  i01.moveHand("right",140,114,120,3,0,11)
  sleep(4)
##more():
  i01.setHandSpeed("right", 0.75, 0.75, 0.75, 0.75, 0.75, 0.65)
  i01.setArmSpeed("left", 0.85, 0.85, 0.85, 0.95)
  i01.setArmSpeed("right", 0.75, 0.65, 0.65, 0.65)
  i01.setHeadSpeed(0.65, 0.65)
  i01.moveHead(16,84,78,80,13)
  i01.moveArm("left",63,52,59,23)
  i01.moveArm("right",82,55,50,16)
  i01.moveHand("left",140,148,180,180,180,0)
  i01.moveHand("right",80,114,88,3,0,11)
  sleep(3)
##handdown():
  i01.setHandSpeed("left", 0.75, 0.75, 0.75, 0.75, 0.75, 0.75)
  i01.setHandSpeed("right", 0.70, 0.70, 0.70, 0.70, 0.70, 1.0)
  i01.moveHead(16,84,78,80,13)
  i01.moveArm("left",66,52,59,23)
  i01.moveArm("right",69,54,50,16)
  i01.moveHand("left",140,148,180,180,180,0)
  i01.moveHand("right",54,95,66,3,0,11)
  sleep(2)
#isitaball():
  i01.setHandSpeed("left", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("right", 0.75, 0.85, 0.95, 0.85)
  i01.setArmSpeed("left", 0.75, 0.85, 0.90, 0.85)
  i01.setHeadSpeed(0.65, 0.75)
  i01.moveHead(90,83,78,80,13)
  i01.moveArm("left",70,59,95,15)
  i01.moveArm("right",12,74,33,15)
  i01.moveHand("left",170,150,180,180,180,164)
  i01.moveHand("right",105,81,78,57,62,105)
  i01.mouth.speakBlocking("I will start tracking the object")
  sleep(2)
  i01.mouth.speakBlocking("you need to set the point")
  i01.headTracking.startLKTracking()
  i01.eyesTracking.startLKTracking()
  sleep()

def welcome():
  sleep(1)
  i01.setHandSpeed("left", 0.60, 0.60, 0.60, 0.60, 0.60, 0.60)
  i01.setHandSpeed("right", 0.60, 0.80, 0.60, 0.60, 0.60, 0.60)
  i01.setArmSpeed("left", 0.60, 0.60, 0.60, 0.60)
  i01.setArmSpeed("right", 0.60, 0.60, 0.60, 0.60)
  i01.setHeadSpeed(0.65, 0.65)
  i01.moveHead(80,90,78,80,13)
  i01.moveArm("left",26,105,30,25)
  i01.moveArm("right",37,124,30,27)
  i01.moveHand("left",2,2,2,2,2,90)
  i01.moveHand("right",2,2,2,2,2,90)
  sleep(1)
  i01.mouth.speakBlocking("Welcome to the hue music")
  sleep(1)




def cyclegesture2():
    relax()
    sleep(2)
    removeleftarm()
    i01.setHandSpeed("left", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
    i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
    i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
    i01.setArmSpeed("right", 1.0, 1.0, 1.0, 1.0)
    i01.setHeadSpeed(1.0, 1.0)
    i01.setTorsoSpeed(1.0, 1.0, 1.0)
    sleep(8)
    handdown()
    i01.setHandSpeed("left", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
    i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
    i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
    i01.setArmSpeed("right", 1.0, 1.0, 1.0, 1.0)
    i01.setHeadSpeed(1.0, 1.0)
    i01.setTorsoSpeed(1.0, 1.0, 1.0)
    sleep(8)
    giving()
    sleep(5)
    handclose()
    i01.setHandSpeed("left", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
    i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
    i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
    i01.setArmSpeed("right", 1.0, 1.0, 1.0, 1.0)
    i01.setHeadSpeed(1.0, 1.0)
    i01.setTorsoSpeed(1.0, 1.0, 1.0)
    sleep(2)
    giving()
    i01.setHandSpeed("left", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
    i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
    i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
    i01.setArmSpeed("right", 1.0, 1.0, 1.0, 1.0)
    i01.setHeadSpeed(1.0, 1.0)
    i01.setTorsoSpeed(1.0, 1.0, 1.0)
    sleep(5)
    removeleftarm()
    sleep(2)
    isitaball()
    i01.setHandSpeed("left", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
    i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
    i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
    i01.setArmSpeed("right", 1.0, 1.0, 1.0, 1.0)
    i01.setHeadSpeed(1.0, 1.0)
    i01.setTorsoSpeed(1.0, 1.0, 1.0)
    sleep(4)
    relax()
    sleep(5)
    powerdown()