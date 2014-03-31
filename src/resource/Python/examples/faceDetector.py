from java.lang import String
from java.lang import Class
from java.awt import Rectangle
from org.myrobotlab.service import Runtime
from org.myrobotlab.service import OpenCV
from org.myrobotlab.opencv import OpenCVData
from com.googlecode.javacv.cpp.opencv_core import CvPoint;
from org.myrobotlab.service import OpenCV

# create or get a handle to an OpenCV service
opencv = Runtime.create("opencv","OpenCV")
opencv.startService()
# reduce the size - face tracking doesn't need much detail
# the smaller the faster
opencv.addFilter("PyramidDown1", "PyramidDown")
# add the face detect filter
opencv.addFilter("FaceDetect1", "FaceDetect")

# ----------------------------------
# input
# ----------------------------------
# the "input" method is where Messages are sent to
# from other Services. The data from these messages can
# be accessed on based on these rules:
# Details of a Message structure can be found here
# http://myrobotlab.org/doc/org/myrobotlab/framework/Message.html 
# When a message comes in - the input function will be called
# the name of the message will be msg_+<sending service name>+_+<sending method name>
# In this particular case when the service named "opencv" finds a face it will publish
# a CvPoint.  The CvPoint can be access by msg_opencv_publish.data[0]
def input():

    #print 'found face at (x,y) ', msg_opencv_publishOpenCVData.data[0].x(), msg_opencv_publish.data[0].y()
    opencvData = msg_opencv_publishOpenCVData.data[0]
    print 'h1'
    if (opencvData.getBoundingBoxArray().size() > 0) :
    	rect = opencvData.getBoundingBoxArray().get(0)
    	print 'face found in box ', rect.x, rect.y, rect.width, rect.height
    	Rectangle 
    	print 'hello!'
    	return object

# create a message route from opencv to python so we can see the coordinate locations
opencv.addListener("publishOpenCVData", python.name, "input", OpenCVData().getClass()); 

# set the input source to the first camera
opencv.capture()




