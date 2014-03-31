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

# create ear and mouth
ear = Runtime.createAndStart("ear","Sphinx")
mouth = Runtime.createAndStart("mouth","Speech")

# start listening for the words we are interested in
ear.startListening("hello | forward | back | stop | go |turn left | turn right | spin | power off")


# set up a message route from the ear --to--> python method "heard"
ear.addListener("recognized", python.getName(), "heard"); 

# this method is invoked when something is 
# recognized by the ear - in this case we
# have the mouth "talk back" the word it recognized
def heard():
      data = msg_ear_recognized.data[0]
      mouth.speak("you said " + data)
      print "heard ", data
      if (data == "forward"):
         print "robot goes forward" 
      elif (data == "hello"):
         print  "robot says hello"
    # ... etc
    
# prevent infinite loop - this will suppress the
# recognition when speaking - default behavior
# when attaching an ear to a mouth :)
ear.attach("mouth")

