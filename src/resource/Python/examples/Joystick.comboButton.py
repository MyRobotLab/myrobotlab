joystick = runtime.createAndStart("joystick","Joystick")
sleep(4)

a = 0
rb = 0

def buttonA():
    global a
    a = msg_joystick_button1.data[0]
    print a
    check()
    
def buttonRB():
    global rb
    rb = msg_joystick_button6.data[0]
    print rb
    check()

def check():
    if ((a == 1) and (rb == 1)):
     print "Combo!"
    elif ((a == 1) and (rb == 0)):
     print "Button A pressed"
    elif ((a == 0) and (rb == 1)):
     print "Button RB pressed"
    else:
     print "Nothing pressed"

joystick.addListener("button1", python.name, "buttonA")
joystick.addListener("button6", python.name, "buttonRB")
