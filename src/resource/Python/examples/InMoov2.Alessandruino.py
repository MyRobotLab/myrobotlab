headPort = "COM8"

i01 = Runtime.createAndStart("i01", "InMoov")

mouthControl=i01.startMouthControl("COM8")

head = i01.startHead(headPort)
leftArm = i01.startLeftArm(headPort)

headTracking = i01.startHeadTracking("COM8")
eyesTracking= i01.startEyesTracking("COM8")

i01.headTracking.startLKTracking()
i01.eyesTracking.startLKTracking()

i01.startEar()


############################################################
#!!!my eyeY servo and jaw servo are reverted, Gael should delete this part !!!!
i01.leftArm.bicep.setMinMax(10,85)
i01.head.eyeY.map(0,180,180,0)
i01.head.eyeY.setMinMax(22,85)
i01.head.eyeX.setMinMax(60,85)
i01.head.eyeY.setRest(45)
i01.head.eyeY.moveTo(45)
i01.head.jaw.map(0,180,180,0)
i01.head.jaw.setMinMax(110,180)
i01.head.jaw.moveTo(70)
i01.mouthControl.setmouth(70,45)
############################################################

i01.startMouth()

i01.mouth.setGoogleURI("http://thehackettfamily.org/Voice_api/api2.php?voice=Graham&txt=")

i01.headTracking.xpid.setPID(10.0,5.0,0.1)
i01.headTracking.ypid.setPID(10.0,5.0,0.1)
i01.eyesTracking.xpid.setPID(15.0,5.0,0.1)
i01.eyesTracking.ypid.setPID(15.0,5.0,0.1)

ear = i01.ear

ear.addCommand("attach", "i01", "attach")
ear.addCommand("detach", "i01", "detach")
ear.addCommand("track humans", "python", "trackHumans")
ear.addCommand("track point", "python", "trackPoint")
ear.addCommand("stop tracking", "python", "stopTracking")
ear.addCommand("rest position", "i01.head", "rest")
ear.addCommand("raise arm", "python", "rais")
ear.addCommand("arm down", "python", "down")
 
ear.addCommand("manual", "i01.ear", "lockOutAllGrammarExcept", "voice control")
ear.addCommand("voice control", "i01.ear", "clearLock")
 
ear.addComfirmations("yes", "correct", "yeah", "ya")
ear.addNegations("no", "wrong", "nope", "nah")
 
# all commands MUST be before startListening
ear.startListening()

def trackHumans():
     i01.headTracking.faceDetect()
     i01.eyesTracking.faceDetect()

def trackPoint():
     i01.headTracking.startLKTracking()
     i01.eyesTracking.startLKTracking()

def stopTracking():
     i01.headTracking.stopTracking()
     i01.eyesTracking.stopTracking()

def rais ():
     i01.leftArm.bicep.moveTo(80)

def down ():
     i01.leftArm.bicep.moveTo(10)
