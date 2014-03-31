keyboard = Runtime.createAndStart("keyboard", "Keyboard")
keyboard.addListener("keyCommand", python.getName(), "input")
 
def input():
    # print 'python object is ', msg_[service]_[method]
    cmd = msg_keyboard_keyCommand.data[0]
    print 'python data is ', cmd
 
    if (cmd == "A"):
      print "hello A !"
    elif (cmd == "B"):
      print "hello B !"
