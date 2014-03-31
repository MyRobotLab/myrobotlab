headPort = "COM15"

i01 = Runtime.createAndStart("i01", "InMoov")
neck = i01.startHeadTracking(headPort)

# LK tracking will track a point set in a 
# video feed
neck.startLKTracking()

# face "detection" is not really tracking but it works
# like tracking since its configured to find the
# first & biggest face - making it pretty fast
#neck.faceDetect()

# records a single frame from the display
#print i01.opencv.recordSingleFrame()