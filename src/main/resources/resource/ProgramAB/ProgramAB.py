#########################################
# ProgramAB.py
# more info @: http://myrobotlab.org/service/ProgramAB
#########################################

# create a ProgramAB service and start a session
alice = Runtime.start("alice", "ProgramAB")
# supported language list : https://github.com/MyRobotLab/aiml/tree/master/bots
alice.startSession("username","en-US")

print alice.getResponse("How are you?")

# create a Speech service
mouth = Runtime.start("mouth", "MarySpeech")
# create a route which sends published Responses to the
# mouth.speak(String) method
alice.attach(mouth)

alice.getResponse(u"What is new?")
sleep(3)
alice.getResponse(u"Tell me a joke?")
sleep(3)
alice.getResponse(u"What time is it?")
sleep(3)
# UTF8 test
alice.getResponse(u"こんにちは")