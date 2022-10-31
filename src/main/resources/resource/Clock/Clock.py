#########################################
# Clock.py
# description: basic clock service
# categories: time programming
# more info @: http://myrobotlab.org/service/Clock
#########################################
# basic service function
# start the services
python = runtime.start("python","Python")

# You can create multiple clock services for different functions within your robot.
clock = runtime.start("clock","Clock")
# will start a clock service but not start the clock.

clock.setInterval(1000)

# will set the time between pulses to every 1000 milliseconds or 1Hz

# define the on_date event handler
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


