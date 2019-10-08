#########################################
# OpenNi.py
# more info @: http://myrobotlab.org/service/OpenNi
#########################################

# very minimal script to start

openni = Runtime.start("openni", "OpenNi")
openni.startUserTracking()
