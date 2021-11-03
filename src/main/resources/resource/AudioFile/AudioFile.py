#########################################
# AudioFile.py
# more info @: http://myrobotlab.org/service/AudioFile
#########################################
 
# start the services
audioPlayer = Runtime.start("audioPlayer", "AudioFile")

# play an internet accessable file - first time will take a while
# but it will be cached in data/Audiofile/cache
audioPlayer.play("https://ia802508.us.archive.org/5/items/testmp3testfile/mpthreetest.mp3")

print('listen for a couple seconds')
sleep(5)

print('pause for a couple seconds')
audioPlayer.pause()
sleep(5)

print('resume')
audioPlayer.resume()
sleep(5)

# print the current track we are using
print('current track is', audioPlayer.getTrack())

# go to "default" track
print('setting to default track', audioPlayer.setTrack())

# volume value between 0.0 and 1.0
audioPlayer.setVolume(0.8)

# a couple resource files exist for this service
# they can be played directly
audioPlayer.playResource("R2D2e.wav")

# these methods do not block so sleep here so
# you can hear what 80% volume sounds like
print('queue 2 files and wait 5 seconds listening to the queued files')
sleep(5)

print('resetting to full volume')
audioPlayer.setVolume(1.0)

# or copied where we want to play them
audioPlayer.copyResource("R2D2e.wav", "data/R2D2e.wav")
audioPlayer.copyResource("good_evening.wav", "data/good_evening.wav")
audioPlayer.copyResource("well.wav", "data/well.wav")

# now we can play them just as we can play 
# any file on the file system
# e.g. audiofile.playFile("c:/sounds/beep.mp3")
print('start playing a new file')
audioPlayer.play("data/well.wav")
sleep(1)

# pause the default track
print('pausing track')
audioPlayer.pause()
sleep(2)

# resume the default track
print('resuming track')
audioPlayer.resume()
sleep(4)

# create a new track named "priority"
# and have the short beeps repeat 8 times
audioPlayer.setTrack("priority")
audioPlayer.repeat("data/well.wav", 8)


# make a new beep track and play r2d2 noises
audioPlayer.setTrack("beepTrack")
audioPlayer.repeat("data/R2D2e.wav")

# go back to the default track
audioPlayer.setTrack("default")
audioPlayer.repeat("data/good_evening.wav", 3)

# hold here in the script for 5 seconds
# everything should be playing simultaneously
sleep(5)

# the repeat without number repeats until 
# we go back to the track and stop it
# move to the beep track
audioPlayer.setTrack("beepTrack")
# stop the beep track
audioPlayer.stop()

print('silencing all')
# silence all 
audioPlayer.silence()

# add a named playlist
# in this case we add a directory or file to our list
audioPlayer.addPlaylist('my list', 'data')

# then we can play the list - it will default to play on its own track
# so you can run more sounds or voices in the foreground

# start my playlist in the background
audioPlayer.startPlaylist('my list')

# repeat good evening in the foreground 3 times
audioPlayer.repeat("data/good_evening.wav", 3)

# silence everything
audioPlayer.silence()
