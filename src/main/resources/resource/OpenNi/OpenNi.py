############################################################################
# OpenNi.py
# This service is used with the Microsoft Kinect
# It provides depth map information as well as
# Skeleton tracking
#
# more info @: http://myrobotlab.org/service/OpenNi
############################################################################

# Start the OpenNi service
openni = runtime.start("openni", "OpenNi")
python = Runtime.getService("python")

# define a method for your callback..  here it passed the full openni data object that contains the skeleton info.
def onOpenNIData(data):
  # python handy method for printing all members on an object.
  print("Head Position x,y,z")
  # example of printout of the head x,y,z coordinates.
  print(str(data.skeleton.head.x) + " " + str(data.skeleton.head.y) + " " + str(data.skeleton.head.z))

# attach to the callback in python for the data
openni.addOpenNIData(python)

# Start the capture and tracking user skeletons
openni.startUserTracking()

