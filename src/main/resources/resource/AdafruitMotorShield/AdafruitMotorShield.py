#########################################
# AdafruitMotorShield.py
# description: motor control
# categories: motor control
# more info @: http://myrobotlab.org/service/AdafruitMotorShield
#########################################
# virtual = True
port = "COM99"

# virtual hardware
if ('virtual' in globals() and virtual):
    virtualArduino = Runtime.start("virtualArduino", "VirtualArduino")
    virtualArduino.connect(port)

# The AFMotor API is supported through Jython
fruity = Runtime.createAndStart("fruity","AdafruitMotorShield")

# connect the arduino to serial com port 3
fruity.connect(port)

# create a motor on port 4 of the AdaFruit board
motor1 = fruity.createDCMotor(4)

# move forward at 40% power
motor1.move(0.4)

sleep(1)

# move reverse at 50% power
motor1.move(-0.5)

sleep(1)

# stops motor
motor1.stop()

# stops motor and locks it so it can not
# be moved until it is unlocked
# motor1.stopAndLock()

# unlocks motor
# motor1.unlock()

# sets max power regardles of move command
# this will allow the motor to go at max 90%
# full power forward or reverse
# motor1.setMaxPower(0.9)
