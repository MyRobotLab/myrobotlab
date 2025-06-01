#########################################
# MotorDualPwm.py
# description: Motor service which support 2 pwr pwm pins clockwise and counterclockwise
# categories: motor
# more info @: http://myrobotlab.org/service/MotorDualPwm
#########################################
# Config
port="COM3"
# start optional virtual arduino service, used for test
if ('virtual' in globals() and virtual):
    virtualArduino = runtime.start("virtualArduino", "VirtualArduino")
    virtualArduino.connect(port)
arduino = runtime.start("arduino","Arduino")
motor = runtime.start("motor","MotorDualPwm")
arduino.connect(port)
motor.setPwmPins(10,11)
motor.attach(arduino)
# At this point you should be able to control the motor thru the Gui
# move both motors forward
# at 50% power
# for 2 seconds
motor.move(0.5)
sleep(2) 
# move both motors backward
# at 50% power
# for 2 seconds
motor.move(-0.5)
sleep(2)
# stop and lock
motor.stopAndLock()
# after locking
# motor should not move
motor.move(0.5)
sleep(2)
# unlock motor and move it
motor.unlock()
motor.move(0.5)
sleep(2)
motor.stop()
