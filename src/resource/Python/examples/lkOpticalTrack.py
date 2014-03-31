# lkOpticalTrack.py
# to experiment with Lucas Kanade optical flow/tracking
# starts opencv and adds the filter and one tracking point

from jarray import zeros, array
import time

opencv = runtime.createAndStart("opencv","OpenCV")

# scale the view down - faster since updating the screen is 
# relatively slow
opencv.addFilter("pyramidDown1","PyramidDown")

# add out LKOpticalTrack filter
opencv.addFilter("lkOpticalTrack1","LKOpticalTrack")

# begin capturing
opencv.capture()

# set focus on the lkOpticalTrack filter
opencv.setDisplayFilter("lkOpticalTrack1")

# rest a second or 2 for video to stabalize
time.sleep(2)

# set a point in the middle of a 320 X 240 view screen
opencv.invokeFilterMethod("lkOpticalTrack1","samplePoint", 160, 120)
#opencv.stopCapture()
