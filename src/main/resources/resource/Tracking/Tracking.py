# a minimal tracking script - this will start all peer
# services and attach everything appropriately
# change parameters depending on your pan tilt, pins and
# Arduino details
# all commented code is not necessary but allows custom
# options

port = "COM19"   #change COM port to your own port
xServoPin = 13   #change this to the right servo pin if needed, for inmoov this is right
yServoPin = 12   #change this to the right servo pin if needed, for inmoov this is right

# create a servo controller and a servo
arduino = runtime.start("arduino","Arduino")
xServo = runtime.start("xServo","Servo")
yServo = runtime.start("yServo","Servo")

# start optional virtual arduino service, used for test
#virtual=1
if ('virtual' in globals() and virtual):
    virtualArduino = runtime.start("virtualArduino", "VirtualArduino")
    virtualArduino.connect(port)

arduino.connect(port)
xServo.attach(arduino.getName(), xServoPin)
yServo.attach(arduino.getName(), yServoPin)

tracker = runtime.start("tracker", "Tracking")

opencv=tracker.getOpenCV()

# set specifics on each Servo
xServo.setMinMax(30, 150)  #minimum and maximum settings for the X servo
# servoX.setInverted(True) # invert if necessary

yServo.setMinMax(30, 150)  #minimum and maximum settings for the Y servo
# servoY.setInverted(True) # invert if necessary

# changing Pid values change the 
# speed and "jumpyness" of the Servos
pid = tracker.getPID()

# these are default setting
# adjust to make more smooth
# or faster

pid.setPID("x",5.0, 5.0, 0.1)
pid.setPID("y",5.0, 5.0, 0.1)
  
# connect to the Arduino ( 0 = camera index )
tracker.connect(opencv, xServo, yServo)

opencv.broadcastState();
sleep(1)

# Gray & PyramidDown make face tracking
# faster - if you dont like these filters - you
# may remove them before you select a tracking type with
# the following command
# tracker.clearPreFilters()

# diffrent types of tracking

# lkpoint - click in video stream with 
# mouse and it should track
# simple point detection and tracking
# tracker.startLKTracking()
# scans for faces - tracks if found
# tracker.findFace()
# tracker + facedetect : tracker.faceDetect(True)
tracker.faceDetect()
