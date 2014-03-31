exec arduino = Runtime.createAndStart("arduino","Arduino")
joystick = Runtime.createAndStart("joystick","Joystick")

dcMotorLeft = Runtime.createAndStart("dcMotorLeft","Servo")
dcMotorRight = Runtime.createAndStart("dcMotorRight","Servo")

shoulderPitchRight = Runtime.createAndStart("shoulderPitchRight","Servo")
shoulderYawRight = Runtime.createAndStart("shoulderYawRight","Servo")
elbowRight = Runtime.createAndStart("elbowRight","Servo")
handRight = Runtime.createAndStart("handRight","Servo")
headPan = Runtime.createAndStart("headPan","Servo")
headTilt = Runtime.createAndStart("headTilt","Servo")

arduino.connect("COM3")
sleep(4)

joystick.setController(2)
joystick.startPolling()

arduino.attach(dcMotorLeft.getName(),2)
arduino.attach(dcMotorRight.getName(),3)

arduino.attach(shoulderPitchRight.getName(),6)
arduino.attach(shoulderYawRight.getName(),7)
arduino.attach(elbowRight.getName(),8)
arduino.attach(handRight.getName(),9)
arduino.attach(headPan.getName(),10)
arduino.attach(headTilt.getName(),11)

a = 0
b = 0
dp = 0
rb = 0
sx = 0
sy = 0
x = 0
y = 0
z = 0

shoulderPitchRight.setSpeed(0.85)
shoulderYawRight.setSpeed(0.85)
elbowRight.setSpeed(0.85)
handRight.setSpeed(0.85)
headPan.setSpeed(0.85)
headTilt.setSpeed(0.85)


shoulderPitchRight.moveTo(145)
shoulderYawRight.moveTo(70)
elbowRight.moveTo(80)
handRight.moveTo(40)
headPan.moveTo(95)
headTilt.moveTo(90)


	# define buttons' variables

def buttonA():
    global a
    a = msg_joystick_button1.data[0]
    print a
    check()

def buttonB():
    global b
    b = msg_joystick_button2.data[0]
    print b
    check()

def buttonLTandRT():
    global z
    z = msg_joystick_ZAxisRaw.data[0]
    print z
    check()

def buttonRB():
    global rb
    rb = msg_joystick_button6.data[0]
    print rb
    check()

def buttonX():
    global x
    x = msg_joystick_button3.data[0]
    print x
    check()

def buttonY():
    global y
    y = msg_joystick_button4.data[0]
    print y
    check()

def directional():
    global dp
    dp = msg_joystick_hatSwitchRaw.data[0]
    print dp
    check()

def leftStickX():
    global sx
    sx = msg_joystick_XAxisRaw.data[0]
    print sx
    check()

def leftStickY():
    global sy
    sy = msg_joystick_YAxisRaw.data[0]
    print sy
    check()


	# define moves
	
def forward():
 print "Forward"
 dcMotorLeft.moveTo(180)
 dcMotorRight.moveTo(180)

def backward():
 print "Backward"
 dcMotorLeft.moveTo(0)
 dcMotorRight.moveTo(0)

def turnLeft():
 print "Turn left"
 dcMotorLeft.moveTo(0)
 dcMotorRight.moveTo(180)

def turnRight():
 print "Turn right"
 dcMotorLeft.moveTo(180)
 dcMotorRight.moveTo(0)


def check():
	# dc motors
    if ((sx >= -1) and (sx <= -0.8)):
     turnLeft()
    elif ((sx <= 1) and (sx >= 0.8)):
     turnRight()
    elif ((sy >= -1) and (sy <= -0.8)):
     forward()
    elif ((sy <= 1) and (sy >= 0.8)):
     backward()

	# right pitch shoulder
    elif ((dp == 0.25) and (rb == 1)):
     print "Right pitch shoulder decreasing"
     shoulderPitchRight.moveTo(20)
    elif ((dp == 0.75) and (rb == 1)):
     print "Right pitch shoulder increasing"
     shoulderPitchRight.moveTo(160)

	# right yaw shoulder
    elif ((dp == 0.5) and (rb == 1)):
     print "Right yaw shoulder decreasing"
     shoulderYawRight.moveTo(0)
    elif ((dp == 1) and (rb == 1)):
     print "Right yaw shoulder increasing"
     shoulderYawRight.moveTo(110)

	# right elbow
    elif ((dp == 0.25) and (z <= -0.996)):
     print "Right elbow decreasing"
     elbowRight.moveTo(20)
    elif ((dp == 0.75) and (z <= -0.996)):
     print "Right elbow increasing"
     elbowRight.moveTo(130)

	# hand
    elif ((z <= -0.996) and (a == 1)):
     print "Hand closing"
     handRight.moveTo(25)
    elif ((z <= -0.996) and (b == 1)):
     print "Hand opening"
     handRight.moveTo(125)
     
	# head
    elif ((dp == 0.5) and (y == 1)):
     print "Look right"
     headPan.moveTo(150)
    elif ((dp == 1) and (y == 1)):
     print "Look left"
     headPan.moveTo(40)
    elif ((dp == 0.25) and (y == 1)):
     print "Look down"
     headTilt.moveTo(20)
    elif ((dp == 0.75) and (y == 1)):
     print "Look up"
     headTilt.moveTo(125)

	# while nothing pre-defined
    else:
     print "Just waiting..."
     dcMotorLeft.moveTo(90)
     dcMotorRight.moveTo(90)
     shoulderPitchRight.stopServo()
     shoulderYawRight.stopServo()
     elbowRight.stopServo()
     handRight.stopServo()
     headPan.stopServo()
     headTilt.stopServo()


joystick.addListener("button1", python.name, "buttonA")
joystick.addListener("button2", python.name, "buttonB")
joystick.addListener("button3", python.name, "buttonX")
joystick.addListener("button4", python.name, "buttonY")
joystick.addListener("button6", python.name, "buttonRB")
joystick.addListener("hatSwitchRaw", python.name, "directional")
joystick.addListener("XAxisRaw", python.name, "leftStickX")
joystick.addListener("YAxisRaw", python.name, "leftStickY")
joystick.addListener("ZAxisRaw", python.name, "buttonLTandRT")