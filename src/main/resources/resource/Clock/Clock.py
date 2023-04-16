#########################################
# Clock.py
# description: basic clock service
# categories: time programming
# more info @: http://myrobotlab.org/service/Clock
#########################################

clock = runtime.start("clock","Clock")
# will start a clock service but not start the clock.

clock.setInterval(1000)
# will set the time between pulses to every 1000 milliseconds or 1Hz

# will start a clock service but not start the clock.

clock.setInterval(1000)

# will set the time between pulses to every 1000 milliseconds or 1Hz

clock.addListener("pulse", "python", "clock_pulse")

# will cause the clock service to call the "clock_pulse" method in Python, 
# you will need to have defined this method.
# You can define the callback method like this

def clock_pulse(timedata):
    print("The clock has pulsed " + str(timedata))

# The timedata is required to be in the definition and contains the current time.
# There are other signals that can also be captured as well.

clock.addListener("clockStarted", "python", "clock_start")

def clock_start():
    print("The clock has started")

# This will be called whenever the service is started.
# You could use this to turn on or off an output or move a servo to 
# open a slide while the clock is running.
# The call back method would be like the following.

clock.addListener("clockStopped", "python", "clock_stopped")

def clock_stopped():
    print("The clock has been stopped")

# This will be called when ever the clock service is stopped.
# This can be used to start or stop a motor or to signal a method to put your robot to sleep.
# The callback method would be on_stopped

# The clock is capable of being started and stopped. Starting the clock can be done like this

clock.startClock()

# You can define the on_date event handler
def on_time(timedata):
    print("on_date " + str(timedata))

# define the on_epoch event handler
def on_epoch(timedata):
    print("on_epoch " + str(timedata))

# create a message routes
clock.addListener("publishTime", "python", "on_time")
clock.addListener("publishEpoch", "python", "on_epoch")

# set interval at 1000 ms or 1s
clock.setInterval(1000)
clock.startClock()
# wait and watch the clock data flow
sleep(5)

#########################################
# Watchdog Timer - a common usage of clock
# https://en.wikipedia.org/wiki/Watchdog_timer
# its like a heartbeat monitor - if a reset doesn't come
# along within a certain time "corrective action" is taken

# create a watchdog - for example if it doesn't get a stream of
# data from a joystick it will stop the robot
watchdog = runtime.start("watchdog","Clock")
# set our watchdog to fire if its not reset within a second
watchdog.setInterval(1000) 
# add the stop command which will stop the robot from moving
watchdog.addClockEvent("python","exec", "stop_robot()")

# define the method that will stop the robot
def stop_robot():
    print("stopping robot ! watchdog apply corrective action")

# start our watchdog
watchdog.startClock()

# wait for 1/2 a second
sleep(0.5)
# reset with data from another source - like the joystick
print('we got data - things are good')
watchdog.restartClock()

# wait for 1/2 a second
sleep(0.5)
# reset with data from another source - like the joystick
print('we got data - things are good')
watchdog.restartClock()

# wait for 1/2 a second
sleep(0.5)
# reset with data from another source - like the joystick
print('we got data - things are good')
watchdog.restartClock()

# wait 1.5 s which is TOO LATE ! - perhaps the joystick got 
# disconnected, but the robot comes to a halt because the data
# has not come within 1 second
sleep(1.5)

watchdog.stopClock()
clock.startClock()

# runtime.release('clock')
# runtime.release('watchdog')