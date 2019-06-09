##############################################################
# MyRobotLab configuration file
# This file is generated from a running instance of MyRobotLab.
# It is meant to get MyRobotLab as close to that instance's state a possible.
# This file can be generated at any time using Runtime.save(filename)
# More information @ http://myrobotlab.org and https://github.com/myrobotlab
# version unknown
# generated Wed Jun 05 08:10:57 PDT 2019

##############################################################
## imports ####
import org.myrobotlab.framework.Platform as Platform
##############################################################
## creating and starting 63 services ####
# Although Runtime.start(name,type) both creates and starts services it might be desirable on creation to
# substitute peers, types or references of other sub services before the service is "started"
# e.g. i01 = Runtime.create('i01', 'InMoov') # this will "create" the service and config could be manipulated before starting 
# e.g. i01_left = Runtime.create('i01.left', 'Ssc32UsbServoController')
cli = Runtime.start('cli', 'Cli')
forwardServo = Runtime.start('forwardServo', 'HobbyServo')
gui = Runtime.start('gui', 'SwingGui')
harry = Runtime.start('harry', 'ProgramAB')
htmlfilter = Runtime.start('htmlfilter', 'HtmlFilter')
i01 = Runtime.start('i01', 'InMoov')
i01_ear = Runtime.start('i01.ear', 'WebkitSpeechRecognition')
i01_eyesTracking = Runtime.start('i01.eyesTracking', 'Tracking')
i01_eyesTracking_pid = Runtime.start('i01.eyesTracking.pid', 'Pid')
i01_head = Runtime.start('i01.head', 'InMoovHead')
i01_head_eyeX = Runtime.start('i01.head.eyeX', 'HobbyServo')
i01_head_eyeY = Runtime.start('i01.head.eyeY', 'HobbyServo')
i01_head_jaw = Runtime.start('i01.head.jaw', 'HobbyServo')
i01_head_neck = Runtime.start('i01.head.neck', 'HobbyServo')
i01_head_rollNeck = Runtime.start('i01.head.rollNeck', 'HobbyServo')
i01_head_rothead = Runtime.start('i01.head.rothead', 'HobbyServo')
i01_headTracking = Runtime.start('i01.headTracking', 'Tracking')
i01_headTracking_pid = Runtime.start('i01.headTracking.pid', 'Pid')
i01_left = Runtime.start('i01.left', 'Arduino')
i01_left_serial = Runtime.start('i01.left.serial', 'Serial')
i01_leftArm = Runtime.start('i01.leftArm', 'InMoovArm')
i01_leftArm_bicep = Runtime.start('i01.leftArm.bicep', 'HobbyServo')
i01_leftArm_omoplate = Runtime.start('i01.leftArm.omoplate', 'HobbyServo')
i01_leftArm_rotate = Runtime.start('i01.leftArm.rotate', 'HobbyServo')
i01_leftArm_shoulder = Runtime.start('i01.leftArm.shoulder', 'HobbyServo')
i01_leftHand = Runtime.start('i01.leftHand', 'InMoovHand')
i01_leftHand_index = Runtime.start('i01.leftHand.index', 'HobbyServo')
i01_leftHand_majeure = Runtime.start('i01.leftHand.majeure', 'HobbyServo')
i01_leftHand_pinky = Runtime.start('i01.leftHand.pinky', 'HobbyServo')
i01_leftHand_ringFinger = Runtime.start('i01.leftHand.ringFinger', 'HobbyServo')
i01_leftHand_thumb = Runtime.start('i01.leftHand.thumb', 'HobbyServo')
i01_leftHand_wrist = Runtime.start('i01.leftHand.wrist', 'HobbyServo')
i01_mouth = Runtime.start('i01.mouth', 'MarySpeech')
i01_mouth_audioFile = Runtime.start('i01.mouth.audioFile', 'AudioFile')
i01_mouthControl = Runtime.start('i01.mouthControl', 'MouthControl')
i01_opencv = Runtime.start('i01.opencv', 'OpenCV')
i01_right = Runtime.start('i01.right', 'Arduino')
i01_right_serial = Runtime.start('i01.right.serial', 'Serial')
i01_rightArm = Runtime.start('i01.rightArm', 'InMoovArm')
i01_rightArm_bicep = Runtime.start('i01.rightArm.bicep', 'HobbyServo')
i01_rightArm_omoplate = Runtime.start('i01.rightArm.omoplate', 'HobbyServo')
i01_rightArm_rotate = Runtime.start('i01.rightArm.rotate', 'HobbyServo')
i01_rightArm_shoulder = Runtime.start('i01.rightArm.shoulder', 'HobbyServo')
i01_rightHand = Runtime.start('i01.rightHand', 'InMoovHand')
i01_rightHand_index = Runtime.start('i01.rightHand.index', 'HobbyServo')
i01_rightHand_majeure = Runtime.start('i01.rightHand.majeure', 'HobbyServo')
i01_rightHand_pinky = Runtime.start('i01.rightHand.pinky', 'HobbyServo')
i01_rightHand_ringFinger = Runtime.start('i01.rightHand.ringFinger', 'HobbyServo')
i01_rightHand_thumb = Runtime.start('i01.rightHand.thumb', 'HobbyServo')
i01_rightHand_wrist = Runtime.start('i01.rightHand.wrist', 'HobbyServo')
i01_torso = Runtime.start('i01.torso', 'InMoovTorso')
i01_torso_lowStom = Runtime.start('i01.torso.lowStom', 'HobbyServo')
i01_torso_midStom = Runtime.start('i01.torso.midStom', 'HobbyServo')
i01_torso_topStom = Runtime.start('i01.torso.topStom', 'HobbyServo')
mixer = Runtime.start('mixer', 'ServoMixer')
python = Runtime.start('python', 'Python')
security = Runtime.start('security', 'Security')
vi01_left = Runtime.start('vi01.left', 'VirtualArduino')
vi01_left_uart = Runtime.start('vi01.left.uart', 'Serial')
vi01_right = Runtime.start('vi01.right', 'VirtualArduino')
vi01_right_uart = Runtime.start('vi01.right.uart', 'Serial')
webgui = Runtime.start('webgui', 'WebGui')

##############################################################
## configuring services ####
# Servo Config : forwardServo
# sets initial position of servo before moving
# in theory this is the position of the servo when this file was created
forwardServo.setPosition(90.0)
forwardServo.setMinMax(0.0,180.0)
forwardServo.setVelocity(-1.0)
forwardServo.setRest(90.0)
# forwardServo.setPin(null)
forwardServo.map(0.0,180.0,0.0,180.0)

# Servo Config : i01_head_eyeX
# sets initial position of servo before moving
# in theory this is the position of the servo when this file was created
i01_head_eyeX.setPosition(80.0)
i01_head_eyeX.setMinMax(60.0,100.0)
i01_head_eyeX.setVelocity(-1.0)
i01_head_eyeX.setRest(80.0)
i01_head_eyeX.setPin(22)
i01_head_eyeX.map(60.0,100.0,60.0,100.0)
i01_head_eyeX.attach("i01.left",22,80.0)
i01_head_eyeX.setAutoDisable(True)

# Servo Config : i01_head_eyeY
# sets initial position of servo before moving
# in theory this is the position of the servo when this file was created
i01_head_eyeY.setPosition(90.0)
i01_head_eyeY.setMinMax(50.0,100.0)
i01_head_eyeY.setVelocity(-1.0)
i01_head_eyeY.setRest(90.0)
i01_head_eyeY.setPin(24)
i01_head_eyeY.map(50.0,100.0,50.0,100.0)
i01_head_eyeY.attach("i01.left",24,90.0)
i01_head_eyeY.setAutoDisable(True)

# Servo Config : i01_head_jaw
# sets initial position of servo before moving
# in theory this is the position of the servo when this file was created
i01_head_jaw.setPosition(10.0)
i01_head_jaw.setMinMax(100.0,140.0)
i01_head_jaw.setVelocity(-1.0)
i01_head_jaw.setRest(110.0)
i01_head_jaw.setPin(26)
i01_head_jaw.map(100.0,140.0,100.0,140.0)
i01_head_jaw.attach("i01.right",26,110.0)
i01_head_jaw.setAutoDisable(True)

# Servo Config : i01_head_neck
# sets initial position of servo before moving
# in theory this is the position of the servo when this file was created
i01_head_neck.setPosition(140.0)
i01_head_neck.setMinMax(100.0,180.0)
i01_head_neck.setVelocity(-1.0)
i01_head_neck.setRest(140.0)
i01_head_neck.setPin(12)
i01_head_neck.map(100.0,180.0,100.0,180.0)
i01_head_neck.attach("i01.right",12,140.0)
i01_head_neck.setAutoDisable(True)

# Servo Config : i01_head_rollNeck
# sets initial position of servo before moving
# in theory this is the position of the servo when this file was created
i01_head_rollNeck.setPosition(90.0)
i01_head_rollNeck.setMinMax(20.0,160.0)
i01_head_rollNeck.setVelocity(-1.0)
i01_head_rollNeck.setRest(90.0)
i01_head_rollNeck.setPin(30)
i01_head_rollNeck.map(20.0,160.0,20.0,160.0)
i01_head_rollNeck.attach("i01.left",30,90.0)
i01_head_rollNeck.setAutoDisable(True)

# Servo Config : i01_head_rothead
# sets initial position of servo before moving
# in theory this is the position of the servo when this file was created
i01_head_rothead.setPosition(90.0)
i01_head_rothead.setMinMax(30.0,150.0)
i01_head_rothead.setVelocity(-1.0)
i01_head_rothead.setRest(90.0)
i01_head_rothead.setPin(13)
i01_head_rothead.map(30.0,150.0,30.0,150.0)
i01_head_rothead.attach("i01.right",13,90.0)
i01_head_rothead.setAutoDisable(True)

# Arduino Config : i01_left
i01_left.setVirtual(True)
# we have the following ports : [/dev/ttyACM1.UART, COM7.UART, COM7, COM3, /dev/ttyACM1]
i01_left.connect("COM7")
# make sure the pins are set before attaching
i01_leftArm_shoulder.setPin("10")
i01_leftHand_wrist.setPin("7")
i01_head_rollNeck.setPin("30")
i01_leftHand_majeure.setPin("4")
i01_leftHand_index.setPin("3")
i01_leftHand_thumb.setPin("2")
i01_leftArm_bicep.setPin("8")
i01_torso_lowStom.setPin("29")
i01_head_eyeY.setPin("24")
i01_leftHand_ringFinger.setPin("5")
i01_torso_midStom.setPin("28")
i01_head_eyeX.setPin("22")
i01_leftArm_omoplate.setPin("11")
i01_leftArm_rotate.setPin("9")
i01_leftHand_pinky.setPin("6")
i01_torso_topStom.setPin("27")
i01_left.attach("i01_leftArm_shoulder")
i01_left.attach("i01_leftHand_wrist")
i01_left.attach("i01_head_rollNeck")
i01_left.attach("i01_leftHand_majeure")
i01_left.attach("i01_leftHand_index")
i01_left.attach("i01_leftHand_thumb")
i01_left.attach("i01_leftArm_bicep")
i01_left.attach("i01_torso_lowStom")
i01_left.attach("i01_head_eyeY")
i01_left.attach("i01_leftHand_ringFinger")
i01_left.attach("i01_torso_midStom")
i01_left.attach("i01_head_eyeX")
i01_left.attach("i01_leftArm_omoplate")
i01_left.attach("i01_leftArm_rotate")
i01_left.attach("i01_leftHand_pinky")
i01_left.attach("i01_torso_topStom")

# Servo Config : i01_leftArm_bicep
# sets initial position of servo before moving
# in theory this is the position of the servo when this file was created
i01_leftArm_bicep.setPosition(5.0)
i01_leftArm_bicep.setMinMax(5.0,90.0)
i01_leftArm_bicep.setVelocity(-1.0)
i01_leftArm_bicep.setRest(5.0)
i01_leftArm_bicep.setPin(8)
i01_leftArm_bicep.map(5.0,90.0,5.0,90.0)
i01_leftArm_bicep.attach("i01.left",8,5.0)
i01_leftArm_bicep.setAutoDisable(True)

# Servo Config : i01_leftArm_omoplate
# sets initial position of servo before moving
# in theory this is the position of the servo when this file was created
i01_leftArm_omoplate.setPosition(10.0)
i01_leftArm_omoplate.setMinMax(10.0,80.0)
i01_leftArm_omoplate.setVelocity(-1.0)
i01_leftArm_omoplate.setRest(10.0)
i01_leftArm_omoplate.setPin(11)
i01_leftArm_omoplate.map(10.0,80.0,10.0,80.0)
i01_leftArm_omoplate.attach("i01.left",11,10.0)
i01_leftArm_omoplate.setAutoDisable(True)

# Servo Config : i01_leftArm_rotate
# sets initial position of servo before moving
# in theory this is the position of the servo when this file was created
i01_leftArm_rotate.setPosition(90.0)
i01_leftArm_rotate.setMinMax(40.0,180.0)
i01_leftArm_rotate.setVelocity(-1.0)
i01_leftArm_rotate.setRest(90.0)
i01_leftArm_rotate.setPin(9)
i01_leftArm_rotate.map(40.0,180.0,40.0,180.0)
i01_leftArm_rotate.attach("i01.left",9,90.0)
i01_leftArm_rotate.setAutoDisable(True)

# Servo Config : i01_leftArm_shoulder
# sets initial position of servo before moving
# in theory this is the position of the servo when this file was created
i01_leftArm_shoulder.setPosition(30.0)
i01_leftArm_shoulder.setMinMax(0.0,180.0)
i01_leftArm_shoulder.setVelocity(-1.0)
i01_leftArm_shoulder.setRest(30.0)
i01_leftArm_shoulder.setPin(10)
i01_leftArm_shoulder.map(0.0,180.0,0.0,180.0)
i01_leftArm_shoulder.attach("i01.left",10,30.0)
i01_leftArm_shoulder.setAutoDisable(True)

# Servo Config : i01_leftHand_index
# sets initial position of servo before moving
# in theory this is the position of the servo when this file was created
i01_leftHand_index.setPosition(2.0)
i01_leftHand_index.setMinMax(0.0,180.0)
i01_leftHand_index.setVelocity(-1.0)
i01_leftHand_index.setRest(2.0)
i01_leftHand_index.setPin(3)
i01_leftHand_index.map(0.0,180.0,0.0,180.0)
i01_leftHand_index.attach("i01.left",3,2.0)
i01_leftHand_index.setAutoDisable(True)

# Servo Config : i01_leftHand_majeure
# sets initial position of servo before moving
# in theory this is the position of the servo when this file was created
i01_leftHand_majeure.setPosition(2.0)
i01_leftHand_majeure.setMinMax(0.0,180.0)
i01_leftHand_majeure.setVelocity(-1.0)
i01_leftHand_majeure.setRest(2.0)
i01_leftHand_majeure.setPin(4)
i01_leftHand_majeure.map(0.0,180.0,0.0,180.0)
i01_leftHand_majeure.attach("i01.left",4,2.0)
i01_leftHand_majeure.setAutoDisable(True)

# Servo Config : i01_leftHand_pinky
# sets initial position of servo before moving
# in theory this is the position of the servo when this file was created
i01_leftHand_pinky.setPosition(2.0)
i01_leftHand_pinky.setMinMax(0.0,180.0)
i01_leftHand_pinky.setVelocity(-1.0)
i01_leftHand_pinky.setRest(2.0)
i01_leftHand_pinky.setPin(6)
i01_leftHand_pinky.map(0.0,180.0,0.0,180.0)
i01_leftHand_pinky.attach("i01.left",6,2.0)
i01_leftHand_pinky.setAutoDisable(True)

# Servo Config : i01_leftHand_ringFinger
# sets initial position of servo before moving
# in theory this is the position of the servo when this file was created
i01_leftHand_ringFinger.setPosition(2.0)
i01_leftHand_ringFinger.setMinMax(0.0,180.0)
i01_leftHand_ringFinger.setVelocity(-1.0)
i01_leftHand_ringFinger.setRest(2.0)
i01_leftHand_ringFinger.setPin(5)
i01_leftHand_ringFinger.map(0.0,180.0,0.0,180.0)
i01_leftHand_ringFinger.attach("i01.left",5,2.0)
i01_leftHand_ringFinger.setAutoDisable(True)

# Servo Config : i01_leftHand_thumb
# sets initial position of servo before moving
# in theory this is the position of the servo when this file was created
i01_leftHand_thumb.setPosition(2.0)
i01_leftHand_thumb.setMinMax(0.0,180.0)
i01_leftHand_thumb.setVelocity(-1.0)
i01_leftHand_thumb.setRest(2.0)
i01_leftHand_thumb.setPin(2)
i01_leftHand_thumb.map(0.0,180.0,0.0,180.0)
i01_leftHand_thumb.attach("i01.left",2,2.0)
i01_leftHand_thumb.setAutoDisable(True)

# Servo Config : i01_leftHand_wrist
# sets initial position of servo before moving
# in theory this is the position of the servo when this file was created
i01_leftHand_wrist.setPosition(90.0)
i01_leftHand_wrist.setMinMax(0.0,180.0)
i01_leftHand_wrist.setVelocity(-1.0)
i01_leftHand_wrist.setRest(90.0)
i01_leftHand_wrist.setPin(7)
i01_leftHand_wrist.map(0.0,180.0,0.0,180.0)
i01_leftHand_wrist.attach("i01.left",7,90.0)
i01_leftHand_wrist.setAutoDisable(True)

# Arduino Config : i01_right
i01_right.setVirtual(True)
# we have the following ports : [/dev/ttyACM1.UART, COM7.UART, COM7, COM3, /dev/ttyACM1]
i01_right.connect("/dev/ttyACM1")
# make sure the pins are set before attaching
i01_rightHand_majeure.setPin("4")
i01_rightHand_ringFinger.setPin("5")
i01_rightHand_wrist.setPin("7")
i01_head_rothead.setPin("13")
i01_rightArm_bicep.setPin("8")
i01_rightArm_shoulder.setPin("10")
i01_head_neck.setPin("12")
i01_rightHand_index.setPin("3")
i01_head_jaw.setPin("26")
i01_rightHand_thumb.setPin("2")
i01_rightArm_omoplate.setPin("11")
i01_rightArm_rotate.setPin("9")
i01_rightHand_pinky.setPin("6")
i01_right.attach("i01_rightHand_majeure")
i01_right.attach("i01_rightHand_ringFinger")
i01_right.attach("i01_rightHand_wrist")
i01_right.attach("i01_head_rothead")
i01_right.attach("i01_rightArm_bicep")
i01_right.attach("i01_rightArm_shoulder")
i01_right.attach("i01_head_neck")
i01_right.attach("i01_rightHand_index")
i01_right.attach("i01_head_jaw")
i01_right.attach("i01_rightHand_thumb")
i01_right.attach("i01_rightArm_omoplate")
i01_right.attach("i01_rightArm_rotate")
i01_right.attach("i01_rightHand_pinky")

# Servo Config : i01_rightArm_bicep
# sets initial position of servo before moving
# in theory this is the position of the servo when this file was created
i01_rightArm_bicep.setPosition(5.0)
i01_rightArm_bicep.setMinMax(5.0,90.0)
i01_rightArm_bicep.setVelocity(-1.0)
i01_rightArm_bicep.setRest(5.0)
i01_rightArm_bicep.setPin(8)
i01_rightArm_bicep.map(5.0,90.0,5.0,90.0)
i01_rightArm_bicep.attach("i01.right",8,5.0)
i01_rightArm_bicep.setAutoDisable(True)

# Servo Config : i01_rightArm_omoplate
# sets initial position of servo before moving
# in theory this is the position of the servo when this file was created
i01_rightArm_omoplate.setPosition(10.0)
i01_rightArm_omoplate.setMinMax(10.0,80.0)
i01_rightArm_omoplate.setVelocity(-1.0)
i01_rightArm_omoplate.setRest(10.0)
i01_rightArm_omoplate.setPin(11)
i01_rightArm_omoplate.map(10.0,80.0,10.0,80.0)
i01_rightArm_omoplate.attach("i01.right",11,10.0)
i01_rightArm_omoplate.setAutoDisable(True)

# Servo Config : i01_rightArm_rotate
# sets initial position of servo before moving
# in theory this is the position of the servo when this file was created
i01_rightArm_rotate.setPosition(90.0)
i01_rightArm_rotate.setMinMax(40.0,180.0)
i01_rightArm_rotate.setVelocity(-1.0)
i01_rightArm_rotate.setRest(90.0)
i01_rightArm_rotate.setPin(9)
i01_rightArm_rotate.map(40.0,180.0,40.0,180.0)
i01_rightArm_rotate.attach("i01.right",9,90.0)
i01_rightArm_rotate.setAutoDisable(True)

# Servo Config : i01_rightArm_shoulder
# sets initial position of servo before moving
# in theory this is the position of the servo when this file was created
i01_rightArm_shoulder.setPosition(30.0)
i01_rightArm_shoulder.setMinMax(0.0,180.0)
i01_rightArm_shoulder.setVelocity(-1.0)
i01_rightArm_shoulder.setRest(30.0)
i01_rightArm_shoulder.setPin(10)
i01_rightArm_shoulder.map(0.0,180.0,0.0,180.0)
i01_rightArm_shoulder.attach("i01.right",10,30.0)
i01_rightArm_shoulder.setAutoDisable(True)

# Servo Config : i01_rightHand_index
# sets initial position of servo before moving
# in theory this is the position of the servo when this file was created
i01_rightHand_index.setPosition(2.0)
i01_rightHand_index.setMinMax(0.0,180.0)
i01_rightHand_index.setVelocity(-1.0)
i01_rightHand_index.setRest(2.0)
i01_rightHand_index.setPin(3)
i01_rightHand_index.map(0.0,180.0,0.0,180.0)
i01_rightHand_index.attach("i01.right",3,2.0)
i01_rightHand_index.setAutoDisable(True)

# Servo Config : i01_rightHand_majeure
# sets initial position of servo before moving
# in theory this is the position of the servo when this file was created
i01_rightHand_majeure.setPosition(2.0)
i01_rightHand_majeure.setMinMax(0.0,180.0)
i01_rightHand_majeure.setVelocity(-1.0)
i01_rightHand_majeure.setRest(2.0)
i01_rightHand_majeure.setPin(4)
i01_rightHand_majeure.map(0.0,180.0,0.0,180.0)
i01_rightHand_majeure.attach("i01.right",4,2.0)
i01_rightHand_majeure.setAutoDisable(True)

# Servo Config : i01_rightHand_pinky
# sets initial position of servo before moving
# in theory this is the position of the servo when this file was created
i01_rightHand_pinky.setPosition(2.0)
i01_rightHand_pinky.setMinMax(0.0,180.0)
i01_rightHand_pinky.setVelocity(-1.0)
i01_rightHand_pinky.setRest(2.0)
i01_rightHand_pinky.setPin(6)
i01_rightHand_pinky.map(0.0,180.0,0.0,180.0)
i01_rightHand_pinky.attach("i01.right",6,2.0)
i01_rightHand_pinky.setAutoDisable(True)

# Servo Config : i01_rightHand_ringFinger
# sets initial position of servo before moving
# in theory this is the position of the servo when this file was created
i01_rightHand_ringFinger.setPosition(2.0)
i01_rightHand_ringFinger.setMinMax(0.0,180.0)
i01_rightHand_ringFinger.setVelocity(-1.0)
i01_rightHand_ringFinger.setRest(2.0)
i01_rightHand_ringFinger.setPin(5)
i01_rightHand_ringFinger.map(0.0,180.0,0.0,180.0)
i01_rightHand_ringFinger.attach("i01.right",5,2.0)
i01_rightHand_ringFinger.setAutoDisable(True)

# Servo Config : i01_rightHand_thumb
# sets initial position of servo before moving
# in theory this is the position of the servo when this file was created
i01_rightHand_thumb.setPosition(2.0)
i01_rightHand_thumb.setMinMax(0.0,180.0)
i01_rightHand_thumb.setVelocity(-1.0)
i01_rightHand_thumb.setRest(2.0)
i01_rightHand_thumb.setPin(2)
i01_rightHand_thumb.map(0.0,180.0,0.0,180.0)
i01_rightHand_thumb.attach("i01.right",2,2.0)
i01_rightHand_thumb.setAutoDisable(True)

# Servo Config : i01_rightHand_wrist
# sets initial position of servo before moving
# in theory this is the position of the servo when this file was created
i01_rightHand_wrist.setPosition(90.0)
i01_rightHand_wrist.setMinMax(0.0,180.0)
i01_rightHand_wrist.setVelocity(-1.0)
i01_rightHand_wrist.setRest(90.0)
i01_rightHand_wrist.setPin(7)
i01_rightHand_wrist.map(0.0,180.0,0.0,180.0)
i01_rightHand_wrist.attach("i01.right",7,90.0)
i01_rightHand_wrist.setAutoDisable(True)

# Servo Config : i01_torso_lowStom
# sets initial position of servo before moving
# in theory this is the position of the servo when this file was created
i01_torso_lowStom.setPosition(90.0)
i01_torso_lowStom.setMinMax(0.0,180.0)
i01_torso_lowStom.setVelocity(-1.0)
i01_torso_lowStom.setRest(90.0)
i01_torso_lowStom.setPin(29)
i01_torso_lowStom.map(0.0,180.0,0.0,180.0)
i01_torso_lowStom.attach("i01.left",29,90.0)
i01_torso_lowStom.setAutoDisable(True)

# Servo Config : i01_torso_midStom
# sets initial position of servo before moving
# in theory this is the position of the servo when this file was created
i01_torso_midStom.setPosition(90.0)
i01_torso_midStom.setMinMax(0.0,180.0)
i01_torso_midStom.setVelocity(-1.0)
i01_torso_midStom.setRest(90.0)
i01_torso_midStom.setPin(28)
i01_torso_midStom.map(0.0,180.0,0.0,180.0)
i01_torso_midStom.attach("i01.left",28,90.0)
i01_torso_midStom.setAutoDisable(True)

# Servo Config : i01_torso_topStom
# sets initial position of servo before moving
# in theory this is the position of the servo when this file was created
i01_torso_topStom.setPosition(90.0)
i01_torso_topStom.setMinMax(60.0,120.0)
i01_torso_topStom.setVelocity(-1.0)
i01_torso_topStom.setRest(90.0)
i01_torso_topStom.setPin(27)
i01_torso_topStom.map(60.0,120.0,60.0,120.0)
i01_torso_topStom.attach("i01.left",27,90.0)
i01_torso_topStom.setAutoDisable(True)

