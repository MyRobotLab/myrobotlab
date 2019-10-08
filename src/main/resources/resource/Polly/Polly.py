#########################################
# Polly.py
# categories: speech
# more info @: http://myrobotlab.org/service/Polly
#########################################

# start the service
mouth = Runtime.start('mouth','Polly')

#possible voices ( selected voice is stored inside config until you change it )
print ("these are the voices I can have", mouth.getVoices())
print ("this is the voice I am using", mouth.getVoice())


# You sould't not expose keys here !! inside gui is a good place
# But you can do it here ( only once is enough )
# An AES safe is used to store keys
# polly.setKeys("YOUR_KEY_ID","YOUR_KEY_SECRET")
mouth.setLanguage("en")
mouth.setVoice(u"Brian")
mouth.speakBlocking(u"Hello this is Brian speakin !")
mouth.setLanguage("fr")
mouth.setVoice(u"Céline")
mouth.speakBlocking(u"Ceci est une voix française")
mouth.setVolume(0.7)
mouth.speakBlocking("Silent please")