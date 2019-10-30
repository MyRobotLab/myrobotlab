#########################################
# Motor.py
# categories: motor
# more info @: http://myrobotlab.org/service/Motor
#########################################
# uncomment for virtual hardware
virtual = True

# demonstrates the basic motor api
# an Arduino is used as a motor controller
# this dc motor has a simple h-bridge
# 1 pin controls power/speed with pulse width modulation
# the other controls direction

port = "COM99"

# start the services
arduino = Runtime.start("arduino", "Arduino")
m1 = Runtime.start("m1","Motor")
m1.setPwrPin(3)
m1.setDirPin(4)

# start optional virtual arduino service, used for test
if ('virtual' in globals() and virtual):
    virtualArduino = Runtime.start("virtualArduino", "VirtualArduino")
    virtualArduino.connect(port)

# connect the Arduino - (our motor controller)
arduino.connect(port)

# connect motor m1 with pwm power pin 3, direction pin 4
arduino.attach(m1)

# move both motors forward
# at 50% power
# for 2 seconds
m1.move(0.5)

sleep(2)

# move both motors backward
# at 50% power
# for 2 seconds
m1.move(-0.5)

sleep(2)

# stop and lock m1
m1.stopAndLock()

# after locking
# m1 should not move
m1.move(0.5)

sleep(2)

# unlock m1 and move it
m1.unlock()
m1.move(0.5)

sleep(2)

m1.stop()
