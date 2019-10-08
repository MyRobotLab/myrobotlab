# start the services
python = Runtime.start("python","Python")
clock = Runtime.start("clock","Clock")
log   = Runtime.start("log","Log")
audio = Runtime.start("audio","AudioFile")

# define a ticktock method
def ticktock(timedata):
    print timedata
    audio.playResource("resource/Clock/tick.mp3")

#create a message routes
clock.addListener("pulse", python.name, "ticktock")
clock.addListener("pulse","log","log")

# start the clock
clock.setInterval(1000)
clock.startClock()
# optional : wait the first loop before execution start
# clock.startClock(1)
