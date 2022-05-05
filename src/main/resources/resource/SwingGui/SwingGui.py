#########################################
# SwingGui.py
# description: Service used to graphically display and control other services
# categories: display
# more info @: http://myrobotlab.org/service/SwingGui
#########################################

# start the service
gui = runtime.start('gui','SwingGui')

# start a new service
python2 = runtime.start('python2','Python')

# focus service
gui.setActiveTab("python2")

#gui.undockTab("python2")
