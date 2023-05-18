#################################################################
#                                                               #
# Program Code for Spot Micro MRL                               #
# Of the Cyber_One YouTube Channel                              #
# https://www.youtube.com/cyber_one                             #
#                                                               #
# This is version 0.2                                           #
# Divided up into sub programs                                  #
# Coded for the Nixie Version of MyRobotLab.                    #
#                                                               #
# Running on MyRobotLab (MRL) http://myrobotlab.org/            #
# Spot Micro MRL is a set of Python scripts that run inside     #
# the MRL system                                                #
#                                                               #
#                                                               #
#################################################################
import time

#################################################################
# Because you might want to place your robots files into a      #
# different dicrectory compared to what I have,                 #
# the RunningFolder variable is the name of the folder you      #
# will be using.                                                #
#################################################################
RuningFolder="data/Spot"

#################################################################
# This is the name the robot will use in some sections of the   #
# program such as WebKitSpeechRecognition.                      #
#################################################################
RobotsName = "Spot"

#################################################################
# This next line is to correct a PWM Frequency error in my      #
# PCA9685. This error may not effect you.                       #
# The only true way to know is to measure the PWM signal        #
# frequency being sent from the PCA9685 to the servos.          #
# You should have 60Hz, if not, you will need to adjust the PWM	#
# frequency up or down to get 60Hz                              #
# Use small changes or you could damage your servos.            #
#################################################################
Back.setPWMFreq(1, 54)

#################################################################
# Display time on the LCD                                       #
# I was having an issue with the Raspi dropping ofline.         #
# In order to help narrow down the cause, I am adding in a      #
# clock to the LCD display.                                     #
# If the clock continues to keep time on the LCD, then the      #
# fault is with the network interface.                          #
#################################################################
def LCD_DisplayTime(data):
    LCD.display(RobotsName, 0)
    format = "%I:%M:%S %p"
    LCD.display(time.strftime(format), 1)
    
clock.addListener("publishTime", "python", "LCD_DisplayTime")
clock.startClock()
LCD.clear()

#################################################################
# Setup the Foot Class routines.                                #
# Once the class has been run, we can create an instance by     #
# calling the Feet method.  This requires all the servo objects #
# to be passed to it as parameters.                             #
#################################################################
execfile(RuningFolder+'/FootClass.py')
legs = Feet(FLShoulder, FLArm, FLWrist, FRShoulder, FRArm, FRWrist, BLShoulder, BLArm, BLWrist, BRShoulder, BRArm, BRWrist)
legs.disableAutoLevel()
legs.rest()

#################################################################
# Setup the MPU6050 calibration and callback functions          #
# the setXGyroOffset() sets the calibration for the Gyro in the #
# MPU6050, to get an idea of what value to set, use the         #
# getGyroXSelfTestFactoryTrim(), change the X for Y and Z to    #
# get the other values.                                         #
# The updateOrientation() function is the call back target for  #
# the MPU6050 service, we then use this to call the balance     #
# routines in the FootClass.                                    #
#################################################################
if runtime.isStarted("MPU6050A"):
    MPU6050A.setXGyroOffset(75)
    MPU6050A.setYGyroOffset(0)
    MPU6050A.setZGyroOffset(0)
    def updateOrientation(data):
        legs.updateIMU(data.pitch, data.roll)
    python.subscribe('MPU6050A', 'publishOrientation', 'python', 'updateOrientation')

#################################################################
# The mounting of the MPU6050 can sometimes not be the most     #
# accurate.  This can effect the balance of the robot.          #
# So provision is allowed here to correct for any errors.       #
# Set both values to 0.0, then print the legs status.           #
# print(legs)                                                   #
# this will give the current Roll and Pitch, note these down.   #
# Rotate the robot 180 degrees and print the status again.      #
# Compare the two set of values.                                #
# Subtracting the smaller value from the larger value should    #
# give the error amount. Subtract that from the currect set     #
# values and restart the robot.                                 #
#################################################################
legs.setRollOffset(0.066961)
legs.setPitchOffset(0.079107)

#################################################################
# When not activly executing a command, we don't want the       #
# robot to just stand there,  This file is responsible for      #
# giving our robot a bitof life.                                #
# By blinking the eyes, coordinating the left and right eyes    #
# and performing other random like movements, just to make our  #
# robot appear to be alive.                                     #
#################################################################
execfile(RuningFolder+'/Gestures.py')
