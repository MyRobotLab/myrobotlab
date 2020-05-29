#########################################
# OpenCV01_stop.py
# categories: opencv
# more info @: http://myrobotlab.org/service/OpenCV
#########################################

# release the opencv service
Runtime.releaseService("opencv")

# we tell to the service what is going on
# intro.isServoActivated = False ## FIXME this gives error readonly 
intro.broadcastState()