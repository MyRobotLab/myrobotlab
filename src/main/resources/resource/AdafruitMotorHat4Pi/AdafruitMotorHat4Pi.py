# Start the services needed
raspi = runtime.start("raspi","RasPi")
hat = runtime.start("hat","AdafruitMotorHat4Pi")
m1 = runtime.start("m1","MotorHat4Pi")
# Attach the HAT to i2c bus 1 and address 0x60
hat.attach("raspi","1","0x60")
# Use the M1 motor port and attach the motor to the hat
m1.setMotor("M1")
m1.attach("hat")
# Now everything is wired up and we run a few tests
# Full speed forward
m1.move(1) 
sleep(3)
# half speed forward
m1.move(.5)
sleep(3)
# Move backward at 60% speed
m1.move(-.6)
sleep(3)
# Stop
m1.move(0)
# Now you should be able to use the GUI or a script to control the motor
