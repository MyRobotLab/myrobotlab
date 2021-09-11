#########################################
# Joystick.py
# categories: input sensor joystick
# more info @: http://myrobotlab.org/service/Joystick
#########################################
# start the services
joy = Runtime.start("joy","Joystick")
#this set which kind of controller you want to poll data from
#it is the number you can see in the Joystick GUI when you open the list of devices
joy.setController(1)

#tell joystick service to send data to python as a message only when new data is aviable
joy.addInputListener(python)

#this is the method in python which receive the data from joystick service
#it is triggered only when new data arrive, it's not a loop !
def onJoystickInput(data):
 #this print the name of the key/button you pressed (it's a String)
 #this print the value of the key/button (it's a Float)
 print data.id, data.value
 if (data.id == "Num 3"):
     print("button 3 was pressed its value is", data.value)
     dexarm1.move_to1(50, 300, -30)
 elif (data.id == "Num 9"):
     print("script example.py", data.value)
     execfile(RuningFolder+'/Dexarm/scripts/example.py')
 elif (data.id == "Num 1"):
     print("test1", data.value)
     dexarm1.set_module_type1(0)
     dexarm1.read_Gcode1()
 elif (data.id == "Num 2"):
     print("Go home", data.value)
     dexarm1.go_home1()
     dexarm2.go_home2()
 elif (data.id == "Num 5"):
     print("Disconnect", data.value)
     dexarm1.disconnect1()
     dexarm2.disconnect2()    
 #elif (data.id == "Num 4"):
     #print("Moving down", data.value)
     #dexarm.move_down()
 #elif (data.id == "Num 7"):
     #print("Moving down", data.value)
     #dexarm.set_relative()
 #elif (data.id == "Num 6"):
     #print("Moving down", data.value)
     #dexarm.set_absolute()
      