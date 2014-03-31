
// create a 200 step stepper on adafruitsheild port 1
Stepper stepper1 = fruity.createStepper(200, 1); 

// step 100 in one direction
stepper1.step(100);
// step 100 in the other
stepper1.step(-100);

// FIXME - needs to be cleaned up - tear down
fruity.releaseStepper(stepper1.getName());


# Arduino and motor details can be changed here

# Here is an example of creating a Adafruit Motor Shield service. 
# Moving motor 1 forward at speed 200 for 1 second, 
# moving it backward at speed 100, then stopping.

fruity.setSpeed(200)
fruity.run(1, AdafruitMotorShield.FORWARD)
sleep(1)
fruity.setSpeed(100)
fruity.run(1, AdafruitMotorShield.BACKWARD)
sleep(1)
fruity.run(1, AdafruitMotorShield.RELEASE)
 

# Additional methods were added in the spirit of Python's minimal code for maximum effect 
# The following script does the same thing as the previous. 

fruity.runForward(1, 200)
sleep(1)
fruity.runBackward(1, 100)
sleep(1)
fruity.stop()
 
# The shield supports the standard MRL Motor API too.

fruity_m1.move(0.5) # move CW at 1/2 power
fruity_m1.move(0.0) # stop
fruity_m1.move(-0.5) # move CCw at 1/2 power