#########################################
# LocalSpeech.py
# description: used as a general template
# categories: speech
# more info @: http://myrobotlab.org/service/LocalSpeech
#########################################
import os
import platform

# start the service
mouth = runtime.start('mouth','LocalSpeech')

# by default LocalSpeech will select a command template appropriate for the current 
# operating system
mouth.speakBlocking("this is how I sound out of the box")

# possible voices ( selected voice is stored inside config until you change it )
mouth.speakBlocking("these are the voices I can have " + str(mouth.getVoices()))
mouth.speakBlocking ("this is the voice I am using " + str(mouth.getVoice()))

if (Runtime.getPlatform().isLinux()):
    mouth.speakBlocking('I appear to be on a Linux system')
    mouth.speakBlocking('I can also use espeak for speech')
    mouth.setEspeak()
    mouth.speakBlocking('this is what espeak sounds like')

if (Runtime.getPlatform().isWindows()):
    mouth.speakBlocking('I appear to be on a Windows system')
    mouth.speakBlocking('I can also use mimic for speech')
    mouth.setMimic()
    mouth.speakBlocking('this is how mimic speech sounds')

if (Runtime.getPlatform().isMac()):
    mouth.speakBlocking('I appear to be on a Mac OS system')

# set your voice from macos control panel
# you can test it using say command from terminal

# mouth.setVoice("Microsoft Zira Desktop - English (United States)")
mouth.speakBlocking(u"Hello this is an english voice")
mouth.speakBlocking(u"Bonjour ceci est une voix française, je teste les accents aussi avec le mot éléphant")

mouth.setVolume(0.7)
mouth.speakBlocking("I have lowered my volume")
mouth.speakBlocking("Silent please")
mouth.setMute(True)
mouth.speakBlocking("you cannot hear me")
mouth.setMute(False)