#########################################
# BoofCV.py
# description: used as a general template
# categories: [general]
# possibly more info @: http://myrobotlab.org/service/BoofCV
#########################################
# start the service
boofcv = Runtime.start("boofcv","BoofCv")

# create a tracker - source is webcame 
# jframe is displayed
# your supposed to select with mouse the area you
# want to track
tracker = boofcv.createTracker()
tracker.start()
sleep(10)
# stop tracker
tracker.stop()
