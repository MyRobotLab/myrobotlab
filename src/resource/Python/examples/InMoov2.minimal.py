#file : InMoov2.minimal.py

# this will run with versions of MRL above 1695
# a very minimal script for InMoov
# although this script is very short you can still
# do voice control of a right hand or finger box
# for any command which you say - you will be required to say a confirmation
# e.g. you say -> open hand, InMoov will ask -> "Did you say open hand?", you will need to
# respond with a confirmation ("yes","correct","yeah","ya")

rightPort = "COM8"

i01 = Runtime.createAndStart("i01", "InMoov")
i01.startEar()
##############
i01.startRightHand(rightPort)
# tweaking defaults settings of right hand
#i01.rightHand.thumb.setMinMax(55,135)
#i01.rightHand.index.setMinMax(0,160)
#i01.rightHand.majeure.setMinMax(0,140)
#i01.rightHand.ringFinger.setMinMax(48,145)
#i01.rightHand.pinky.setMinMax(45,146)
#i01.rightHand.thumb.map(0,180,55,135)
#i01.rightHand.index.map(0,180,0,160)
#i01.rightHand.majeure.map(0,180,0,140)
#i01.rightHand.ringFinger.map(0,180,48,145)
#i01.rightHand.pinky.map(0,180,45,146)
#################

# verbal commands
ear = i01.ear

ear.addCommand("attach right hand", "i01.rightHand", "attach")
ear.addCommand("disconnect right hand", "i01.rightHand", "detach")
ear.addCommand("rest", i01.getName(), "rest")
ear.addCommand("open hand", "python", "handopen")
ear.addCommand("close hand", "python", "handclose")
ear.addCommand("capture gesture", ear.getName(), "captureGesture")
ear.addCommand("manual", ear.getName(), "lockOutAllGrammarExcept", "voice control")
ear.addCommand("voice control", ear.getName(), "clearLock")
 
ear.addComfirmations("yes","correct","yeah","ya")
ear.addNegations("no","wrong","nope","nah")

ear.startListening()

def handopen():
  i01.moveHand("left",0,0,0,0,0)
  i01.moveHand("right",0,0,0,0,0)

def handclose():
  i01.moveHand("left",180,180,180,180,180)
  i01.moveHand("right",180,180,180,180,180)