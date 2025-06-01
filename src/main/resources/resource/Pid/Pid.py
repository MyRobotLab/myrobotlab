#########################################
# Pid.py
# categories: pid
# more info @: http://myrobotlab.org/service/Pid
#########################################

import time
import random

pid = runtime.start("test", "Pid")

pid.setPid("pan", 1.0, 0.1, 0.0)
pid.setSetpoint("pan", 320)

pid.setPid("tilt", 1.0, 0.1, 0.0)
pid.setSetpoint("tilt", 240)

for i in range(0,200):

    i = random.randint(200,440)
    pid.compute("pan", i)
    
    i = random.randint(200,440)
    pid.compute("tilt", i)
    
    time.sleep(0.1)
