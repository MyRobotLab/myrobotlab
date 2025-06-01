# Some Services do not like anything other than pure text
# while other Services will produce text with markup tags included
# To join (route) the output of a Service with markup tags to one that
# doesn't support the markup tags, we need to filter it.
# Enter the HtmlFilter service

# The most common use for this service is from a chatbot service like 
# ProgramAB to a TTS Service like MarySpeech Service.

# Start the service
htmlfilter = runtime.start("htmlfilter","HtmlFilter")

# Start one of the Text To Speach Service
mouth = runtime.start("mouth", "MarySpeech")

# Start a chatbox service to generate an output
alice2 = runtime.start("alice2", "ProgramAB")

# Load a session into the chatbox
alice2.startSession("user", "alice2")

# Add routing from the the chatbox service to the HtmlFilter service
alice2.addTextListener(htmlfilter)

# Add routing to the TTS service from the htmlfilter
htmlfilter.addTextListener(mouth)
