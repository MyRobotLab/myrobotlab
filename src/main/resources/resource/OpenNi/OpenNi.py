#########################################
# OpenNi.py
# more info @: http://myrobotlab.org/service/OpenNi
#########################################

# very minimal script to start

openni = Runtime.start("openni", "OpenNi")
python = Runtime.getService("python")

# define a method for your callback..  here it passed the full openni data object that contains the skeleton info.
def onOpenNIData(data):
  # python handy method for printing all members on an object.
  print "Full data object"
  print dir(data)
  print "Skeleton object"
  print dir(data.skeleton)  
  # example of printout of the head x,y,z coordinates.
  print str(data.skeleton.head.x) + " " + str(data.skeleton.head.y) + " " + str(data.skeleton.head.z)

# attach to the callback in python for the data
openni.addOpenNIData(python)

openni.startUserTracking()

