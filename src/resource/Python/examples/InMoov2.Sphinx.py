headPort = "COM8"
headPort = "COM17"

i01 = Runtime.createAndStart("i01", "InMoov")

mouth = i01.startMouth()
ear = i01.startEar()


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
