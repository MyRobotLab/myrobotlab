# This is a demo of the setup for DiyServo. 
# This setup is valid from version 1.0.2274
# 
# The DiyServo service don't need to be used with an Arduino. 
# This example show a configuration to drive it from the GPIO pins on a raspberry PI using 
# the Raspi service, an Adafruit16CServoDriver and an Ads1115 AD-converter
# You can also combine the different alternatives in any fashion that you want
#
# Start of script for DiyServo
# Start the RasPi service ( Ths only works on a Raspberry PI )
raspi = runtime.start("raspi","RasPi")
# Start and configure the Adafruit16CServoDriver connect to the raspberry on i2c address 0x40 ( default )
ada = runtime.start("ada","Adafruit16CServoDriver")
ada.setController(raspi,"1","0x40")
# Start the Motor. You can use also use a different type of Motor
motor = runtime.start("diyservo.motor","Motor")
# Tell the motor to attach to the Adafruit16CServoDriver and setup the power and direction pins
# How you configure the motor is depending on what type of motor board you use
motor.attach(ada)
motor.setPwrDirPins(0, 1)
# Start the Ads1115 service to get the Analog input and set the i2c address
ads1115 = runtime.start("ads1115","Ads1115")
ads1115.setController(raspi,"1","0x48")
# Start the DiyServo
servo = runtime.start("diyservo","DiyServo")
servo.attach(ads1115,0) # Attach the analog pin 0 
servo.moveTo(90)
# At this stage you can use the gui or a script to control the DiyServo
