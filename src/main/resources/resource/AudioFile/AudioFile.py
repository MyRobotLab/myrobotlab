#########################################
# AudioFile.py
# more info @: http://myrobotlab.org/service/AudioFile
#########################################
 
# start the services
audioFile = Runtime.start("audioFile", "AudioFile")

# print the current track we are using
print(audioFile.getTrack())

# go to default track
audioFile.track()

# volume value between 0.0 and 1.0
audioFile.setVolume(0.8)

# a couple resource files exist for this service
# they can be played directly
audioFile.playResource("R2D2e.wav")

# or copied where we want to play them
audioFile.copyResource("R2D2e.wav", "data/R2D2e.wav")
audioFile.copyResource("good_evening.wav", "data/good_evening.wav")
audioFile.copyResource("well.wav", "data/well.wav")

# now we can play them just as we can play 
# any file on the file system
# e.g. audiofile.playFile("c:/sounds/beep.mp3")
audioFile.playFile("data/good_evening.wav")

# pause the default track
audioFile.pause()
sleep(1)

# resume the default track
audioFile.resume()
sleep(4)


# create a new track named "priority"
# and have the short beeps repeat on it
audioFile.track("priority")
audioFile.repeat("data/well.wav", 3)

# make a new beep track and play r2d2 noises
audioFile.track("beepTrack")
audioFile.repeat("data/R2D2e.wav")

# go back to the default track
audioFile.track("default")
audioFile.repeat("data/good_evening.wav", 3)

# hold here in the script for 5 seconds
# everything should be playing simultaneously
sleep(5)

# the repeat without number repeats until 
# we go back to the track and stop it
# move to the beep track
audioFile.track("beepTrack")
# stop the beep track
audioFile.stop()

print('silencing all')
# silence all 
audioFile.silence()
