# Short demo of the Clock service

# First we need to create an instance of the service
clock = Runtime.create("clock","Clock")

# configuration can be done before "starting" the clock
# but for anything interesting to happen, like communicating
# with other service it has to be "started"
clock.startService()

# Next we need to set the pulse interval in milliseconds
clock.setInterval(1000)

# There are three possible call back methods
# lets create them next
# Don't forget, if you have multiple clock services running,
# you will need to create multiple call back methods,
# each with a unique name.
def clock_pulse(timedata):
    print ("The clock has pulsed")

def clock_start():
    print ("The clock has started")

def clock_stopped():
    print ("The clock has been stopped")

# Next we need to add a listener to call our call back methods.
clock.addListener("pulse", python.name, "clock_pulse")
clock.addListener("clockStarted", python.name, "clock_start")
clock.addListener("clockStopped", python.name, "clock_stopped")

# "addListener" and "subscribe" are equivalent but addListener is
# done by the event publisher, and subscribe is done by subscriber.
# "subscribe" has the option of having a smaller parameter set too.
# In following case a method callback is created for this shorthand
# way of writing.  The expected callback event handler will be a method
# called "onPulse"
python.subscribe("clock", "pulse")

# onPulse(timedata) will be called when "pulse" is subscribed too.
# If the method starts with "get" or "publish" the get/publishe is replaced by "on"
def onPulse(timedata):
   print("timedata is " + str(timedata))

# Finally, we need to start the clock.
clock.startClock(1)

# If the clock is used as part of a watchdog service or a sleep timer,
# we may need to restart the clock
# clock.restartClock(1)

# When we don't need the clock running, then we can stop it.
# clock.stopClock()
