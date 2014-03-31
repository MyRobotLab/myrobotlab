from java.lang import String

# a demo of using speech recognition to 
# turn an Arduino's pin 13 off or on

# the commands are 2 stage - with the command, the the system
# asking if that command was said, then a affirmation or negation
# e.g. - you say "on", the system asks if you said "on", you say "yes"
# the system turns the Arduino pin 13 on

# create services
python = Runtime.createAndStart("python", "Python")
mouth = Runtime.createAndStart("mouth", "Speech")
arduino = Runtime.createAndStart("arduino", "Arduino")
ear = Runtime.createAndStart("ear", "Sphinx")

# connect mrl to the arduino - change the port on your system
arduino.connect("COM10")

# attaching the mouth to the ear
# prevents listening when speaking
# which causes an undesired feedback loop
ear.attach(mouth)

# for anything recognized we'll send that data back to python to be printed
# in the python tab
# ear.addListener("recognized", "python", "heard", String.class);
# add a "on" -> arduino.digitalWrite(13, 1)  - turn's pin 13 on
ear.addCommand("on", arduino.getName(), "digitalWrite", 13, 1)
# add a "off" -> arduino.digitalWrite(13, 0)  - turn's pin 13 off
ear.addCommand("off", arduino.getName(), "digitalWrite", 13, 0)

arduino.pinMode(13,1)

# add confirmations and negations - this makes any command a 2 part commit
# where first you say the command then mrl asks you if that is what you said
# after recognition
ear.addComfirmations("yes","correct","yeah","ya") 
ear.addNegations("no","wrong","nope","nah")

# begin listening
ear.startListening()

def heard():
  data = msg_ear_recognized.data[0]
  # print it
  print "heard ", data         
  