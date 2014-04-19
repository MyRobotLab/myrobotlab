headPort="COM6"
i01 = Runtime.createAndStart("i01", "InMoov")
i01.startMouth()
tracker = i01.startHeadTracking(headPort)
i01.headTracking.xpid.setPID(15.0,5.0,0.1)
i01.headTracking.ypid.setPID(20.0,5.0,0.1)
tracker.startLKTracking()