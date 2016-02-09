# Example of how to use the Acapela Speech service
acapelaSpeech = Runtime.createAndStart("speech", "AcapelaSpeech")
acapelaSpeech.speak("Hello world")
voices = acapelaSpeech.getVoices()
for voice in voices:
    acapelaSpeech.setVoice(voice)
    print(voice)
    acapelaSpeech.speak("Hello world. I'm " + voice)
