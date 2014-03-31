########################################################
# authors : GroG & michael96_27
# references:
# http://myrobotlab.org/content/color-tracking-mrl-using-opencv-python-and-arduino-services
# File colorTracking.py
# //////////BEGIN PYTHON SCRIPT////////////////////////////

configType = 'michael'

# configuration variables - differ on each system
if configType == 'GroG':
  # GroG's config
  panServoPin = 9
  tiltServoPin = 10
  comPort = 'COM9'
  cameraIndex = 1
else:
  # michael's config
  panServoPin = 2
  tiltServoPin = 3
  comPort = 'COM7'
  cameraIndex = 0


# ///////////////////PID/////////////////////////////////////////////////////
# 2 PID services to adjust for X & Y of the open cv point and to map them
# to the pan and tilt servo values
# X : opencv 0 - 320 ->  pan 0 - 180
# Y : opencv 0 - 240 -> tilt 0 - 180

# xpid = Runtime.createAndStart("xpid","PID")
# ypid = Runtime.createAndStart("ypid","PID")

# xpid.setInputRange(0, 320)
# xpid.setOutputRange(0, 180)
# xpid.setPoint(160) # we want the target in the middle of the x

# ypid.setInputRange(0, 240)
# ypid.setOutputRange(0, 180)
# xpid.setPoint(160) # and the middle of the y


# ///////////////////ARDUINO/////////////////////////////////////////////////////

from time import sleep
from org.myrobotlab.service import Arduino

arduino = runtime.createAndStart('arduino','Arduino')

# set and open the serial device 
# arduino.connect('/dev/ttyUSB0', 57600, 8, 1, 0)
arduino.connect(comPort, 57600, 8, 1, 0)

sleep(3) # sleep because even after initialization the serial port still takes time to be ready
arduino.pinMode(16, Arduino.INPUT)
# arduino.digitalReadPollingStop(7)
arduino.analogReadPollingStart(16) # A2
sleep(1)
arduino.analogReadPollingStop(16)

# //////////////SERVOS////////////////////////////////////////////
from org.myrobotlab.service import Servo

pan = runtime.createAndStart('pan','Servo')
tilt = runtime.createAndStart('tilt','Servo')

# attach the pan servo to the Arduino on pin 2
# pan.attach(arduino.getName(),panServoPin) 
arduino.servoAttach(pan.getName(),panServoPin)
# attach the pan servo to the Arduino on pin 3
#tilt.attach(arduino.getName(),tiltServoPin) 
arduino.servoAttach(tilt.getName(),tiltServoPin)

pan = runtime.createAndStart('pan','Servo')
tilt = runtime.createAndStart('tilt','Servo')

# head shake 3 times and nod 3 times - checks arduino
# and servo connectivity
for pos in range(0,3):
  pan.moveTo(70)
  pan.moveTo(100)
  sleep(0.5)

pan.moveTo(90)

for pos in range(0,3):
  tilt.moveTo(70)
  tilt.moveTo(100)
  sleep(0.5)

tilt.moveTo(90)


# //////////OPENCV////////////////////////////////////////

from java.lang import String
from java.lang import Class
from org.myrobotlab.service import Runtime
from org.myrobotlab.service import OpenCV
from com.googlecode.javacv.cpp.opencv_core import CvPoint;
from org.myrobotlab.service import OpenCV

# create or get a handle to an OpenCV service
opencv = Runtime.createAndStart("opencv","OpenCV")

# add the desired filters
opencv.addFilter("PyramidDown1", "PyramidDown")
opencv.addFilter("InRange1", "InRange")
opencv.addFilter("Dilate1", "Dilate")
opencv.addFilter("FindContours1", "FindContours")

if configType == 'GroG':
  # GroG is looking for a purple balloon 
  opencv.setFilterCFG("InRange1","hueMin", "30")
  opencv.setFilterCFG("InRange1","hueMax", "54")
  opencv.setFilterCFG("InRange1","saturationMin", "70")
  opencv.setFilterCFG("InRange1","saturationMax", "241")
  opencv.setFilterCFG("InRange1","valueMin", "56")
  opencv.setFilterCFG("InRange1","valueMax", "89")
  opencv.setFilterCFG("InRange1","useHue", True)
  opencv.setFilterCFG("InRange1","useSaturation", True)
  opencv.setFilterCFG("InRange1","useValue", True)
else:
  # michael is looking for something else
  opencv.setFilterCFG("InRange1","hueMin", "3")
  opencv.setFilterCFG("InRange1","hueMax", "33")
  opencv.setFilterCFG("InRange1","saturationMin", "87")
  opencv.setFilterCFG("InRange1","saturationMax", "256")
  opencv.setFilterCFG("InRange1","valueMin", "230")
  opencv.setFilterCFG("InRange1","valueMax", "256")
  opencv.setFilterCFG("InRange1","useHue", True)
  opencv.setFilterCFG("InRange1","useSaturation", True)
  opencv.setFilterCFG("InRange1","useValue", True)


# change value of the FindContours filter
# opencv.setFilterCFG("FindContours1","minArea", "150")
# opencv.setFilterCFG("FindContours1","maxArea", "-1")
# opencv.setFilterCFG("FindContours1","useMinArea", True)
# opencv.setFilterCFG("FindContours1","useMaxArea", false)

# ----------------------------------
# input
# ----------------------------------
# the "input" method is where Messages are sent to this Service
# from other Services. The data from these messages can
# be accessed on based on these rules:
# Details of a Message structure can be found here
# http://myrobotlab.org/doc/org/myrobotlab/framework/Message.html 
# When a message comes in - the input function will be called
# the name of the message will be msg_++_+
# In this particular case when the service named "opencv" finds a face it will publish
# a CvPoint.  The CvPoint can be access by msg_opencv_publish.data[0]

sampleCount = 0
xAvg = 0
yAvg = 0
pixelsPerDegree = 5

def input():
  global sampleCount
  global xAvg 
  global yAvg 
  arrayOfPolygons = msg_opencv_publish.data[0]
  if (arrayOfPolygons.size() > 0):
    # grab the first polygon - print it's center (x,y)
    x = arrayOfPolygons.get(0).centeroid.x()
    y = arrayOfPolygons.get(0).centeroid.y()
    print x,y
    # figure out how far off from the center of the view is this point
    panOffset = (320/2 - x) / pixelsPerDegree # (screenWidth/2 -x) / pixelsPerDegree
    tiltOffset = (240/2 - y) / pixelsPerDegree # (screenWidth/2 -y) / pixelsPerDegree
    if (sampleCount > 10):
      pan.moveTo(90 + xAvg/sampleCount)
      tilt.moveTo(90 + yAvg/sampleCount)
      xAvg = 0
      yAvg = 0
      sampleCount = 0

    sampleCount += 1
    xAvg += panOffset
    yAvg += tiltOffset
    print xAvg, yAvg, sampleCount
   
  return object

# create a message route from opencv to python so we can see the coordinate locations
opencv.addListener("publish", python.name, "input", CvPoint().getClass()); 

opencv.setCameraIndex(cameraIndex)

# set the input source to the first camera
opencv.capture()



# ////////////////////END PYTHON  SCRIPT/////////////////////////////////////////