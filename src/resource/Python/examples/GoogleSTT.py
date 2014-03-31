#create a googlestt service named googlestt
googlestt = Runtime.createAndStart("googlestt","GoogleSTT")
googlestt.captureAudio();

# look in the Java log for recognized text