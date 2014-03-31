leftPort = "COM7"
headPort = "COM7"

i01 = Runtime.createAndStart("i01", "InMoov")
i01.startHead(leftPort)
i01.startHeadTracking(leftPort)
i01.startEyesTracking(leftPort)

i01.headTracking.faceDetect()
i01.eyesTracking.faceDetect()
i01.headTracking.pyramidDown()