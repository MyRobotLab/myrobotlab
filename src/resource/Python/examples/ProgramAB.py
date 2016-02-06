# create a ProgramAB service and start a session
alice = Runtime.createAndStart("alice", "ProgramAB")
alice.startSession()

# create a Speech service
mouth = Runtime.createAndStart("mouth", "Speech")

# create a route which sends published Responses to the
# mouth.speak(String) method
alice.addTextListener(mouth)

print alice.getResponse("How are you?")
alice.getResponse("What is new?")
alice.getResponse("Tell me a joke?")
alice.getResponse("What time is it?")
alice.getResponse("What day is it?")
alice.getResponse("What is todays date?")
alice.getResponse("What is my name?")
alice.getResponse("my name is Batman")
alice.getResponse("What is my name?")
alice.getResponse("my name is Batman")
alice.getResponse("you are a good robot")
alice.getResponse("Goodbye")
