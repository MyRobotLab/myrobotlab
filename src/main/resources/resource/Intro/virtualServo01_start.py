#########################################
# virtualServo01_start.py
# description: used as a general template
# categories: simulator
# more info @: http://myrobotlab.org/service/JMonkeyEngine
#########################################

# start the service
virtualServo01 = Runtime.start('virtualServo01','JMonkeyEngine')
# we load the model from path
virtualServo01.loadModels('/resource/Intro/JMonkeyEngine/assets')
# we set the rotation axe to the defined part
virtualServo01.setRotation("servo01", "y")
# we do our mapping to the part
virtualServo01.setMapper("servo01", 0, 180, 2, -178)
# we set our view on another part
virtualServo01.cameraLookAt("servo")