#inmoov2.ear and mouth talk
headPort = "COM7"
headPort = "COM15"

i01 = Runtime.createAndStart("i01", "InMoov")
head = i01.startHead(headPort)
#neck = i01.getHeadTracking()
#neck.faceDetect()

#eyes = i01.getEyesTracking()
#eyes.faceDetect()
############################################################
#if needed we can tweak the default settings with these lines
i01.head.jaw.setMinMax(6,30)
i01.head.mouthControl.setmouth(6,30)
############################################################

ear = runtime.createAndStart("ear","Sphinx")


ear.attach(i01.head.mouthControl.mouth)
ear.addCommand("attach head", "i01.head", "attach")
ear.addCommand("disconnect head", "i01.head", "detach")

ear.addCommand("manual", ear.getName(), "lockOutAllGrammarExcept", "voice control")
ear.addCommand("voice control", ear.getName(), "clearLock")
 
ear.addComfirmations("yes", "correct", "yeah", "ya")
ear.addNegations("no", "wrong", "nope", "nah")

# all commands MUST be before startListening
ear.startListening()