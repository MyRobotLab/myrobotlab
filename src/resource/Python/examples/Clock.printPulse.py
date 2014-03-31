#create a clock service named clock
clock = Runtime.createAndStart("clock","Clock")
#create a log service named log
log = Runtime.createAndStart("log","Log")
#create a message between clock and log, so you can read date and time on log
clock.addListener("pulse","log","log")
#start clock
clock.startClock()
