from java.lang import String
from java.lang import Class
from java.awt import Rectangle
from org.myrobotlab.service import Runtime
from org.myrobotlab.service import OpenCV
from org.myrobotlab.opencv import OpenCVData
from com.googlecode.javacv.cpp.opencv_core import CvPoint;
from org.myrobotlab.service import OpenCV
from org.myrobotlab.service import Arduino
from org.myrobotlab.service import Servo
from time import sleep
 
 
xpid = Runtime.createAndStart("xpid","PID")
ypid = Runtime.createAndStart("ypid","PID")
xpid.setMode(1)
xpid.setOutputRange(-1, 1)
xpid.setPID(5.0, 0.0, 1.0)
xpid.setControllerDirection(0)
xpid.setSetpoint(80)  #setpoint now is 80 instead of 160 because of 2 PD filters
ypid.setMode(1)
ypid.setOutputRange(-1, 1)
ypid.setPID(5.0, 0.0, 1.0)
ypid.setControllerDirection(0)
ypid.setSetpoint(60) #set point is now 60 instead of 120 because of 2 PD filters
xpid.invert()
#ypid.invert()
arduino = Runtime.createAndStart("arduino","Arduino")
speech= Runtime.createAndStart("speech","Speech")
pan  = Runtime.createAndStart("pan","Servo")
tilt = Runtime.createAndStart("tilt","Servo")
arduino.connect("/dev/ttyACM0", 57600, 8, 1, 0)
sleep(4)
arduino.attach(pan.getName() , 14)
arduino.attach(tilt.getName(), 15)
 
global actservox
global actservoy
actservox = 90
actservoy = 90
pan.moveTo(actservox)
tilt.moveTo(actservoy)
 
# create or get a handle to an OpenCV service
opencv = Runtime.create("opencv","OpenCV")
opencv.startService()
# reduce the size - face tracking doesn't need much detail
# the smaller the faster
opencv.addFilter("Gray", "Gray")
opencv.addFilter("PyramidDown2", "PyramidDown")
opencv.addFilter("PyramidDown1", "PyramidDown")
#opencv.addFilter("PyramidUp1", "PyramidUp")
 
# add the face detect filter
opencv.addFilter("FaceDetect1", "FaceDetect")
dirx=2
diry=20
frameSkip=0
frameSkipHuman=0
spokeScan=False;
spokeSearch=False;
def input():
 
    #print 'found face at (x,y) ', msg_opencv_publishOpenCVData.data[0].x(), msg_opencv_publish.data[0].y()
    opencvData = msg_opencv_publishOpenCVData.data[0]
    global x
    global y
    global sposx
    global sposy
    global posx
    global posy
    global servox
    global servoy
    global movex
    global movey
    global dirx
    global diry
    global frameSkip
    global frameSkipHuman
    global spokeSearch
    global spokeScan
    if (opencvData.getBoundingBoxArray().size() > 0) :
     if not spokeScan:
      speech.speak("hello")
      spokeScan=True
      spokeSearch=False
     frameSkip=0
     if (frameSkipHuman<50):
       arduino.digitalWrite(10,1)
       arduino.digitalWrite(9,1)
       arduino.digitalWrite(4,0)
     frameSkipHuman+=1
     if frameSkipHuman==51:
       speech.speak("eliminating");
       arduino.digitalWrite(10,1);
       arduino.digitalWrite(9,0);
       arduino.digitalWrite(4,1);
       
     rect = opencvData.getBoundingBoxArray().get(0)
     posx = rect.x
     posy = rect.y
     
     w = rect.width
     h = rect.height
     sposx = (w/2)
     sposy = (h/2)
     x = (posx + sposx)
     y = (posy + sposy)
     print x
     print y
     xpid.setInput(x)
     xpid.compute()
     servox = xpid.getOutput()
     movex = (actservox + servox)
     global actservox
     actservox = movex
     ypid.setInput(y)
     ypid.compute()
     servoy = ypid.getOutput()
     global actservoy
     movey = (actservoy + servoy)
     actservoy = movey
     
     print 'x servo' , int(actservox)
     print 'y servo' , int(actservoy)
     pan.moveTo(int(actservox))
     tilt.moveTo(int(actservoy))
 
     return object
    else:
     frameSkipHuman=0;
     frameSkip+=1
     if frameSkip>20:
       if not spokeSearch:
        speech.speak("searching")
        spokeScan=False
        spokeSearch=True
       actservox+=dirx
       if (actservox<11 or actservox>169):
         dirx=-dirx
         while (actservox<11 or actservox>169):actservox+=dirx
         actservoy+=diry
         if actservoy<81 or actservoy>159:
          diry=-diry
          while (actservoy<81 or actservoy>159):actservoy+=diry
 
       pan.moveTo(int(actservox))
       tilt.moveTo(int(actservoy))
       arduino.digitalWrite(10,0)
       arduino.digitalWrite(9,1)
       arduino.digitalWrite(4,1)
       x=actservox;
       y=actservoy;
       return object
     
       
 
# create a message route from opencv to python so we can see the coordinate locations
opencv.addListener("publishOpenCVData", python.name, "input", OpenCVData().getClass());
 
opencv.setCameraIndex(1)
 
# set the input source to the first camera
opencv.capture()