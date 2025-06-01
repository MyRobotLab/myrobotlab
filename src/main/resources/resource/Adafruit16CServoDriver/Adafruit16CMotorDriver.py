#########################################
# Adafruit16CServoDriver.py
# description: servo driver
# categories: servo shield
# more info @: http://myrobotlab.org/service/Adafruit16CServoDriver
#########################################
# From version 1.0.2316 use attach instead of setController
# An example of how to use the Adafruit16CServoDriver to 
# drive a motor.
#
# config
port = "COM99"

# runtime.start("WebGui", "WebGui");
arduino = runtime.start("Arduino", "Arduino");
arduino.connect(port)
sleep(5)
ada = runtime.start("Ada","Adafruit16CServoDriver")

# ada.setController(arduino,"1","0x40")
ada.attach(arduino,"1","0x40")
sleep(2)
motor01 = runtime.start("motor01", "Motor");
motor01.setPwmPins(0,1);
motor01.attach(ada);
motor01.move(0.3);
