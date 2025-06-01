from org.myrobotlab.image import Util
# start a opencv service

opencv = runtime.start("opencv","OpenCV")
#gui.setActiveTab("opencv")

# add python as a listener to OpenCV data
# this tells the framework - whenever opencv.publishOpenCVData is invoked
# python.onOpenCVData will get called
python = runtime.start("python","Python")
python.subscribe("opencv", "publishOpenCVData")


# call back - all data from opencv will come back to 
# this method
def onOpenCVData(data):
  # check for a bounding box
  if data.getBoundingBoxArray() != None:
    for box in data.getBoundingBoxArray():
      print("bounding box", box.x, box.y, box.width, box.height)

# to capture from an image on the file system
# opencv.captureFromImageFile("C:\Users\grperry\Desktop\mars.jpg")

# not for you, it's for test
if ('virtual' in globals() and virtual):
  opencv.setMinDelay(500)
  opencv.setFrameGrabberType("org.bytedeco.javacv.FFmpegFrameGrabber")
  opencv.setInputSource("file")
  opencv.setInputFileName(Util.getRessourceDir()+"OpenCV/testData/monkeyFace.mp4")

opencv.capture()

#### LKOpticalTrack ####################
# experiment with Lucas Kanade optical flow/tracking
# adds the filter and one tracking point

opencv.addFilter("LKOpticalTrack")
opencv.setDisplayFilter("LKOpticalTrack")
# attempt to set a sample point in the middle 
# of the video stream - you can 
opencv.invokeFilterMethod("LKOpticalTrack","samplePoint", 0.5, 0.5)
sleep(4)
opencv.removeFilters()

opencv.addFilter("FaceDetect")
opencv.setDisplayFilter("FaceDetect")
# attempt to set a sample point in the middle 
# of the video stream - you can 

sleep(4)
opencv.removeFilters()
 

#### PyramidDown ####################
# scale the view down - faster since updating the screen is 
# relatively slow
opencv.addFilter("PyramidDown")
opencv.setDisplayFilter("PyramidDown")
sleep(4)
# adding a second pyramid down filter - we need
# a unique name - so we'll call it PyramidDown2
opencv.addFilter("PyramidDown2","PyramidDown")
opencv.setDisplayFilter("PyramidDown2")
sleep(4)
opencv.removeFilters()


#### Canny ########################
# adding a canny filter
opencv.addFilter("Canny")
opencv.setDisplayFilter("Canny")
sleep(4)
canny = opencv.getFilter("Canny") 
# changing parameters
canny.apertureSize = 3
canny.lowThreshold = 10.0
canny.highThreshold = 200.0

sleep(2)

canny.apertureSize = 5
canny.lowThreshold = 10.0
canny.highThreshold = 100.0

sleep(4)
opencv.removeFilters()
opencv.stopCapture()