#########################################
# virtualServo01_start.py
# description: used as a general template
# categories: simulator
# more info @: http://myrobotlab.org/service/JMonkeyEngine
#########################################

from org.myrobotlab.framework import Service
from org.myrobotlab.service import Intro

# make whole system virtual
# doing it with this function makes all service virtual
# and any newly created ones will be virtual too
Runtime.setAllVirtual(True)

# start the service
simulator = runtime.start('simulator','JMonkeyEngine')
# we load the model from path
simulator.loadModels(Service.getResourceDir('Intro','JMonkeyEngine/assets'))
# we set the rotation axe to the defined part
simulator.setRotation("servo01", "y")
# we do our mapping to the part
simulator.setMapper("servo01", 0, 180, -2, -171)
# we set our view on another part
simulator.cameraLookAt("servo")
#simulator.getNode("camera").move(3, 1, 4) # relative local axes are x,y,z
simulator.moveTo("camera", 0.95, 1.2, 1.4) # relative absolute axes are x,y,z
simulator.rotateOnAxis("camera", "y", 220)

# simulator.setDefaultServoSpeed(50.0)
