# a minimal tracking script - this will start all peer
# services and attach everything appropriately
# change parameters depending on your pan tilt, pins and
# Arduino details
# all commented code is not necessary but allows custom
# options

port = "COM19"   #change COM port to your own port
xServoPin = 13   #change this to the right servo pin if needed, for inmoov this is right
yServoPin = 12   #change this to the right servo pin if needed, for inmoov this is right

tracker = Runtime.createAndStart("tracker", "Tracking")

# set specifics on each Servo
servoX = tracker.getX()
servoX.setPin(xServoPin)
servoX.setMinMax(30, 150)  #minimum and maximum settings for the X servo

servoY = tracker.getY()
servoY.setPin(yServoPin)
servoY.setMinMax(30, 150)  #minimum and maximum settings for the Y servo

# changing PID values change the 
# speed and "jumpyness" of the Servos
xpid = tracker.getXPID()
ypid = tracker.getYPID()

# these are default setting
# adjust to make more smooth
# or faster
# xpid.setPID(5.0, 5.0, 0.1)
# ypid.setPID(5.0, 5.0, 0.1)

# optional filter settings
opencv = tracker.getOpenCV()

# setting camera index to 1 default is 0
opencv.setCameraIndex(1) 

# connect to the Arduino
tracker.connect(port)

# Gray & PyramidDown make face tracking
# faster - if you dont like these filters - you
# may remove them before you select a tracking type with
# the following command
# tracker.clearPreFilters()

# diffrent types of tracking

# simple face detection and tracking
# tracker.faceDetect()

# lkpoint - click in video stream with 
# mouse and it should track
tracker.startLKTracking()

# scans for faces - tracks if found
# tracker.findFace() 
