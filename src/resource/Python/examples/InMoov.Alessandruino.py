# a minimal tracking script - this will start all peer
# services and attach everything appropriately
# change parameters depending on your pan eyeY, pins and
# Arduino details
# all commented code is not necessary but allows custom
# options
 
port = "COM8"
eyeXPin = 3
eyeYPin = 6
headXPin = 10
headYPin = 9 
 
eyes = Runtime.create("eyes", "Tracking")
head = Runtime.create("head", "Tracking")

# name to bind correctly
eyes.reserveAs("x", "eyeX")
eyes.reserveAs("y", "eyeY")
eyes.reserveAs("xpid", "eyeXPID")
eyes.reserveAs("ypid", "eyeYPID")

head.reserveAs("x", "rothead")
head.reserveAs("y", "neck")
head.reserveAs("xpid", "rotheadPID")
head.reserveAs("ypid", "neckPID")
 
# naming - binding of peer services is done with service names
# the Tracking service will use the following default names
# arduinoName = "arduino" - the arduino controller - used to control the servos
# xpidName = "xpid" - the PID service to control X tracking
# ypidName = "ypid" - the PID service to control Y tracking
# xName = "x" - the x servo (pan)
# yName = "y" - the y servo (eyeY)
# opencvName = "opencv" - the camera
 
# after the Tracking service is "created" you may create peer service
# and change values of that service - for example if we want to invert a
# servo :
eyeY = Runtime.create("eyeY", "Servo")
eyeY.setInverted(True)

 
# initialization 
eyes.connect(port)
eyes.attachServos(eyeXPin, eyeYPin)
head.attachServos(headXPin, headYPin)
 
# set limits if necessary
# default is servo limits
eyes.setServoLimits(65, 90, 22, 85) 
 
# set rest position default is 90 90
eyes.setRestPosition(80, 47) 
 
#eyes.setPIDDefaults()
# changing PID values 
# setXPID(Kp, Ki, Kd, Direction 0=direct 1=reverse, Mode 0=manual 1= automatic, minOutput, maxOutput, sampleTime, setPoint);
# defaults look like this_AUTOMATIC
eyes.setXPID(10.0, 5, 1, 0, 1, -10, 10, 30, 0.5)
eyes.setYPID(10.0, 5, 1, 0, 1, -10, 10, 30, 0.5)
head.setXPID(5.0, 0, 0.1, 0, 1, -1, 1, 30, 0.5)
head.setYPID(5.0, 0, 0.1, 0, 1, -1, 1, 30, 0.5)
eyes.startService()
head.startService()

# set a point and track it
# there are two interfaces one is float value
# where 0.5,0.5 is middle of screen
# eyes.trackPoint(0.5, 0.5)
 
# don't be surprised if the point does not
# stay - it needs / wants a corner in the image
# to presist - otherwise it might disappear
# you can set points manually by clicking on the
# opencv screen
 
# face tracking from face detection filter
eyes.faceDetect()
head.faceDetect()