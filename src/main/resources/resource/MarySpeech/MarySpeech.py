#########################################
# MarySpeech.py
# categories: speech
# more info @: http://myrobotlab.org/service/MarySpeech
#########################################

#start Service
mouth = runtime.start("mouth", "MarySpeech")

#possible voices ( selected voice is stored inside config until you change it )
print ("these are the voices I can have", mouth.getVoices())
print ("this is the voice I am using", mouth.getVoice())

#switch voice:
mouth.setVoice("Mark")
#mouth.setVoice("Camille")
#etc...

#speakBlocking!
# this blocks until speaking is done
mouth.speakBlocking(u"Hello world")
mouth.speakBlocking(u"I speak English. More voices are available, but they need to be installed")
mouth.speakBlocking(u"Echo echo echo")
mouth.speakBlocking(u"What should I use")

mouth.setVolume(0.7)
mouth.speakBlocking("Silent please")
mouth.setVolume(1.0)
#speak!
# this not blocks speaking and next line is executed immediatly
mouth.speak(u"Happy birthday Kyle")