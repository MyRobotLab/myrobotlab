tracker = Runtime.create("tracker","Tracking")
 
# create all the peer services
eyeX = Runtime.create("eyeX","Servo")
eyeY = Runtime.create("eyeY","Servo")
eyeY.setInverted(True)
arduino = Runtime.create("arduino","Arduino")
xpid = Runtime.create("xpid","PID");
ypid = Runtime.create("ypid","PID");
 
# adjust values
arduino.connect("COM3")
eye = Runtime.create("eye","OpenCV")
eye.setCameraIndex(0)
 
# flip the pid if needed
# xpid.invert()
xpid.setOutputRange(-1, 1)
xpid.setPID(10.0, 0, 0.1)
xpid.setSetpoint(0.5) # we want the target in the middle of the x
 
# flip the pid if needed
# ypid.invert()
ypid.setOutputRange(-1, 1)
ypid.setPID(10.0, 0, 0.1)
ypid.setSetpoint(0.5)
 
# set safety limits - servos
# will not go beyond these limits
eyeX.setMinMax(65,90)
 
eyeY.setMinMax(22,85)
 
# here we are attaching to the
# manually created peer services
 
tracker.attach(arduino)
tracker.attachServos(eyeX, 3, eyeY, 6)
tracker.attach(eye)
tracker.attachPIDs(xpid, ypid)
 
tracker.setRestPosition(80, 47)
 
tracker.startService()
tracker.trackPoint(0.5,0.5)