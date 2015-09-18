# fun with Runtime :)

# install and/or update the Arduino service
runtime.update("org.myrobotlab.service.Arduino")
# install and/or update the OpenCV service
runtime.update("org.myrobotlab.service.OpenCV")

# install and/or update everything
runtime.updateAll()

# update the bleeding edge myrobotlab.jar
runtime.updateMyRobotLab()

# prints the days hours minutes your MRL has been alive
print runtime.getUptime()

# learn about your system
print runtime.availableProcessors()
print runtime.getFreeMemory()
print runtime.getTotalMemory()

# create and start an Arduino service
runtime.createAndStart("arduino", "Arduino")

# start the auto update system
# checks and installs updates - interval is 
# every 300 seconds
runtime.startAutoUpdate()

# stop the auto update system
runtime.stopAutoUpdate()

# the Big Hammer - the last thing
# MRL will see - shuts everything down
runtime.exit()