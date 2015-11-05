from com.leapmotion.leap.Finger import Type
# from __future__ import division

leap = Runtime.createAndStart("leap","LeapMotion")


leap.addListener("publishLeapData", "python", "onLeapData"); 
# publishLeapData
# leap.addFrameListener(python)

def onLeapData(leapdata):
  # print "Hello world"
  print leapdata.leftHand.posX
  print leapdata.leftHand.posY
  print leapdata.leftHand.posZ

leap.startTracking()