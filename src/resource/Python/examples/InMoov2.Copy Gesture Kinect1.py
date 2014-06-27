leftPort = "COM7"
rightPort = "COM8"
 
i01 = Runtime.createAndStart("i01","InMoov")
i01.startMouth()
#to tweak the default voice
i01.mouth.setGoogleURI("http://thehackettfamily.org/Voice_api/api2.php?voice=Ryan&txt=")
i01.startHead(leftPort)
i01.startEar()
# tweaking default settings of eyes
i01.head.eyeY.setMinMax(80,100)
i01.head.eyeY.setRest(78)
i01.head.eyeX.setMinMax(75,100)
i01.head.eyeX.setRest(82)
i01.head.neck.setRest(80)
i01.head.rothead.setRest(86)
##############
i01.startMouthControl(leftPort)
# tweaking default settings of jaw
i01.head.jaw.setMinMax(13,55)
i01.mouthControl.setmouth(13,38)
i01.startLeftArm(leftPort)
i01.startRightArm(rightPort)
 
i01.leftArm.bicep.setMinMax(5,90)
 
#i01.leftArm.bicep.setMinMax(0,70)
#i01.leftArm.rotate.setMinMax(46,160)
#i01.leftArm.shoulder.setMinMax(30,100)
i01.leftArm.omoplate.setMinMax(11,75)

i01.rightArm.bicep.setMinMax(5,90)
 
#i01.rightArm.bicep.setMinMax(0,70)
#i01.rightArm.rotate.setMinMax(46,160)
#i01.rightArm.shoulder.setMinMax(30,100)
i01.rightArm.omoplate.setMinMax(11,75)
 
i01.copyGesture(True)
# verbal commands
ear = i01.ear
 
ear.addCommand("rest", i01.getName(), "rest")

ear.addCommand("attach head", "i01.head", "attach")
ear.addCommand("disconnect head", "i01.head", "detach")
ear.addCommand("attach eyes", "i01.head.eyeY", "attach")
ear.addCommand("disconnect eyes", "i01.head.eyeY", "detach")
ear.addCommand("attach right hand", "i01.rightHand", "attach")
ear.addCommand("disconnect right hand", "i01.rightHand", "detach")
ear.addCommand("attach left hand", "i01.leftHand", "attach")
ear.addCommand("disconnect left hand", "i01.leftHand", "detach")
ear.addCommand("attach all", "i01", "attach")
ear.addCommand("disconnect all", "i01", "detach")
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
ear.addCommand("fist hips", i01.getName(), "fistHips")
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
#ear.addCommand("power down", "python", "powerdown")
#ear.addCommand("power up", "python", "powerup")
ear.addCommand("servo", "python", "servos")
ear.addCommand("how many fingers do you have", "python", "howmanyfingersdoihave")
ear.addCommand("who's there", "python", "welcome")
 
ear.addComfirmations("yes","correct","ya","yeah")
ear.addNegations("no","wrong","nope","nah")
 
ear.startListening()