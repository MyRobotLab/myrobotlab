from java.lang import String
from java.lang import Class
from java.awt import Rectangle
from org.myrobotlab.service import Runtime
from org.myrobotlab.service import OpenCV
from org.myrobotlab.opencv import OpenCVData
from com.googlecode.javacv.cpp.opencv_core import CvPoint;

global posx
global posy
global newx
global newy

opencv = Runtime.createAndStart("opencv","OpenCV")
opencv.publishState()
opencv.addFilter("pd","PyramidDown")
opencv.setDisplayFilter("pd")
opencv.addFilter("detector","Detector")
opencv.setDisplayFilter("detector")
opencv.addFilter("findcontours","FindContours")
opencv.setDisplayFilter("findcontours")

tracker = Runtime.create("tracker","Tracking")
 
# create all the peer services
rotation = Runtime.create("rotation","Servo")
neck = Runtime.create("neck","Servo")
arduino = Runtime.create("arduino","Arduino")
xpid = Runtime.create("xpid","PID");
ypid = Runtime.create("ypid","PID");
 
# adjust values
arduino.connect("COM3")
eye = Runtime.create("eye","OpenCV")
eye.publishState()
eye.addFilter("lk","LKOpticalTrack")
eye.setDisplayFilter("lk")
eye.setInputSource("pipeline")
eye.setPipeline("opencv.input")
eye.publishState()

xpid.setOutputRange(-3, 3)
xpid.setSetpoint(0.5) # we want the target in the middle of the x

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
eye.capture()

tracker.attachPIDs(xpid, ypid)
tracker.xpid.setPID(10.0, 0, 0.1)
tracker.ypid.setPID(10.0, 0, 0.1)
# flip the pid if needed
tracker.xpid.invert()
tracker.ypid.invert()
 
tracker.setRestPosition(90, 72)
 
tracker.startService()
tracker.stopVideoStream()
tracker.trackPoint(0.5,0.5)

def input():
 
    #print 'found face at (x,y) ', msg_opencv_publishOpenCVData.data[0].x(), msg_opencv_publish.data[0].y()
    opencvData = msg_opencv_publishOpenCVData.data[0]
    if (opencvData.getBoundingBoxArray() is not None and opencvData.getBoundingBoxArray().size() > 0) :
     rect = opencvData.getBoundingBoxArray().get(0)
     posx = rect.x
     posy = rect.y
     print ("X is: ",posx)
     print ("Y is: ", posy)
     newx = (posx/320.0)
     newy = (posy/240.0)
     tracker.trackPoint(newx,newy)
     return object
 
# create a message route from opencv to python so we can see the coordinate locations
opencv.addListener("publishOpenCVData", python.name, "input", OpenCVData().getClass()); 
 
# set the input source to the first camera
opencv.capture()