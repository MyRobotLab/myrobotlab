# Short demo of the Clock service

# First we need to create an instance of the service
clock = Runtime.create("clock","Clock")

# Next we need to set the pulse interval
clock.setInterval(1000)

# There are three possible call back methods
# lets create them next
# Don't forget, if you have multiple clock services running,
# you will need to create multiple call back methods,
# each with a unique name.
def ClockPulse(timedata)
    print "The clock has pulsed"

def ClockStart():
    print "The CLock has started"

def ClockStopped()
    print "The Clock has been stopped"

# next we need to add a listener to call our call back methods.
clock.addListener("pulse", python.name, "ClockPulse")
clock.addListener("clockStarted", python.name, "ClockStart")
clock.addListener("clockStopped", python.name, "ClockStopped")

# Finally, we need to start the clock.
clock.startClock(1)

# If the clock is used as part of a watchdog service or a sleep timer,
# we may need to restart the clock
clock.restartClock(1)

# When we don't need the clock running, then we can stop it.
clock.stopClock()
