from java.lang import String
from org.myrobotlab.service import Speech
from org.myrobotlab.service import Sphinx
from org.myrobotlab.service import Runtime

# create ear and mouth
ear = Runtime.createAndStart("ear","Sphinx")
mouth = Runtime.createAndStart("mouth","Speech")
mouth.setGoogleURI("http://thehackettfamily.org/Voice_api/api2.php?voice=Heather&txt=")

gender = 1

# start listening for the words we are interested in
ear.startListening("hello | forward | back | go |turn left | turn right | male voice | female voice")

# set up a message route from the ear --to--> python method "heard"
ear.addListener("recognized", python.getName(), "heard");

# this method is invoked when something is
# recognized by the ear - in this case we
# have the mouth "talk back" the word it recognized
def heard():
data = msg_ear_recognized.data[0]

if (data == "male voice"):
mouth.setGoogleURI("http://thehackettfamily.org/Voice_api/api2.php?voice=Rod&txt=")
global gender
gender = 0
mouth.speak("i am a man now")

elif (data == "female voice"):
mouth.setGoogleURI("http://thehackettfamily.org/Voice_api/api2.php?voice=Heather&txt=")
global gender
gender = 1
mouth.speak("i am a women now")

elif (data == "hello"):
if gender == 0 :
mouth.speak("Hello")
elif gender == 1 :
mouth.speak("Hello.")

elif (data == "forward"):
if gender == 0 :
mouth.speak("forward")
elif gender == 1 :
mouth.speak("forward.")

elif (data == "back"):
if gender == 0 :
mouth.speak("back")
elif gender == 1:
mouth.speak("back.")

elif (data == "go"):
if gender == 0 :
mouth.speak("go")
elif gender == 1 :
mouth.speak("go.")

elif (data == "turn left"):
if gender == 0 :
mouth.speak("turn left")
elif gender == 1 :
mouth.speak("turn left.")

elif (data == "turn right"):
if gender == 0 :
mouth.speak("turn right")
elif gender == 1 :
mouth.speak("turn right.")

# prevent infinite loop - this will suppress the
# recognition when speaking - default behavior
# when attaching an ear to a mouth :)
ear.attach("mouth")