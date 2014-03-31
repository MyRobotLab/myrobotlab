# system specific variables
port = "COM12"
servoPin = 6
joystickIndex = 7
# set servo speed 
# if you are going to do "stops" on a servo
# it MUST have speed < 1.0
servoSpeed = 0.7

# globals
servoDirection = 0
# max and min of servos - use to limit the movement
servoMin = 0
servoMax = 180

joystick = Runtime.createAndStart("joystick", "Joystick")
arduino = Runtime.createAndStart("arduino", "Arduino")
servo = Runtime.createAndStart("servo", "Servo")

if (not joystick.isPolling()):
	joystick.setController(joystickIndex)
	joystick.startPolling()

if (not arduino.isConnected()):
	print "connecting arduino"
	arduino.connect(port)
	print "attaching servo"
  servo.attach(arduino.getName(), servoPing)
	# add the python service as a listener to the joystick event YAxisRaw
	joystick.addListener("YAxisRaw", python.name, "y")
	# add the ptyon service as a listener to the joystick event buttons
	joystick.addListener("button1", python.name, "button1")
	joystick.addListener("button5", python.name, "button5")
	joystick.addListener("button7", python.name, "button7")

servo.setSpeed(servoSpeed)

# servo direction 
# joystick all the way down = down
# joystick all the way up = up
# joystick <> up || down = no direction
def y():
	global servoDirection
	servoDirection = msg_joystick_YAxisRaw.data[0]
	if (servoDirection == 1):
		print "servo down"
	elif (servoDirection == -1):
		print "servo up"
	else:
		servoDirection = 0
		print "servo no movement"

# servo move / stop
# click = move
# release = stop
def button1():
	global servoDirection
	global servoMin
	global servoMax
	# API is now sissy 1 based for those who don't know how to deal with 0 index :)
	button1 = msg_joystick_button1.data[0]
	if (button1 == 1):
		print "servo will move"
		if (servoDirection == 1):
			print 'down'
			servo.moveTo(servoMin)
		elif (servoDirection == -1):
			servo.moveTo(servoMax)
			print 'up'
		else:
			print 'no direction'
	else:
		servo.stopServo()
		print 'is not moving'

# increase speed 5%
def button5():
	button5 = msg_joystick_button5.data[0]
	if (button5 == 1):
		# only care about pressed
		global servoSpeed
		servoSpeed += 0.05
		if (servoSpeed > 0.95): # we don't want to go to 1.0
			servoSpeed = 0.95
		servo.setSpeed(servoSpeed)
		print "speed is now ", servoSpeed

# decrease speed 5%
def button7():
	button7 = msg_joystick_button7.data[0]
	# only care about pressed
	if (button7 == 1):
		global servoSpeed
		servoSpeed -= 0.05
		if (servoSpeed < 0.05):
			servoSpeed = 0.05
		servo.setSpeed(servoSpeed)
		print "speed is now ", servoSpeed
