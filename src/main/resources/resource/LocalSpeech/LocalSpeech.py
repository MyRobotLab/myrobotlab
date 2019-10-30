#########################################
# LocalSpeech.py
# description: used as a general template
# categories: speech
# more info @: http://myrobotlab.org/service/LocalSpeech
#########################################

# start the service
mouth = Runtime.start('mouth','LocalSpeech')

#possible voices ( selected voice is stored inside config until you change it )
print ("these are the voices I can have", mouth.getVoices())
print ("this is the voice I am using", mouth.getVoice())

# ( macOs )
# set your voice from macos control panel
# you can test it using say command from terminal

# mouth.setVoice("Microsoft Zira Desktop - English (United States)")
mouth.speakBlocking(u"Hello this is an english voice")
mouth.speakBlocking(u"Bonjour ceci est une voix française, je teste les accents aussi avec le mot éléphant")

mouth.setVolume(0.7)
mouth.speakBlocking("Silent please")