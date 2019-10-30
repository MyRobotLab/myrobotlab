#########################################
# NaturalReaderSpeech.py
# description: Natural Reader based speech service.
# categories: speech
# more info @: http://myrobotlab.org/service/NaturalReaderSpeech
#########################################


# start the service
mouth = Runtime.start("mouth", "NaturalReaderSpeech")

#possible voices ( selected voice is stored inside config until you change it )
print ("these are the voices I can have", mouth.getVoices())
print ("this is the voice I am using", mouth.getVoice())

mouth.setVoice("Seoyeon")
mouth.speakBlocking(u"내 로봇 연구소는 너무 강력하다")

mouth.setVoice("Mathieu")
mouth.speakBlocking(u"Hey, Watson was here!")
#unicode test
mouth.setVoice("Celine")
mouth.speakBlocking(u"coucou les francophones")

mouth.setVolume(0.7)
mouth.speakBlocking("Silent please")