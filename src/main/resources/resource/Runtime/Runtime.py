#########################################
# Runtime.py
# description: Runtime starts stops saves all other services
# categories: system
# more info @: http://myrobotlab.org/service/Runtime
#########################################

# prints the days hours minutes your MRL has been alive
print runtime.getUptime()

# print the id of your instance of mrl
print ("my mrl version is {}".format(runtime.getVersion()))
print ("myrobotlab id is {}".format(runtime.getId()))
print ("computer {}".format(runtime.getHostname()))
print ("process id {}".format(runtime.getPid()))

# learn about your system
print ("{} processors".format(runtime.availableProcessors()))
print ("{} Gb free memory".format(runtime.getFreeMemory()/100000000))
print ("{} Gb total memory".format(runtime.getTotalMemory()/100000000))

# start a single service
clock = runtime.start('clock', 'Clock')
clock.startClock()
sleep(3)

# release the service
runtime.release('clock')

# get list of currently running services
services = runtime.getServices()
print ("list my {} currently running services".format(len(services)))
for service in services:
  print (service.getName())

# save the current state of the system into a config set
# located data/config/my-config
runtime.saveConfig('my-config')

# start that config set up
runtime.startConfig('my-config')

# release that config
# runtime.releaseConfig('my-config')

# create and start an Arduino service
runtime.start("arduino", "Arduino")

print(runtime.getHostname())
print(runtime.getPid())

# To execute a program from the Operating System (OS)
# Note: this is blocking, that is the program must finish before control returns to MRL
# runtime.exec("c:/notepad.exe")
# runtime.shutdown(10)
