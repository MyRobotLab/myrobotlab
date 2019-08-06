##############################################################
# MyRobotLab configuration file
# This file is generated from a running instance of MyRobotLab.
# It is meant to get MyRobotLab as close to that instance's state a possible.
# This file can be generated at any time using Runtime.save(filename)
# More information @ http://myrobotlab.org and https://github.com/myrobotlab
# version 1.1.86
# generated Thu Jul 18 21:06:14 PDT 2019

##############################################################
## imports ####
import org.myrobotlab.framework.Platform as Platform

# Platform.setVirtual(True)\n##############################################################
## creating and starting 6 services ####
# Although Runtime.start(name,type) both creates and starts services it might be desirable on creation to
# substitute peers, types or references of other sub services before the service is "started"
# e.g. i01 = Runtime.create('i01', 'InMoov') # this will "create" the service and config could be manipulated before starting 
# e.g. i01_left = Runtime.create('i01.left', 'Ssc32UsbServoController')
gui = Runtime.start('gui', 'SwingGui')
hobbyservo = Runtime.start('hobbyservo', 'HobbyServo')
mega = Runtime.start('mega', 'Arduino')
mega_serial = Runtime.start('mega.serial', 'Serial')
security = Runtime.start('security', 'Security')

##############################################################
## configuring services ####
# HobbyServo Config : hobbyservo
# sets initial position of servo before moving
# in theory this is the position of the servo when this file was created
hobbyservo.setPosition(113.0)
hobbyservo.setMinMax(0.0,180.0)
hobbyservo.setVelocity(2.0)
hobbyservo.setRest(90.0)
hobbyservo.setPin(22)
hobbyservo.map(0.0,180.0,0.0,180.0)

# Arduino Config : mega
# mega.setVirtual(True)
# we have the following ports : [COM7, COM3]
mega.connect("COM7")
# make sure the pins are set before attaching
hobbyservo.setPin("22")
mega.attach("hobbyservo")

