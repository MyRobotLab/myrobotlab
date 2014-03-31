arduino = Runtime.createAndStart("arduino","Arduino")
joystick = runtime.createAndStart("joystick","Joystick")
hand  = Runtime.createAndStart("hand","Servo")
arduino.connect("COM3", 57600, 8, 1, 0)
sleep(4)
arduino.attach(hand.getName() , 2)

b = 100
print b
hand.moveTo(b)
 
def x():
    global b
    x = msg_joystick_XAxisRaw.data[0]
    print x
    if (x == 1):
     b += 1
     print b
     hand.moveTo(b)
     
    elif (x == -1):
     b -= 1
     print b
     hand.moveTo(b)
    return
def a():
    # the API is 0 based cause arrays are 0 based - but when I count button I start with 1
    # so now buttons start @ 1  msg_joystick_button1 = button1 - its the "right" thing to do ...
    # anyway Alessandruino said we are men not machines - so I will make it manly and not sissy machine !
    a = msg_joystick_button1.data[0]
    print a
    if (a == 1):
     print 'button pressed'
    elif ( a == 0):
     print 'button not pressed'
   
#create a message route from joy to python so we can listen for button
joystick.addListener("XAxisRaw", python.name, "x")
joystick.addListener("button1", python.name, "a")
