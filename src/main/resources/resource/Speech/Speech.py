#########################################
# Speech.py
# description: used as a general template
# categories: general
# more info @: http://myrobotlab.org/service/Speech
#########################################

# start all speech services ( to test them )

# local :
marySpeech = runtime.start("marySpeech", "MarySpeech")
localSpeech = runtime.start("localSpeech", "LocalSpeech")
mimicSpeech = runtime.start("mimicSpeech", "MimicSpeech")

# api needed
polly = runtime.start("polly", "Polly")
voiceRss = runtime.start("voiceRss", "VoiceRss")
indianTts = runtime.start("indianTts", "IndianTts")
