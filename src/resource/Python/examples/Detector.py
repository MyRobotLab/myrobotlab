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
     return object
 
# create a message route from opencv to python so we can see the coordinate locations
opencv.addListener("publishOpenCVData", python.name, "input", OpenCVData().getClass()); 
 
# set the input source to the first camera
opencv.capture()