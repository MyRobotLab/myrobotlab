# a safe tracking script - servos are created seperately
# and their limits are programmed, they are then "bound" to
# the tracking service
tracker = Runtime.create("tracker","Tracking")
 
# create servos BEFORE starting the tracking service
# so we can specify values for the servos and specify names
# before it starts tracking
 
rotation = Runtime.create("rotation","Servo")
neck = Runtime.create("neck","Servo")
arduino = Runtime.create("arduino","Arduino")
arduino.connect("COM3", 57600, 8, 1, 0)
eye = Runtime.create("eye","OpenCV")
eye.setCameraIndex(1)

#attaching servos to arduino using PIN 3 and PIN 9
arduino.attach(rotation.getName() , 13)
arduino.attach(neck.getName(), 12) 

# set safety limits - servos
# will not go beyond these limits
rotation.setMinMax(50,170)
 
neck.setMinMax(50,170)
 
 
# here we are binding are new servos with different names
# to the tracking service.  If not specified the tracking service
# will create a servo named x and y
 
tracker.attach(arduino)
tracker.attachServos(rotation, neck)
tracker.attach(eye)
 
tracker.setRestPosition(90, 90)
 
# setXMinMax & setYMinMax (min, max) - this will set the min and maximum
# x value it will send the servo - typically this is not needed
# because the tracking service will pull the min and max positions from 
# the servos it attaches too
tracker.setXMinMax(10, 170)
tracker.setYMinMax(10, 170)
 
# setServoPins (x, y) set the servo of the pan and tilt repectively
tracker.setServoPins(13,12)
# tracker.setCameraIndex(1) #change cameras if necessary
 
tracker.startService()
tracker.trackLKPoint()
 
#tracker.learnBackground()
