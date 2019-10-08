# fun with Runtime :)

# prints the days hours minutes your MRL has been alive
print Runtime.getUptime()

# print the id of your instance of mrl
print "my mrl version is {}".format(Runtime.getVersion)
print "myrobotlab id is {}".format(Runtime.getId())
print "computer {}".format(Runtime.getHostname())
print "process id {}".format(Runtime.getPid())

# learn about your system
print "{} processors".format(Runtime.availableProcessors())
print "{} Gb free memory".format(Runtime.getFreeMemory()/10000000)
print "{} Gb free memory".format(Runtime.getTotalMemory()/10000000)

# get list of currently running services
services = Runtime.getServices()
print "list my {} currently running services".format(len(services))
for service in services:
  print service.getName()

# create and start an Arduino service
Runtime.start("arduino", "Arduino")

+# Send a copy of the logs to the MRL debugging team
+# Pass your MRL Username as the parameter
Runtime.noWorky("GroG")
# the Big Hammer - the last thing
# MRL will see - shuts everything down in 10 seconds
# Runtime.shutdown(10)
+
+# To execute a program from the Operating System (OS)
+# Note: this is blocking, that is the program must finish before control returns to MRL
+# Runtime.exec("c:/notepad.exe")
