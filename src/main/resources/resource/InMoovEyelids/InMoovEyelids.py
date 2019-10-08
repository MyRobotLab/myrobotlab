#########################################
# InMoovEyelids.py
# description: InMoov Eyelids
# categories: robot
# more info @: http://myrobotlab.org/service/InMoovEyelids
#########################################

# This example shows how to use the eyelids service
# It can be used with any compatible servo controller ( Arduino / Adafruit16CServoDriver ... )
# It you have only 1 servo for 2 eyelids, just set eyelidRightPin to fake pin


# start the service
inmooveyelids = Runtime.start('inmooveyelids','InMoovEyelids')
eyelidLeftPin=2
eyelidRightPin=3

# Code to be able to use this script with virtalArduino
# virtual = True
if ('virtual' in globals() and virtual):
    virtualArduino = Runtime.start("virtualArduino", "VirtualArduino")
    virtualArduino.connect("COM3")
    
# RASPI
# Uncomment this line below if you use the RasPi
# raspi = Runtime.createAndStart("raspi","RasPi")

# ARDUINO 
# Uncomment this lines below if you use ARDUINO
arduino = Runtime.start("arduino","Arduino")
arduino.connect("COM3")

# ADAFRUIT16C
# You can attach Adafruit16CServoDriver to arduino OR raspi
# Uncomment this line below if you use the Adafruit16CServoDriver
# adafruit16CServoDriver = Runtime.start("adafruit16CServoDriver","Adafruit16CServoDriver")
#
# Choose :
# adafruit16CServoDriver.attach("arduino","0","0x40")
# adafruit16CServoDriver.attach("raspi","1","0x40")

# Now attach eyelids service to the choosen servo controler
inmooveyelids.attach(arduino,eyelidLeftPin,eyelidRightPin)
# inmooveyelids.attach(adafruit16CServoDriver,eyelidLeftPin,eyelidRightPin)

# Set it to True for auto power off servos
inmooveyelids.setAutoDisable(False)

# servos limits
inmooveyelids.eyelidleft.map(0,180,20,100)
inmooveyelids.eyelidright.map(0,180,20,100)
inmooveyelids.eyelidright.setInverted(True)

inmooveyelids.blink()
sleep(2)
inmooveyelids.blink()
sleep(2)

inmooveyelids.autoBlink(True)
sleep(10)
inmooveyelids.autoBlink(False)