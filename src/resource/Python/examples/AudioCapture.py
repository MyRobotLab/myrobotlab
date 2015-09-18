from org.myrobotlab.service import Runtime
from org.myrobotlab.service import AudioCapture
from time import sleep
audiocapture = Runtime.createAndStart("audiocapture","AudioCapture")
#it starts capturing audio
audiocapture.captureAudio()
# it will record for 5 seconds
sleep(5)
#then it stops recording audio
audiocapture.stopAudioCapture()
#it plays audio recorded
audiocapture.playAudio()
