#########################################
# IndianTts.py
# description: used as a general template
# more info @: http://myrobotlab.org/service/IndianTts
#########################################

tts=runtime.start("tts", "IndianTts")

# You sould't not expose keys here !! inside gui is a good place
# But you can do it here ( only once is enough )
# An AES safe is used to store keys
# tts.setKeys("USER_ID","API")

tts.speak(u"नमस्ते भारत मित्र")
