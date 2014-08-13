from org.myrobotlab.service import Stepper

stepper = Runtime.createAndStart("stepper","Servo")

comPort = "COM12" # com port of the arduino
dirPin = 34 # the direction pin
stepPin = 38 # the step pin
steps = 200 # number of steps 


stepper.attach("COM12", steps, dirPin, stepPin)