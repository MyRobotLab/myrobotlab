#########################################
# Servo01_start.py
# categories: intro
# more info @: http://myrobotlab.org/service/Intro
#########################################
# uncomment for virtual hardware
# Platform.setVirtual(True)

# Every settings like limits / port number / controller are saved after initial use
# so you can share them between differents script 

# servoPin01 = 4

# port = "/dev/ttyUSB0"
# port = "COM15"

# create a servo controller and a servo
arduino = runtime.start("arduino","Arduino")
servo01 = runtime.start("servo01","Servo")

# initialize arduino
# linux or macos -> arduino.connect("/dev/ttyUSB0")
# print("connecting arduino to serial port")
# arduino.connect(port)

# set limits
print("setting min max limits of servo")
# servo01.setMinMax(0, 180)
servo01.map(0, 180, 0, 180)

# set rest position
servo01.setRest(90)
# set speed
servo01.setSpeed(100.0)

# attach servo
# print("attaching servo with pins to controller")
# servo01.attach(arduino.getName(), servoPin01)

# auto disable - this enables (starts pwm) before a movement
# and disables (stops pwm) after a movement
servo01.setAutoDisable(True)


