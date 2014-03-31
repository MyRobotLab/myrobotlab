from time import sleep
import math

#These lines are initialization of the LIDAR
lidar = Runtime.createAndStart("Lidar", "LIDAR")
lidar.startService()
lidar.connect("/dev/ttyUSB0")

def input():
  startingAngle = 0
  code = msg_Lidar_publishLidarData.data[0]  
  length = len(code)
  print "got " + str(length) +"readings:"
  if length==101 or length==201 or length==401:
  	startingAngle = 40
  else:	
	startingAngle = 0
  	
  for i in range(0, length):   	
   		#print hex(code[i])  #print result in hexadecimal
#    	print (code[i])  #print results (without units)
#	   	print (code[i]/float(100))	#Print results in centimeters (if LIDAR was in CM mode)
#	    	print ((code[i]/100)/2.54)	#Print results in inches (if LIDAR was in CM mode)
#    	print str(i)+"\t"+str((code[i]))  #print polarCoordinates (without units) 

#convert polar to cartesian
		x = (code[i])*math.cos(((i*100/length)+startingAngle)* (3.14159 / 180))
		y =(code[i])*math.sin(((i*100/length)+startingAngle)* (3.14159 / 180))
		print str(i+startingAngle)+"\t"+str((code[i]))+"\t"+str(x) +"\t"+str(y) # print in cartesian coordinates for plotting or graphing

    	
    	
 
#have python listening to lidar
lidar.addListener("publishLidarData", python.name, "input") 

print "setting scan mode"
lidar.setScanMode(100, 1)  # sets scan mode for 100 degree spread with a reading every 1 degree

#Give time for LIDAR to reply:
sleep (1)

while 1:
	#This should return a single scan from the LIDAR and plot it to the LIDARGUI window (and trigger the python listener above)
	lidar.singleScan()
	sleep(1)