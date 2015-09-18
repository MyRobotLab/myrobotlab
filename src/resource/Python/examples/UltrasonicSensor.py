python = Runtime.start("python","Python")
sr04 = Runtime.start("sr04", "UltrasonicSensor")
sr04.attach("COM15", 12, 11)
sr04.addRangeListener(python)

def onRange(distance):
  print "distance ", distance, " cm"

# event driven ranging
# start ranging for 5 seconds - the publishRange(distance) will be
# called for every attempt to range - this SHOULD NOT INTERFERE WITH SERVOS
# YAY !
# the even ranging DOES NOT USE Arduino's pulseIn() method - 
# at the moment range() & ping() do
sr04.startRanging()
sleep(5)
sr04.stopRanging()

# range can also be retreieved in a blocking call
print "on demand range is ", sr04.range()

# you can also ping - ping does not do any calculations
# it simply returns the duration in microseconds of the ping
print "ping ", sr04.ping(), " mircoseconds"
