#########################################
# BodyPart.py
# more info @: http://myrobotlab.org/service/Null
#########################################

# this script is provided as a basic scripting guide for control a skeleton tree
# or attach things to the InMoov service ( or an other root controller ), for a complete InMoov scripting ( if you don't use the GUI )
# just attach first parrents to inmoov service, ex :
# inMoov.attach(rightArm)

## THIS IS SAMPLE FOR A RIGHT ARM + A RIGHT HAND

##############
# Hardware setup
# Any compatible controller should work
rightPort="COM42"
# start optional virtual arduino service, used for internal test and virtual inmoov
#virtual=True
if ('virtual' in globals() and virtual):
    virtualArduinoRight = Runtime.start("virtualArduinoRight", "VirtualArduino")
    virtualArduinoRight.connect(rightPort)
# end used for internal test

# start your compatible controller, here it is Arduino
rightArduino = Runtime.createAndStart("rightArduino", "Arduino")
rightArduino.connect(rightPort)

# start your servos or diyServo for hand
# 6 servos for hand is standardized sample, you can use 1 or 99...
rightThumb = Runtime.start("rightHand.thumb", "Servo")
rightIndex = Runtime.start("rightHand.index", "Servo")
rightMajeure = Runtime.start("rightHand.majeure", "Servo")
rightRingFinger = Runtime.start("rightHand.ringFinger", "Servo")
rightPinky = Runtime.start("rightHand.pinky", "Servo")
rightWrist = Runtime.start("rightHand.wrist", "Servo")
#otherServo = Runtime.start(rightHand.otherServo,Servo)

# start your servos or diyServo for arm
# 6 servos for hand is standardized sample, you can use 1 or 99...
rightBicep = Runtime.start("rightArm.bicep", "Servo")
rightRotate = Runtime.start("rightArm.rotate", "Servo")
rightShoulder = Runtime.start("rightArm.shoulder", "Servo")
rightomoplate = Runtime.start("rightArm.omoplate", "Servo")
#otherServo = Runtime.start(rightArm.otherServo,Servo)



##############
# Start body parts
rightHand = Runtime.start("rightHand", "BodyPart")
rightArm = Runtime.start("rightArm", "BodyPart")


##############
# Attach things !

# servo to arduino
rightThumb.attach(rightArduino, 2)
rightIndex.attach(rightArduino, 3)
rightMajeure.attach(rightArduino, 4)
rightRingFinger.attach(rightArduino, 5)
rightPinky.attach(rightArduino, 6)
rightWrist.attach(rightArduino, 7)

rightBicep.attach(rightArduino, 8)
rightRotate.attach(rightArduino, 9)
rightShoulder.attach(rightArduino, 10)
rightomoplate.attach(rightArduino, 11)

# leafs to node
rightHand.attach(rightThumb,rightIndex,rightMajeure,rightRingFinger,rightPinky,rightWrist)
rightArm.attach(rightBicep,rightRotate,rightShoulder,rightomoplate)
# don't forget this ( child to parrent ) :
rightArm.attach(rightHand)

###############
# you can use servo Gui to calibrate and tweak servo in live after script start, it is easier
# or override here like it :
# rightHand.get("thumb").map(0,180,64,135)
# rightHand.get("thumb").setRest(90)

# lets control now the skeleton with basic methods for now ( goal is to use IK later )
# we are waiting every fingers closed, then full open hand

rightHand.setVelocity(40,40,40,40,40)

rightArm.moveToBlocking("Hand",0,0,0,0,0)
rightArm.moveToBlocking("Hand",180,180,180,180,180)

# you can also control body part directly :

rightArm.moveTo(90,90,90,90)