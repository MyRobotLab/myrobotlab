#########################################
# Polly.py
# categories: speech
# more info @: http://myrobotlab.org/service/Polly
#########################################

# start the service
mouth = runtime.start('mouth','Polly')
voices = mouth.getVoices()

def sayHello():
  for voice in voices:
    print(str(voice))
    mouth.setVoice(voice.name)
    mouth.speakBlocking('Hello my name is ' + voice.name)

#possible voices ( selected voice is stored inside config until you change it )

print ("these are the voices I can have", str(voices))
print ("this is the voice I am using", mouth.getVoice())

# if you want them all to say hello
# sayHello()


# You sould't not expose keys here !! inside gui is a good place
# But you can do it here ( only once is enough )
# An AES safe is used to store keys
# polly.setKeys("YOUR_KEY_ID","YOUR_KEY_SECRET")
mouth.setLanguage("en")
mouth.setVoice(u"Brian")
mouth.speakBlocking(u"Hello this is Brian speakin !")
mouth.speakBlocking(u"The polly service has " + str(voices.size()) + " voices to choose from")
mouth.speakBlocking(u"if you want to hear a sample of them all, uncomment the say hello method in this script")
mouth.setLanguage("fr")
mouth.setVoice(u"Chantal")
mouth.speakBlocking(u"Ceci est une voix française")
mouth.setVolume(0.7)
mouth.speakBlocking("Silent please")