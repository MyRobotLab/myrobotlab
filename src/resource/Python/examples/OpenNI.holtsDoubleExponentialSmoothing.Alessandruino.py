from org.myrobotlab.openni import Skeleton

global t
t = 1
global a
global b
a = 0.25
b = 0.25

Runtime.createAndStart("openni","OpenNI")
openni.startUserTracking()
openni.addListener("publish",python.name,"input")


def input():
  global skeleton
  skeleton = msg_openni_publish.data[0]
  global oldSmoothed
  global oldTrend
  global actualSmoothed
  global actualTrend
  global a
  global b
  global t
  if (t ==1) :
    global oldSmoothed
    global oldTrend
    global t
    oldSmoothed = skeleton.rightHand.z
    oldTrend = 1
    t = 2
  elif (t==2) :
   global actualSmoothed
   global actualTrend
   global oldSmoothed
   global oldTrend
   global a
   global b
   actualSmoothed = ((a*(skeleton.rightHand.z))+((1-a)*(oldSmoothed + oldTrend)))
   actualTrend = ((b*(actualSmoothed - oldSmoothed))+((1 - b)*oldTrend))
   print actualSmoothed
   oldSmoothed = actualSmoothed
   oldTrend = actualTrend
