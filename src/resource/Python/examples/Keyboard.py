keyboard = Runtime.start("keyboard", "Keyboard")
python = Runtime.getService("python")

keyboard.addKeyListener(python);

def onKey(key):
    print "you pressed ", key
