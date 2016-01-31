# Example of how to use the Acapela Speech service
acapelaSpeech = Runtime.createAndStart("speech", "AcapelaSpeech")
acapelaSpeech.speak(u'Répète après moi')
voices = acapelaSpeech.getVoices()
for voice in voices:
    acapelaSpeech.setVoice(voice)
    print(voice)
    acapelaSpeech.speak(u'hello world')
    



