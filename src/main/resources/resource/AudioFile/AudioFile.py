#########################################
# AudioFile.py
# more info @: http://myrobotlab.org/service/AudioFile
#########################################
 
# start the services
audiocapture = Runtime.start("audiocapture","AudioCapture")
audiofile = Runtime.start("audiofile", "AudioFile")

# it starts capturing audio
audiocapture.captureAudio()
# it will record for 5 seconds
sleep(5)
# save last capture
audiocapture.save("mycapture.wav");

# audiofile.playFile("c:/sounds/beep.mp3")
audiofile.playFile("mycapture.wav")

from org.myrobotlab.image import Util
audiofile.setVolume(1)
audiofile.playFileBlocking(Util.getRessourceDir()+"ServoOrchestrator/click.wav")
audiofile.setVolume(0.8)
audiofile.playFileBlocking(Util.getRessourceDir()+"ServoOrchestrator/click.wav")