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

# system specif variables
actservox = 90
actservoy = 90

xpid = Runtime.createAndStart("xpid","PID")
ypid = Runtime.createAndStart("ypid","PID")
xpid.setMode(1)
xpid.setOutputRange(-3, 3)
xpid.setPID(10.0, 0, 1.0)
xpid.setControllerDirection(1)
xpid.setSetpoint(160) # we want the target in the middle of the x
ypid.setMode(1)
ypid.setOutputRange(-3, 3)
ypid.setPID(10.0, 0, 1.0)
ypid.setControllerDirection(1)
ypid.setSetpoint(120)

arduino = Runtime.createAndStart("arduino","Arduino")
pan 	= Runtime.createAndStart("pan","Servo")
tilt	= Runtime.createAndStart("tilt","Servo")
arduino.connect("COM3", 57600, 8, 1, 0)

arduino.attach(pan.getName() , 12)
arduino.attach(tilt.getName(), 13)
global actservox
global actservoy


# create or get a handle to an OpenCV service
opencv = Runtime.create("opencv","OpenCV")
opencv.startService()
# reduce the size - face tracking doesn't need much detail
# the smaller the faster
opencv.addFilter("PyramidDown1", "PyramidDown")
# add the face detect filter
opencv.addFilter("FaceDetect1", "FaceDetect")
 
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
    if (opencvData.getBoundingBoxArray().size() > 0) :
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

# create a message route from opencv to python so we can see the coordinate locations
opencv.addListener("publishOpenCVData", python.name, "input");

# opencv.setCameraIndex(1)

opencv.capture()
