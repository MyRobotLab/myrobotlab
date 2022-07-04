from java.lang import String
from org.myrobotlab.service import Sphinx
from org.myrobotlab.service import Runtime

# create ear and mouth
ear = runtime.start("ear","Sphinx")
mouth = runtime.start("mouth","MarySpeech")

# start listening for the words we are interested in
ear.addComfirmations("yes","correct","ya")
ear.addNegations("no","wrong","nope","nah")
#ear.startListening("hello world|happy monkey|go forward|stop|yes|correct|ya|no|wrong|nope|nah")
ear.startListening("hello world|happy monkey|go forward|stop")

ear.addCommand("hello world", "python", "helloWorld")

# set up a message route from the ear --to--> python method "heard"
# ear.addListener("recognized", python.name, "heard");

# ear.addComfirmations("yes","correct","yeah","ya")
# ear.addNegations("no","wrong","nope","nah")

# ear.addCommand("hello world", "python", "helloworld")

# set up a message route from the ear --to--> python method "heard"
# ear.addListener("recognized", python.name, "heard");


# this method is invoked when something is
# recognized by the ear - in this case we
# have the mouth "talk back" the word it recognized
#def heard(phrase):
#      mouth.speak("you said " + phrase)
#      print "heard ", phrase

# prevent infinite loop - this will suppress the
# recognition when speaking - default behavior
# when attaching an ear to a mouth :)

ear.attach(mouth)

def helloworld(phrase):
    print "This is hello world in python."
    print phrase
