audiocapture = runtime.start("audiocapture","AudioCapture")
#it starts capturing audio
audiocapture.captureAudio()
# it will record for 5 seconds
sleep(5)
#then it stops recording audio
audiocapture.stopAudioCapture()
#it plays audio recorded
audiocapture.playAudio()

sleep(5)

# setting the recording audio format
# 8000,11025,16000,22050,44100
sampleRate = 16000;
# 8 bit or 16 bit
sampleSizeInBits = 16;
# 1 or 2 channels
channels = 1;
# bits are signed or unsigned
bitSigned = True;
# bigEndian or littleEndian
bigEndian = False;

# setting audio format     bitrate sample
audiocapture.setAudioFormat(sampleRate, sampleSizeInBits, channels, bitSigned, bigEndian)

#it starts capturing audio
audiocapture.captureAudio()
# it will record for 5 seconds
sleep(5)
#then it stops recording audio
audiocapture.stopAudioCapture()
#it plays audio recorded
audiocapture.playAudio()

# save last capture
audiocapture.saveAudioFile("mycapture.wav");
