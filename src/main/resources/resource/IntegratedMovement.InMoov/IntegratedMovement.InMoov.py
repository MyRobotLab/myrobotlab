#start the InMoov
i01 = Runtime.createAndStart("i01","InMoov")
i01.startAll("COM5","COM6")

#start IntegratedMovement()
i01.startIntegratedMovement()

#move arms at a specific point
i01.integratedMovement.moveTo("leftArm",300,400,500)

#start the kinect
i01.integratedMovement.startOpenNI()

#use the kinect to take a snapshot of the surrounding and process the data into the inMoov 3D space
i01.integratedMovement.processKinectData()

#use the SwingGui for easier interraction with objects
