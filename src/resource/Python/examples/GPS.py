from time import sleep


gps1 = Runtime.createAndStart("gps1", "GPS")
gps1.startService()
gps1.connect("/dev/ttyUSB0")
sleep(1)


def input():
  startingAngle = 0
  Latitude = msg_gps1_publishGGAData.data[0][2]
  Longitude =  msg_gps1_publishGGAData.data[0][4]
  altitude = msg_gps1_publishGGAData.data[0][9]
  print "Lat: " + Latitude
  print "Long: " + Longitude
  print "Alt: " + altitude + "\n"
  

#have python listening to lidar
gps1.addListener("publishGGAData", python.name, "input") 

print "Ready to receive Data from GPS..."


