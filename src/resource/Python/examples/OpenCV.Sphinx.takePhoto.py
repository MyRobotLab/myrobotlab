from java.lang import String
from org.myrobotlab.service import Speech
from org.myrobotlab.service import Sphinx
from org.myrobotlab.service import Runtime

# create ear and mouth
ear = Runtime.createAndStart("ear","Sphinx")
mouth = Runtime.createAndStart("mouth","Speech")
opencv = Runtime.createAndStart("opencv","OpenCV")
opencv.addFilter("pdown","PyramidDown")
opencv.setDisplayFilter("pdown")
opencv.capture()
# start listening for the words we are interested in
ear.startListening("hello robot|take photo")


# set up a message route from the ear --to--> python method "heard"
ear.addListener("recognized", python.name, "heard", String().getClass()); 

def heard():
      data = msg_ear_recognized.data[0]
      print "heard ", data
      if (data == "hello robot"):
         mouth.speak("Hi Alessandro.") 
      elif (data == "take photo"):           
           photoFileName = opencv.recordSingleFrame()
           print "name file is" , photoFileName
           mouth.speak("photo taken")

ear.attach("mouth")