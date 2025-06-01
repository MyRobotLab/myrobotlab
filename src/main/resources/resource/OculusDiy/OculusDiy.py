#########################################
# OculusDiy.py
# more info @: http://myrobotlab.org/service/OculusDiy
#########################################
# start the service
oculusdiy = runtime.start("oculusdiy","OculusDiy")

port="COM3"

#virtual=1
if ('virtual' in globals() and virtual):
    virtualArduino = runtime.start("virtualArduino", "VirtualArduino")
    virtualArduino.connect(port)
    
oculusdiy.connect(port)
oculusdiy.mpu6050.attach(oculusdiy.arduino, "0", "0x68")