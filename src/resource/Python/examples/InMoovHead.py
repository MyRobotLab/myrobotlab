#tweak these values to match the port of your Arduino!
headPort = "COM5"
mouthPort="COM5"

#starting the InMoov-service 
i01 = Runtime.createAndStart("i01", "InMoov")

#starting the mouth
i01.startMouth()
#set a custom voice
i01.mouth.setGoogleURI("http://thehackettfamily.org/Voice_api/api2.php?voice=Graham&txt=")

#start the (InMoov-)head(-service) 
head = i01.startHead(headPort)

#start the head- & eyes-tracking(-services)
headTracking = i01.startHeadTracking(headPort)
eyesTracking= i01.startEyesTracking(headPort)

#start the head- & eyes-tracking 
i01.headTracking.startLKTracking()
i01.eyesTracking.startLKTracking()

#custom smoothing values for the tracking-services 
i01.headTracking.xpid.setPID(10.0,5.0,0.1)
i01.headTracking.ypid.setPID(15.0,5.0,0.1)
i01.eyesTracking.xpid.setPID(15.0,5.0,0.1)
i01.eyesTracking.ypid.setPID(15.0,5.0,0.1)

#start the ear(-service)
i01.startEar()

############################################################
#min- & max-values for the servos
#i01.head.eyeY.setMinMax(22,85)
#i01.head.eyeX.setMinMax(60,85)
#i01.head.eyeY.setRest(45)
#i01-head.eyeX.setRest(45)
#i01.head.jaw.setMinMax(10,75)
#i01.mouthControl.setmouth(75,55)
############################################################
 

#verbal commands 
ear = i01.ear
 
ear.addCommand("attach", "i01", "attach")
ear.addCommand("detach", "i01", "detach")
ear.addCommand("track humans", "python", "trackHumans")
ear.addCommand("track point", "python", "trackPoint")
ear.addCommand("stop tracking", "python", "stopTracking")
ear.addCommand("rest position", "i01.head", "rest")
 
 
ear.addComfirmations("yes", "correct", "yeah", "ya")
ear.addNegations("no", "wrong", "nope", "nah")
 


def trackHumans():
  i01.headTracking.faceDetect()
  i01.eyesTracking.faceDetect()

def trackPoint():
  i01.headTracking.startLKTracking()
  i01.eyesTracking.startLKTracking()

def stopTracking():
  i01.headTracking.stopTracking()
  i01.eyesTracking.stopTracking()
  

# all commands MUST be before startListening  
ear.startListening()
