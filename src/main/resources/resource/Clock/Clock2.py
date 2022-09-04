# Short demo of the Clock service

# First we need to create an instance of the service
clock = Runtime.create("clock","Clock")

clock.setInterval(1000)

# There are three possible call back methods
# lets create them next
# Don't forget, if you have multiple clock services running,
# you will need to create multiple call back methods,
# each with a unique name.


# Finally, we need to start the clock.
clock.startClock(1)

# If the clock is used as part of a watchdog service or a sleep timer,
# we may need to restart the clock

