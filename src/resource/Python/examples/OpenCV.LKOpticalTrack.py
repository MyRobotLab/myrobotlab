# a small script which shows some of the possiblities of the
# LKOpticalTrack filter
# LKOpitcal is good low resource algorithm which is good at tracking points through a video
# stream

from org.myrobotlab.opencv import OpenCVFilterLKOpticalTrack

# create services
opencv = Runtime.createAndStart("opencv","OpenCV")

# add listener so data comes back to python
opencv.addListener("publishOpenCVData", "python", "input")

lkfilter = opencv.getFilter("LKOpticalTrack")
if (lkfilter == None):
  lkfilter = OpenCVFilterLKOpticalTrack()
  opencv.addFilter(lkfilter)

# other options

# if you want to get pixel values instead of floats
# floats are nice because the value doesnt change even if the
# resolution does
# lkfilter.useFloatValues=False # default is true
# lkfilter.needTrackingPoints=True #default is false
lkfilter.samplePoint(0.5,0.5)# programmatically sets a point


# a set of points can come back from LKOptical
def input ():
  points = msg_opencv_publishOpenCVData.data[0].getPoints()
  if  (not points == None):
    print points
    if (points.size() > 0):
      print points.get(0).x, points.get(0).y
    
opencv.capture()
