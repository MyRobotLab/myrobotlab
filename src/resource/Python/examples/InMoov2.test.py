headPort = "COM7"
rightHandPort = "COM8"
rightArmPort = "COM8"
leftArmPort = "COM7"

i01 = Runtime.createAndStart("i01", "InMoov")
rightArm = i01.startRightArm(rightArmPort)
rightHand = i01.startRightHand(rightHandPort)
leftArm = i01.startLeftArm(leftArmPort)

head = i01.startHead(headPort)
neck = i01.getHeadTracking()
neck.faceDetect()

############################################################
#!!!my eyeY servo and jaw servo are reverted, Gael should delete this part !!!!
i01.head.eyeY.setInverted(True)
i01.head.eyeY.setMinMax(22,85)
i01.head.eyeY.setRest(45)
i01.head.eyeY.moveTo(45)
i01.head.jaw.setInverted(True)
i01.head.jaw.setMinMax(0,50)
i01.head.jaw.moveTo(20)
i01.head.mouthControl.setmouth(50,0)
############################################################

i01.head.headTracking.xpid.setPID(15.0,5.0,0.1)
i01.head.headTracking.ypid.setPID(15.0,5.0,0.1)
i01.head.eyesTracking.xpid.setPID(15.0,5.0,1.0)
i01.head.eyesTracking.ypid.setPID(15.0,5.0,1.0)

eyes = i01.getEyesTracking()
eyes.faceDetect()

ear = runtime.createAndStart("ear","Sphinx")
 
ear.attach(i01.head.mouthControl.mouth)
ear.addCommand("attach", "python", "attach")
ear.addCommand("detach", "python", "detach")
ear.addCommand("search humans", "i01.head.headTracking", "findFace")
ear.addCommand("stop searching", "i01.head.headTracking", "stopScan")
ear.addCommand("rest", "python", "rest")
 
ear.addCommand("manual", ear.getName(), "lockOutAllGrammarExcept", "voice control")
ear.addCommand("voice control", ear.getName(), "clearLock")
 
ear.addComfirmations("yes", "correct", "yeah", "ya")
ear.addNegations("no", "wrong", "nope", "nah")
 
# all commands MUST be before startListening
ear.startListening()

attach():
    i01.head.attach()
    i01.rightHand.attach()
    i01.rightArm.attach()
    i01.leftArm.attach()

detach():
    i01.head.detach()
    i01.rightHand.detach()
    i01.rightArm.detach()
    i01.leftArm.detach()

rest():
    i01.head.rest()
    i01.rightHand.rest()
    i01.rightArm.rest()
    i01.leftArm.rest()
