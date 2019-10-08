#########################################
# AndroidSpeechRecognition.py
# more info @: http://myrobotlab.org/service/AndroidSpeechRecognition
#########################################

# start the service
androidspeechrecognition = Runtime.start("androidspeechrecognition","AndroidSpeechRecognition")

# start mouth
marySpeech = Runtime.start("marySpeech", "MarySpeech")

# shutdown microphone if robot speaking
androidspeechrecognition.attach(marySpeech)

# auto rearm microphone
androidspeechrecognition.setAutoListen(True)
androidspeechrecognition.addCommand("turn on the light", "python", "lightOn")
androidspeechrecognition.addCommand("turn off the light", "python", "lightOff")

def lightOn():
  marySpeech.speakBlocking("light is on")

def lightOff():
  marySpeech.speakBlocking("light is off")
