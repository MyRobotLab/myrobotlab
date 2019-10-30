#########################################
# OculusDiy.py
# more info @: http://myrobotlab.org/service/OculusDiy
#########################################
# start the service
oculusdiy = Runtime.start("oculusdiy","OculusDiy")

port="COM3"

#virtual=1
if ('virtual' in globals() and virtual):
    virtualArduino = Runtime.start("virtualArduino", "VirtualArduino")
    virtualArduino.connect(port)
    
oculusdiy.connect(port)
oculusdiy.mpu6050.attach(oculusdiy.arduino, "0", "0x68")