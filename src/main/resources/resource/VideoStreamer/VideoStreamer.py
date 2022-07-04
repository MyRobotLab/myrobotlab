from org.myrobotlab.service import Runtime
from org.myrobotlab.service import OpenCV
from org.myrobotlab.service import VideoStreamer
from time import sleep
from org.myrobotlab.net import BareBonesBrowserLaunch

# create a video source (opencv) & a video streamer
opencv = runtime.start("opencv","OpenCV")
streamer = runtime.start("streamer","VideoStreamer")

# attache them
streamer.attach(opencv)

# add a pyramid down filter and gray to minimize the data
opencv.addFilter("pyramidDown", "PyramidDown");
opencv.addFilter("gray", "Gray");

# start the camera
opencv.capture();
#added sleep in order to give opencv the time to "warm up" the cam
sleep(3)


# go to http://localhost:9090/output
BareBonesBrowserLaunch.openURL("http://localhost:9090")
