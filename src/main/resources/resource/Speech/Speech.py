#########################################
# Speech.py
# description: used as a general template
# categories: general
# more info @: http://myrobotlab.org/service/Speech
#########################################

# start all speech services ( to test them )

# local :
marySpeech = Runtime.start("marySpeech", "MarySpeech")
localSpeech = Runtime.start("localSpeech", "LocalSpeech")
mimicSpeech = Runtime.start("mimicSpeech", "MimicSpeech")

# api needed
polly = Runtime.start("polly", "Polly")
voiceRss = Runtime.start("voiceRss", "VoiceRss")
indianTts = Runtime.start("indianTts", "IndianTts")

# beta :
naturalReaderSpeech = Runtime.start("naturalReaderSpeech", "NaturalReaderSpeech")