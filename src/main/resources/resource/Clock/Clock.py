# start the services
clock = Runtime.start("clock","Clock")

# define the on_date event handler
def on_time(timedata):
    print("on_date " + str(timedata))

# define the on_epoch event handler
def on_epoch(timedata):
    print("on_epoch " + str(timedata))

# create a message routes
clock.addListener("publishTime", "python", "on_time")
clock.addListener("publishEpoch", "python", "on_epoch")

# start the clock
clock.setInterval(1000)
clock.startClock()
# optional : wait the first loop before execution start
# clock.startClock(1)
# clock.stopClock() when you are done