#########################################
# MimicSpeech.py
# description: Speech synthesis based on Mimic from the MyCroft AI project.
# categories: speech, sound
# more info @: http://myrobotlab.org/service/MimicSpeech
#########################################

# start the service
mouth = runtime.start('mouth','MimicSpeech')

#possible voices ( selected voice is stored inside config until you change it )
print ("these are the voices I can have", mouth.getVoices())
print ("this is the voice I am using", mouth.getVoice())

mouth.speakBlocking('hello, this is mimic speech from mycroft project')
mouth.speakBlocking('I am a speech synthesis program')
mouth.speakBlocking('How was that ?')
mouth.speakBlocking('can someone fix my list voices, i think its broke. Oh thanks, fixed now')

#mouth.setVoice('Henry')
mouth.setVolume(0.7)
mouth.speakBlocking("Silent please")
