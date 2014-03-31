headPort = "COM8"
headPort = "COM15"

i01 = Runtime.createAndStart("i01", "InMoov")
mouth = i01.startMouth()
ear = i01.startEar()
mouthControl = i01.startMouthControl(headPort)
head = i01.startHead(headPort)
neck = i01.startHeadTracking(headPort)
eyes = i01.startEyesTracking(headPort)

neck.startLKTracking()
#eyes.startLKTracking()

############################################################
#!!!my eyeY servo and jaw servo are reverted, Gael should delete this part !!!!
# i01.head.eyeY.setInverted(True)
# i01.head.eyeY.setMinMax(22,85)
# i01.head.eyeY.setRest(45)
# i01.head.eyeY.moveTo(45)
# i01.head.jaw.setInverted(True)
# i01.mouthControl.setmouth(50,0)
i01.opencv.setCameraIndex(1)
############################################################

i01.headTracking.xpid.setPID(15.0,5.0,0.1)
i01.headTracking.ypid.setPID(20.0,5.0,0.1)
i01.eyesTracking.xpid.setPID(15.0,5.0,1.0)
i01.eyesTracking.ypid.setPID(15.0,5.0,1.0)

ear.addCommand("attach", "i01.head", "attach")
ear.addCommand("detach", "i01.head", "detach")
ear.addCommand("track humans", "i01.head.headTracking", "faceDetect")
ear.addCommand("stop tracking", "i01.head.headTracking", "stopTracking")
ear.addCommand("rest", "i01.head", "rest")
 
ear.addCommand("manual", ear.getName(), "lockOutAllGrammarExcept", "voice control")
ear.addCommand("voice control", ear.getName(), "clearLock")
 
ear.addComfirmations("yes", "correct", "yeah", "ya")
ear.addNegations("no", "wrong", "nope", "nah")
 
# all commands MUST be before startListening
ear.startListening()
