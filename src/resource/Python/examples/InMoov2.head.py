headPort = "COM7"

i01 = Runtime.createAndStart("i01", "InMoov")
head = i01.startHead(headPort)
neck = i01.getHeadTracking()
neck.faceDetect()

eyes = i01.getEyesTracking()
eyes.faceDetect()
