from java.lang import String
from org.myrobotlab.service import Speech
from org.myrobotlab.service import Sphinx
from org.myrobotlab.service import Runtime

# This demo is a basic speech recognition script.
#
# A set of commands needs to be defined before the recognizer starts
# Internet connectivity is needed initially to download the audio files
# of the Speech service (its default behavior interfaces with Google)
# once the phrases are spoken once, the files are used from that point on
# and internet connectivity is no longer used.  These cached files 
# can be found ./audioFile/google/<language code>/audrey/phrase.mpe
#
# A message route is created to NOT recognize speech when the speech service is talking.
# Otherwise, initially amusing scenarios can occur such as infinite loops of
# the robot recognizing "hello", then saying "hello", then recognizing "hello"...
#
# The recognized phrase can easily be hooked to additional function such as
# changing the mode of the robot, or moving it.  Speech recognition is not
# the best interface to do finely controlled actuation.  But, it is very
# convenient to express high-level (e.g. go to center of the room) commands
#
# FYI - The memory requirements for Sphinx are a bit hefty and depending on the
# platform additional JVM arguments might be necessary e.g. -Xmx256m

# create an ear
ear = Runtime.create("ear","Sphinx")

# create the grammar you would like recognized
# this must be done before the service is started
ear.createGrammar("all open | hand close | pinch mode | open pinch | hand open | hand rest | hand open two ")
ear.startService()

# start the mouth
mouth = Runtime.createAndStart("mouth","Speech")

# set up a message route from the ear --to--> python method "heard"
ear.addListener("recognized", python.name, "heard", String().getClass()); 

# this method is invoked when something is 
# recognized by the ear - in this case we
# have the mouth "talk back" the word it recognized

    
# prevent infinite loop - this will suppress the
# recognition when speaking - default behavior
# when attaching an ear to a mouth :)
ear.attach("mouth")

#create an Arduino &  name arduino & index
runtime.createAndStart("arduino","Arduino")
 
runtime.createAndStart("thumb","Servo")
runtime.createAndStart("index","Servo")
runtime.createAndStart("majeure","Servo")
runtime.createAndStart("ringfinger","Servo")
runtime.createAndStart("pinky","Servo")
runtime.createAndStart("wrist","Servo")
runtime.createAndStart("biceps","Servo")
runtime.createAndStart("rotate","Servo")
runtime.createAndStart("shoulder","Servo")
# runtime.createAndStart("omoplat","Servo")
runtime.createAndStart("neck","Servo")
runtime.createAndStart("rothead","Servo")
 
# configuration for the arduino & quick test
arduino.setBoard("atmega1280") # atmega328 | atmega168 | mega2560 | atmega1280 etc
arduino.connect("COM7",57600,8,1,0)
sleep(2)
arduino.pinMode(17,0)
arduino.analogReadPollingStart(17)
sleep(1)
arduino.pinMode(17,0)
arduino.analogReadPollingStop(17)
 
# attach servos
arduino.servoAttach("thumb",2)
arduino.servoAttach("index",3)
arduino.servoAttach("majeure",4)
arduino.servoAttach("ringfinger",5)
arduino.servoAttach("pinky",6)
arduino.servoAttach("wrist",7)
arduino.servoAttach("biceps",8)
arduino.servoAttach("rotate",9)
arduino.servoAttach("shoulder",10)
#arduino.servoAttach("omoplat",11)
arduino.servoAttach("neck",12)
arduino.servoAttach("rothead",13)
 
# refresh the gui 
arduino.publishState()
thumb.publishState()
index.publishState()
majeure.publishState()
ringfinger.publishState()
pinky.publishState()
wrist.publishState()
biceps.publishState()
rotate.publishState()
shoulder.publishState()
#omoplat.publishState()
neck.publishState()
rothead.publishState()

def allopen():
  thumb.moveTo(0)
  index.moveTo(0)
  majeure.moveTo(0)
  ringfinger.moveTo(0)
  pinky.moveTo(0)
  wrist.moveTo(0)
  biceps.moveTo(0)
  rotate.moveTo(90)
  shoulder.moveTo(0)
  #omoplat.moveTo(0)
  neck.moveTo(90)
  rothead.moveTo(90)

def handclose():
  thumb.moveTo(130)
  index.moveTo(180)
  majeure.moveTo(180)
  ringfinger.moveTo(180)
  pinky.moveTo(180)
  wrist.moveTo(180)
  biceps.moveTo(90)
  rotate.moveTo(50)
  shoulder.moveTo(20)
  #omoplat.moveTo(0)
  neck.moveTo(130)
  rothead.moveTo(110)

def pinchmode():
  thumb.moveTo(130)
  index.moveTo(140)
  majeure.moveTo(180)
  ringfinger.moveTo(180)
  pinky.moveTo(180)
  wrist.moveTo(180)
  biceps.moveTo(90)
  rotate.moveTo(80)
  shoulder.moveTo(20)
  #omoplat.moveTo(0)
  neck.moveTo(140)
  rothead.moveTo(110)

def openpinch():
  thumb.moveTo(0)
  index.moveTo(0)
  majeure.moveTo(180)
  ringfinger.moveTo(180)
  pinky.moveTo(180)
  wrist.moveTo(180)
  biceps.moveTo(90)
  rotate.moveTo(80)
  shoulder.moveTo(25)
  #omoplat.moveTo(0)
  neck.moveTo(145)
  rothead.moveTo(125)
 
def handopen():
  thumb.moveTo(0)
  index.moveTo(0)
  majeure.moveTo(0)
  ringfinger.moveTo(0)
  pinky.moveTo(0)
  wrist.moveTo(180)
  biceps.moveTo(80)
  rotate.moveTo(85)
  shoulder.moveTo(25)
  #omoplat.moveTo(0)
  neck.moveTo(140)
  rothead.moveTo(130)
 
def handrest():
  thumb.moveTo(60)
  index.moveTo(40)
  majeure.moveTo(30)
  ringfinger.moveTo(40)
  pinky.moveTo(40)
  wrist.moveTo(50)
  biceps.moveTo(0)
  rotate.moveTo(90)
  shoulder.moveTo(0)
  #omoplat.moveTo(0)
  neck.moveTo(160)
  rothead.moveTo(80)

def handopen2():
  thumb.moveTo(0)
  index.moveTo(0)
  majeure.moveTo(0)
  ringfinger.moveTo(0)
  pinky.moveTo(0)
  wrist.moveTo(0)
  biceps.moveTo(0)
  rotate.moveTo(60)
  shoulder.moveTo(0)
 
#for x in range (0, 180):
# allopen()
# sleep(2.5)
# handclose()
# sleep(2.5)
# pinchmode()
# sleep(1.5)
# openpinch()
# sleep(0.5)
# handopen()
# sleep(1.5)
# handrest()
# sleep(2.0)

# all open | hand close | pinch mode | open pinch | hand open | hand rest | hand open two 

def heard():
      data = msg_ear_recognized.data[0]
      # mouth.speak("you said " + data)
      print "heard ", data
      ear.stopListening()
      if (data == "all open"):
        allopen()
      elif (data == "hand close"):
         handclose()
      elif (data == "pinch mode"):
         pinchmode()
      elif (data == "open pinch"):
         openpinch()
      elif (data == "hand open"):
         handopen()
      elif (data == "hand rest"):
         handrest()
      elif (data == "hand open two"):
         handopen2()
      ear.startListening()
      
    # ... etc
 
