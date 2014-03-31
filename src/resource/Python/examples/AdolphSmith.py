# script for AdolphSmith
# http://myrobotlab.org/content/my-inmoov-parts-list-and-way-working

from org.myrobotlab.framework import Service

# mouth = Runtime.createAndStart("mouth","Speech") - we don't need 2 mouths
Service.reserveRootAs("tracker.arduino","arduino")
Service.reserveRootAs("mouthControl.arduino","arduino")

tracker = Runtime.createAndStart("tracker", "Tracking")
ear = Runtime.createAndStart("ear","Sphinx")
mouthControl = Runtime.createAndStart("mouthControl","MouthControl")
mouth = mouthControl.mouth

arduino = Runtime.createAndStart("arduino","Arduino")
arduino.connect("COM11")

mouth.setGoogleURI("http://thehackettfamily.org/Voice_api/api2.php?voice=Ryan&txt=")
mouth.speakBlocking("Hello. I have powered up")
mouth.speakBlocking("And now I will start a Tracking service")

# mouthControl.mouth.setGoogleURI("http://thehackettfamily.org/Voice_api/api2.php?voice=Ryan&txt=")
# mouthControl.mouth.setGoogleURI("http://thehackettfamily.org/Voice_api/api2.php?voice=Peter&txt=")
mouthControl.mouth.setGoogleURI("http://thehackettfamily.org/Voice_api/api2.php?voice=Will&txt=")
# mouthControl.mouth.setGoogleURI("http://thehackettfamily.org/Voice_api/api2.php?voice=Kenny&txt=")
# mouthControl.mouth.setGoogleURI("http://thehackettfamily.org/Voice_api/api2.php?voice=Graham&txt=")
mouthControl.jaw.detach()
mouthControl.jaw.setPin(5)
mouthControl.jaw.attach()
mouthControl.setmouth(162,155)
mouthControl.mouth.speak("hello. i am testing mouth control. does it work. i dont know")

# set specifics on each Servo
servoX = tracker.getX()
servoX.setPin(6)
servoX.setMinMax(30, 170)

servoY = tracker.getY()
servoY.setPin(10)
servoY.setMinMax(30, 150)
servoY.setRest(56)

# optional filter settings
opencv = tracker.getOpenCV()

# setting camera index to 1 default is 0
opencv.setCameraIndex(1) 

# connect to the Arduino
tracker.connect("COM10")

# Gray & PyramidDown make face tracking
# faster - if you dont like these filters - you
# may remove them before you select a tracking type with
# the following command
# tracker.clearPreFilters()

# diffrent types of tracking

# simple face detection and tracking
# tracker.faceDetect()

# lkpoint - click in video stream with 
# mouse and it should track
tracker.startLKTracking()

# scans for faces - tracks if found
# tracker.findFace() 


# mouthControl.mouth.speak("I am a Humanoid programmed by My Robot Lab")
# mouthControl.mouth.speak("I moove now for the first time and i am very hapy")
# mouthControl.mouth.speak("Thank yo GroG for my live ")

# mouthControl.mouth.speak("I am a Humanoid programmed by My Robot Lab")
# mouthControl.mouth.speak("I am Syler the unbelievable tree d printed robot")
# mouthControl.mouth.speak("i am here to take over tegnology ")
# mouthControl.mouth.speak("if you need anything from the internet or if you want help with anything just ask ")

# mouthControl.mouth.speak("i will do my best to help ")
# mouthControl.mouth.speak("if i do not know what to do , i search google an then i know more than you, human.  ")

# start listening for the words we are interested in
ear.startListening("hello | how are you | are you alive | what are you doing")

# set up a message route from the ear --to--> python method "heard"
ear.addListener("recognized", python.name, "heard"); 

# prevent infinite loop - this will suppress the
# recognition when speaking - default behavior
# when attaching an ear to a mouth :)
ear.attach(mouth)

# this method is invoked when something is 
# recognized by the ear - in this case we
# have the mouth "talk back" the word it recognized
def heard():
      data = msg_ear_recognized.data[0]
      #mouth.speak("you said " + data)
      #print "heard ", data
      if (data == "hello"):
         mouth.speak("hello. I am a Humanoid programmed by My Robot Lab")
         servoY.moveTo(60)
         sleep(1)
         servoY.moveTo(53)
      elif (data == "how are you"):
         mouth.speak("i am fine and all my circuits are functioning perfectly")
      elif (data == "are you alive"):
         mouth.speak("define alive. are you alive just because you breath. is a virus alive. is the earth alive.")
    # ... etc
    


