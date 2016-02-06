from time import sleep
from org.myrobotlab.service import Speech
from org.myrobotlab.service import Runtime

# sayThings.py
# example script for MRL showing various methods
# of the Speech Service 
# http://myrobotlab.org/doc/org/myrobotlab/service/Speech.html

# The preferred method for creating services is
# through the Runtime. This will allow
# the script to be rerun without creating a new
# Service each time. The initial delay from Service
# creation and caching voice files can be large, however
# after creation and caching methods should return 
# immediately

# Create a running instance of the Speech Service.
# Name it "speech".
speech = Runtime.create("speech","Speech")
speech.startService()

# Speak with initial defaults - Google en
speech.speak("hello brave new world")

# Google must have network connectivity
# the back-end will cache a sound file
# once it is pulled from Goole.  So the 
# first time it is slow but subsequent times its very 
# quick and can be run without network connectivity.
speech.setBackendType("GOOGLE") 
speech.setLanguage("en")
speech.speak("Hello World From Google.")
speech.setLanguage("pt") # Google supports some language codes
speech.speak("Hello World From Google.")
speech.setLanguage("de")
speech.speak("Hello World From Google.")
speech.setLanguage("ja")
speech.speak("Hello World From Google.")
speech.setLanguage("da")
speech.speak("Hello World From Google.")
speech.setLanguage("ro")
speech.speak("Hello World From Google.")

sleep(3)
# Switching to FreeTTS <<url>>
# http://freetts.sourceforge.net/docs/index.php
speech.setBackendType("FREETTS") 
speech.speak("Hello World From Free TTS.")


