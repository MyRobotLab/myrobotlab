#file : InMoov2.HelloRobyn.1.py

# this script is provided as a basic guide
# most parts can be run by uncommenting them
# InMoov now can be started in modular pieces
 
import random

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
i01.head.jaw.setMinMax(78,103)
i01.mouthControl.setmouth(78,100)
helvar = 1
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

ear.addCommand("look forward", "python", "lookforward")

ear.addCommand("arms down", "python", "armsdown")

ear.addCommand("look right", "python", "lookright")
 
ear.addComfirmations("yes","correct","ya","yeah")
ear.addNegations("no","wrong","nope","nah")

ear.startListening("sorry | how do you do | goodbye | i love you")

# set up a message route from the ear --to--> python method "heard"
ear.addListener("recognized", "python", "heard")

def heard():
    data = msg_i01_ear_recognized.data[0]
 
 
 
    if (data == "sorry"):
        x = (random.randint(1, 3))
        if x == 1:
            i01.mouth.speak("no problems")
        if x == 2:
            i01.mouth.speak("it doesn't matter")
        if x == 3:
            i01.mouth.speak("it's okay")
 
    if (data == "goodbye"):
        i01.mouth.speak("goodbye")
        global helvar
        helvar = 1
        x = (random.randint(1, 4))
        if x == 1:
            i01.mouth.speak("i'm looking forward to see you again")
        if x == 2:
            i01.mouth.speak("see you soon")
 
 
    if (data == "how do you do"):
        if helvar <= 2:    
            i01.mouth.speak("hello")
            global helvar
            helvar += 1
        elif helvar == 3:
            i01.mouth.speak("how do you do you have already said that at least twice")
#            i01.moveArm("left",20,40,140,45)
#            i01.moveArm("right",20,150,164,45)
            i01.moveHand("left",0,0,0,0,0,119)
            i01.moveHand("right",0,0,0,0,0,119)
            sleep(2)
            armsdown()
            global helvar
            helvar += 1
        elif helvar == 4:
            i01.mouth.speak("what is your problem stop saying how do you do all the time")
#            i01.moveArm("left",20,88,140,73)
#            i01.moveArm("right",20,85,164,80)
            i01.moveHand("left",130,180,180,180,180,119)
            i01.moveHand("right",130,180,180,180,180,119)
            sleep(2)
            armsdown()
            global helvar
            helvar += 1
        elif helvar == 5:
            i01.mouth.speak("i will ignore you if you say how do you do one more time")
            lookright()
            global helvar
            helvar += 1
 
    if (data == "i love you"):
        i01.mouth.speak("i love you too")

def armsdown():
  i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
  i01.setArmSpeed("right", 1.0, 1.0, 1.0, 1.0)
#  i01.moveArm("left",90,88,140,73)
#  i01.moveArm("right",77,85,164,80)
 
 
def lookforward():
  i01.setHeadSpeed(0.65, 0.75)
  i01.moveHead(74,90)
 
def lookright():
  i01.setHeadSpeed(0.65, 0.75)
  i01.moveHead(60,150)
