#########################################
# Servo01_stop.py
# categories: intro
# more info @: http://myrobotlab.org/service/Intro
#########################################
# uncomment for virtual hardware
# Platform.setVirtual(True)

# Every settings like limits / port number / controller are saved after initial use
# so you can share them between differents script 

# servoPin01 = 4

# port = "/dev/ttyUSB0"
# port = "COM15"

# release a servo controller and a servo
Runtime.releaseService("arduino")
Runtime.releaseService("servo01")

# we tell to the service what is going on
# intro.isServoActivated = False ## FIXME this gives error readonly 
intro.broadcastState()


