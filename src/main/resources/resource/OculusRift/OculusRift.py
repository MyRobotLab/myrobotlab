# start the service
oculusrift = Runtime.start("oculusrift","OculusRift")

leftEyeURL = "http://10.0.0.2:8080/?action=stream"
rightEyeURL = "http://10.0.0.2:8081/?action=stream"
    
rift.setLeftEyeURL(leftEyeURL)
rift.setRightEyeURL(rightEyeURL)
    
rift.leftCameraAngle = 0
rift.leftCameraDy = 5
rift.rightCameraDy = -5
#// call this once you've updated the affine stuff?
rift.updateAffine()

rift.initContext()

rift.logOrientation()

# TODO: fix this script up.
