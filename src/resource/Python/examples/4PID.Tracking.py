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

global servox
global servoy
global actservox
global actservoy

actservox = 90
actservoy = 90
 
tracker = Runtime.create("tracker","Tracking")
 
# create all the peer services
rotation = Runtime.create("rotation","Servo")
neck = Runtime.create("neck","Servo")
arduino = Runtime.create("arduino","Arduino")
xpid = Runtime.create("xpid","PID");
ypid = Runtime.create("ypid","PID");

rotationb = Runtime.create("rotationb","Servo")
neckb = Runtime.create("neckb","Servo")
xpidb = Runtime.create("xpidb","PID");
ypidb = Runtime.create("ypidb","PID");
 
# adjust values
arduino.connect("COM3")
eye = Runtime.create("eye","OpenCV")
eye.setCameraIndex(1)
 
#i'm wondering if these are working??? they are by default...
xpid.setOutputRange(-3, 3)
xpid.setSetpoint(0.5)
ypid.setOutputRange(-3, 3)
ypid.setSetpoint(0.5)


 
# set safety limits - servos
# will not go beyond these limits
rotation.setMinMax(50,170)
 
neck.setMinMax(50,170)
 
# here we are attaching to the
# manually created peer services
 
tracker.attach(arduino)
tracker.attachServos(rotation, 3, neck, 9)
tracker.attach(eye)
tracker.attachPIDs(xpid, ypid)
tracker.xpid.setPID(10.0, 0, 0.1)
tracker.ypid.setPID(10.0, 0, 0.1)
xpidb.setMode(1)
xpidb.setPID(8, 0, 0) # head is less responsive than eye, so 8 instead of 10 and 0 instead of 0.1
ypidb.setMode(1)
ypidb.setPID(8, 0, 0) # head is less responsive than eye, so 8 instead of 10 and 0 instead of 0.1
xpidb.setControllerDirection(1)
ypidb.setControllerDirection(1)
xpidb.setOutputRange(-1, 1) #the output is of 1, instead of 3 because the head moves less fast than eye
xpidb.setSetpoint(90) #90 because InMoov's eye must be centered while is tracking, the head moves to make it possible

ypidb.setOutputRange(-1, 1) #the output is of 1, instead of 3 because the head moves less fast than eye
ypidb.setSetpoint(90) #90 because InMoov's eye must be centered while is tracking, the head moves to make it possible
# flip the pid if needed
tracker.xpid.invert()
tracker.ypid.invert()

tracker.setRestPosition(90, 90)
arduino.attach(rotationb.getName() , 6)
arduino.attach(neckb.getName(), 12)
rotationb.moveTo(90)
neckb.moveTo(90)
 
tracker.startService()
tracker.trackPoint(0.5,0.5)

for i in range(10000):
     global posx
     global posy
     global newx
     global newy
     global movex
     global movey
     servox = rotation.getPosition()
     servoy = neck.getPosition()
     print servox
     print servoy
     xpidb.setInput(servox)
     xpidb.compute()
     ypidb.setInput(servoy)
     ypidb.compute()
     valx = xpidb.getOutput()
     valy = ypidb.getOutput()
     global actservox
     movex = (actservox + valx)
     actservox = movex
     global actservoy
     movey = (actservoy + valy)
     actservoy = movey
     print 'x servo' , int(actservox)
     print 'y servo' , int(actservoy)
     rotationb.moveTo(int(actservox))
     neckb.moveTo(int(actservoy))