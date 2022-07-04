#########################################
# Servo.py
# categories: servo
# more info @: http://myrobotlab.org/service/Servo
#########################################
# uncomment for virtual hardware
Platform.setVirtual(True)

# Every settings like limits / port number / controller are saved after initial use
# so you can share them between differents script 

servoPin01 = 4
servoPin02 = 5

# port = "/dev/ttyUSB0"
port = "COM15"

# create a servo controller and a servo
arduino = runtime.start("arduino","Arduino")
servo01 = runtime.start("servo01","Servo")
servo02 = runtime.start("servo02","Servo")


# initialize arduino
# linux or macos -> arduino.connect("/dev/ttyUSB0")
print("connecting arduino to serial port")
arduino.connect(port)

# set limits
print("setting min max limits of servo")
# servo01.setMinMax(0, 180)
servo01.map(0, 180, 0, 180)

# set rest position
servo01.setRest(90)

# attach servo
print("attaching servo with pins to controller")
servo01.attach(arduino.getName(), servoPin01)
servo02.attach(arduino.getName(), servoPin02)

# auto disable - this enables (starts pwm) before a movement
# and disables (stops pwm) after a movement
servo01.setAutoDisable(True)
# servo02.setAutoDisable(False)

# speed changes
print("speed changes")
servo01.setSpeed(20) ## Low speed
servo01.moveToBlocking(90) # moveToBlocking will wait for finished move

servo01.setSpeed(50) ## medium speed
servo01.moveToBlocking(180) # moveToBlocking will wait for finished move

servo01.unsetSpeed() ## max speed ( no more speed conytol )
servo01.moveTo(0) # we cannot use moveToBlocking if servo speed is set to -1 ( max ) !!
sleep(2)


# fast sweep 10 seconds
print("fast sweep")
servo01.setSpeed(300)
servo01.sweep(0, 180);
sleep(10)
servo01.stop()


# print info
print("servo position :{}".format(servo01.getPos()))
print("servo pin :{}".format(servo01.getPin()))
print("servo rest position :{}".format(servo01.getRest()))
print("servo speed :{}".format(servo01.getSpeed()))
print("servo is inverted :{}".format(servo01.isInverted()))
print("servo min :{}".format(servo01.getMin()))
print("servo max :{}".format(servo01.getMax()))


# sync servo02 with servo01
# now servo2 will be a slave to servo01
print("syncing servo02 with servo01")
servo02.sync(servo01)

servo01.moveTo(10)
sleep(0.5)

servo01.moveTo(179)
sleep(0.5)

servo01.moveTo(10)
sleep(0.5)


# writing position in us
servo01.writeMicroseconds(1875)
print("servo position :{}".format(servo01.getPos())) # check if correct ?


# moving to rest position
print("servo01 rest")
servo01.rest()
sleep(2)

# turn off power if servo01.setAutoDisable(False)
print("turn of servos pwm")
servo01.disable()
servo02.disable()

# detaching servo01 from controller
# TODO - make arduino.detach() detach all services
print("detaching servos from controller")
servo01.detach()
servo02.detach()