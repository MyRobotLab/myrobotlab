from time import sleep


gps1 = runtime.start("gps1", "Gps")

gps1.connect("COM3")
sleep(1)

# define some points ... 
# Lets use Nova Labs 1.0
lat1 = 38.950829
lon1 = -77.339502
novalabs1 = gps1.setPoint(lat1, lon1)

# and Nova Labs 2.0 North East of NL 1
lat2 = 38.954471 
lon2 = -77.338271
novalabs2 = gps1.setPoint(lat2, lon2)

# and the nearest Metro station
lat3 = 38.947254
lon3 = -77.337844
novalabsmetro = gps1.setPoint(lat3, lon3)

# and the Sand Trap out back
lat4 = 38.954844
lon4 = -77.338797
novalabstrap = gps1.setPoint(lat4, lon4)

# Lets create some points for a Polygon Geofence with diagonals defined by NL 1.0 and 2.0
cornerNW = gps1.setPoint(lat2,lon1) # NW corner
cornerNE = gps1.setPoint(lat2,lon2) # NE corner
cornerSE = gps1.setPoint(lat1,lon2) # SE corner
cornerSW = gps1.setPoint(lat1,lon1) # SW corner

def input():
  startingAngle = 0
  Latitude = msg_gps1_publishGGAData.data[0][2]
  Longitude =  msg_gps1_publishGGAData.data[0][4]
  altitude = msg_gps1_publishGGAData.data[0][9]
  print "Lat: " + Latitude
  print "Long: " + Longitude
  print "Alt: " + altitude + "\n"
  

#have python listening to gps
gps1.addListener("publishGGAData", python.name, "input") 

print "Ready to receive Data from GPS..."


# Time to play with some GPS points and GeoFence examples
print "Let's put a GeoFence around around Nova Labs 2.0 (",novalabs2.getLat(),",",novalabs2.getLon(),") with a 100 meter radius"
# create a point based geofence with a 100m radius
geofence = gps1.setPointGeoFence(lat2, lon2, 100)
# or we could have done this 
# geofence = gps1.setPointGeoFence(novalabs2, 100)

distance = gps1.calculateDistance(lat1, lon1, lat2, lon2)
# or we could have done this
# distance = gps1.calculateDistance(novalabs1, novalabs2)

# check if a GPS point is inside the fence
if (gps1.checkInside(geofence, lat1, lon1)): # or we could have used gps1.checkInside(geofence, novalabs1)
    print "Nova Labs 1.0 is Inside the Fence around Nova Labs 2.0"
else:
    print "Nova Labs 1.0 is Outside the Fence around Nova Labs 2.0"
print "Distance (meters): ",distance," between Nova Labs 1.0 and Nova Labs 2.0\n"

distance = gps1.calculateDistance(lat2, lon2, lat3, lon3)

# check if a GPS point is inside the fence
if (gps1.checkInside(geofence, lat3, lon3)):
    print "The Metro station is Inside the Fence around Nova Labs 2.0"
else:
    print "The Metro station is Outside the Fence around Nova Labs 2.0"
print "Distance (meters): ",distance, " between NL 2 and the nearest Metro Station\n"

distance = gps1.calculateDistance(lat2, lon2, lat4, lon4)

# check if a GPS point is inside the fence
if (gps1.checkInside(geofence, lat4, lon4)):
    print "Sand Trap is Inside the Fence around Nova Labs 2.0"
else:
    print "Sand Trap is Outside the Fence around Nova Labs 2.0"
print "Distance (meters): ",distance, "between NL 2 and the nearest sand trap\n"

# create a Polygon geofence based on the NL 1.0 and 2.0 corners
print "Let's build a geofence with NL 1.0 and 2.0 in diagonal corners"
geogrid = gps1.setPolygonGeoFence([cornerNW, cornerNE, cornerSE, cornerSW])

#check if the sand trap is in the fence
if (gps1.checkInside(geogrid, lat4, lon4)): # or we could have used gps1.checkInside(geogrid, novalabstrap)
    print "Sand Trap is Inside the Fence"
else:
    print "Sand Trap is Outside the Fence"
