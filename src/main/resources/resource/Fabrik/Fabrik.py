#########################################
# Fabrik.py
# description: FABRIK inverse kinematics (Aristidou)
# categories: robot, control
# more info @: http://myrobotlab.org/service/Fabrik
#########################################

fabrik = runtime.start("fabrik", "Fabrik")
fabrik.setCurrentArm("left", "i01", "left")
fabrik.centerAllJoints()
print(fabrik.currentPosition("left"))
