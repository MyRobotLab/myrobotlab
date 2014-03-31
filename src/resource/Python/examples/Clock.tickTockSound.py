# IMPORTANT! Since AudioFile (http://myrobotlab.org/service/AudioFile)
# has other dependencies it must be "installed" be sure to 
# install the AudioFile service otherwise you will not be able to play
# audio files

#create a clock service named clock
clock = Runtime.createAndStart("clock","Clock")
#create an audio file player service named audio
audio = Runtime.createAndStart("audio","AudioFile")
def ticktock():
    #if tick.mp3 is not in the main folder (myrobotlab)
    #it should be replaced with the full file-path eg. "C:\\myrobotlab\\src\\resource\\Clock\\tick.mp3"
    #audio.playFile("tick.mp3")
    audio.playResource("/resource/Clock/tick.mp3")
#create a message between clock and audio, so a tick tock sound could be played
clock.addListener("pulse", python.name, "ticktock")
#start clock
clock.startClock()