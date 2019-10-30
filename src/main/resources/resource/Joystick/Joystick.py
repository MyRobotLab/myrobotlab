#########################################
# Joystick.py
# categories: input sensor joystick
# more info @: http://myrobotlab.org/service/Joystick
#########################################
# start the services
joy = Runtime.start("joy","Joystick")
python = Runtime.start("python","Python")
#this set which kind of controller you want to poll data from
#it is the number you can see in the Joystick GUI when you open the list of devices
joy.setController(5)

#tell joystick service to send data to python as a message only when new data is aviable
joy.addInputListener(python)

#this is the method in python which receive the data from joystick service
#it is triggered only when new data arrive, it's not a loop !
def onJoystickInput(data):
 #this print the name of the key/button you pressed (it's a String)
 #this print the value of the key/button (it's a Float)
 print data.id, data.value
 if (data.id == "3"):
     print("button 3 was pressed its value is", data.value)
 elif (data.id == "x"):
     print("stick x", data.value)    
