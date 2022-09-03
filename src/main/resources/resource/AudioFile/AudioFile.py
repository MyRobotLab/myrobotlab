#########################################
# AudioFile.py
# more info @: http://myrobotlab.org/service/AudioFile
#########################################
 
# start the services
player = runtime.start("player", "AudioFile")

# play an internet accessable file - first time will take a while
# but it will be cached in data/Audiofile/cache
player.play("https://upload.wikimedia.org/wikipedia/commons/1/1f/Bach_-_Brandenburg_Concerto.No.1_in_F_Major-_II._Adagio.ogg")

print('listen for a couple seconds')
sleep(5)

print('pause for a couple seconds')
player.pause()
sleep(5)

print('resume')
player.resume()
sleep(5)

# print the current track we are using
print('current track is', player.getTrack())

# go to "default" track
print('setting to default track', player.setTrack())

# volume value between 0.0 and 1.0
player.setVolume(0.8)

# a couple resource files exist for this service
# they can be played directly
player.playResource("R2D2e.wav")

# these methods do not block so sleep here so
# you can hear what 80% volume sounds like
print('queue 2 files and wait 5 seconds listening to the queued files')
sleep(5)

print('resetting to full volume')
player.setVolume(1.0)

# or copied where we want to play them
player.copyResource("R2D2e.wav", "data/R2D2e.wav")
player.copyResource("good_evening.wav", "data/good_evening.wav")
player.copyResource("well.wav", "data/well.wav")

# now we can play them just as we can play 
# any file on the file system
# e.g. audiofile.playFile("c:/sounds/beep.mp3")
print('start playing a new file')
player.play("data/well.wav")
sleep(1)

# pause the default track
print('pausing track')
player.pause()
sleep(2)

# resume the default track
print('resuming track')
player.resume()
sleep(4)

# create a new track named "priority"
# and have the short beeps repeat 8 times
player.setTrack("priority")
player.repeat("data/well.wav", 8)


# make a new beep track and play r2d2 noises
player.setTrack("beepTrack")
player.repeat("data/R2D2e.wav")

# go back to the default track
player.setTrack("default")
player.repeat("data/good_evening.wav", 3)

# hold here in the script for 5 seconds
# everything should be playing simultaneously
sleep(5)

# the repeat without number repeats until 
# we go back to the track and stop it
# move to the beep track
player.setTrack("beepTrack")
# stop the beep track
player.stop()

print('silencing all')
# silence all 
player.silence()

# add a named playlist
# in this case we add a directory or file to our list
player.addPlaylist('my list', 'data')

# then we can play the list - it will default to play on its own track
# so you can run more sounds or voices in the foreground

# start my playlist in the background
player.startPlaylist('my list')

# repeat good evening in the foreground 3 times
player.repeat("data/good_evening.wav", 3)

# silence everything
player.silence()

sleep(1)
player.resume()

######################################
# Robot mouth control
# you can have a Servo service subscribe to an 
# AudioFile service and publish from peak loudness
# to the servos moveTo method

mouthServo = runtime.start("mouthServo", "Servo")
mouthServo.setPin('5')
mega = runtime.start("mega", "Arduino")
mega.connect('/dev/ttyACM0')
mega.attach('mouthServo')


# connect servo to Arduino, Adafruit or some other driver

# subscribe the servo to the peak loudness 
mouthServo.subscribe('player', 'publishPeak', 'mouthServo', 'moveTo')

player.setMute(False)
player.resume()

# check AudioFile configuration in audioFile.yml for other values to tweak
player.play('https://upload.wikimedia.org/wikipedia/commons/c/c7/An_Unfinished_Story.ogg')

