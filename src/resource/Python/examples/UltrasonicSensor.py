from org.myrobotlab.service import Stepper

stepper = Runtime.start("stepper","Stepper")

comPort = "COM12" # com port of the arduino
dirPin = 34 # the direction pin
stepPin = 38 # the step pin
# steps = 200 # number of steps 

# TODO - speed, blocking calls, stepper event handler

stepper.attach("COM12", dirPin, stepPin)

# Stepper Events
# adding an event listener for stepper events
# currently a stop event is only implemented
# however, a position change event could be possible
# not sure how much latency there would be on a 57K line
# probably a good idea - the event in this case is just
# handled by printing it - both other services could handle
# it differently - e.g. if stepper < threshold - do something
stepper.addPublishStepperEventListener(python)
def publishStepperEvent(position):
  print "position ", position

# move 200 in 1 direction
stepper.move(200)

sleep(2)

# move 100 more
stepper.move(100)

sleep(2)

stepper.move(-300)

sleep(2)

# reset internal counter - is now 0
stepper.reset()

# this will begin to move the motor
# then quickly stop it - the 
# stop produces a "stop" event which 
# will report at what position the motor
# was stopped
stepper.move(-200)
stepper.stop()

