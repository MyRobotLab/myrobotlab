# From version 1.0.2316 use attach instead of setController
# This script is if you use the GPOI pins of the Raspberry PI
raspi = runtime.start("raspi","RasPi")
# adaFruit16c.setController("RasPi","1","0x40")
adaFruit16c = runtime.start("adaFruit16c","Adafruit16CServoDriver")
adaFruit16c.attach(raspi,"1","0x40")
#
# This part is common for both devices and creates two servo instances
# on port 3 and 8 on the Adafruit16CServoDriver
# Change the names of the servos and the pin numbers to your usage
thumb = runtime.start("thumb", "Servo")
# attach it to the pwm board - pin 9
thumb.attach(adaFruit16c,9)
# When this script has been executed you should be able to 
# move the servos using the GUI or using python
thumb.setVelocity(40)
thumb.moveToBlocking(0)
thumb.moveToBlocking(180)
thumb.moveToBlocking(0)
thumb.moveToBlocking(180)


