# a minimal tracking script - this will start all peer
# services and attach everything appropriately
# change parameters depending on your pan tilt, pins and
# Arduino details
# all commented code is not necessary but allows custom
# options
 
port = "COM12"
xServoPin = 13
yServoPin = 12
#select the pin where to start polling ( we can connect a PIR to this pin to see his state HIGH/LOW)
readDigitalPin = 8
 
tracker = Runtime.createAndStart("tracker", "Tracking")
 
# set specifics on each Servo
servoX = tracker.getX()
servoX.setPin(xServoPin)
servoX.setMinMax(30, 150)
 
servoY = tracker.getY()
servoY.setPin(yServoPin)
servoY.setMinMax(30, 150)
 
# optional filter settings
opencv = tracker.getOpenCV()
 
# setting camera index to 1 default is 0
opencv.setCameraIndex(1) 
 
# connect to the Arduino
tracker.connect(port)
#set a Low sample rate, we don't want to bork serial connection !

arduino = tracker.getArduino()

arduino.setSampleRate(8000)

#start polling data from the digital pin
arduino.digitalReadPollingStart(readDigitalPin)
#add python as listener of the arduino service, each time arduino publish the value of the pin
arduino.addListener("publishPin", "python", "publishPin")

#define a function which is called every time arduino publish the value of the pin
def publishPin():
  pin = msg_tracker_arduino_publishPin.data[0]
  print pin.pin, pin.value,pin.type,pin.source
  #if an HIGH state is read, PIR is detecting something so start face tracking
  if (pin.value == 1):
   if tracker.isIdle():
    tracker.faceDetect()
  #if a LOW state is read , stop tracking.. there is no human there !
  elif (pin.value == 0):
   if not tracker.isIdle():
    tracker.stopTracking()
 
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
# tracker.startLKTracking()
 
# scans for faces - tracks if found
# tracker.findFace()