# start the service
oculusRift = runtime.start("oculusRift","OculusRift")

leftEyeURL = "http://10.0.0.2:8080/?action=stream"
rightEyeURL = "http://10.0.0.2:8081/?action=stream"
    
oculusRift.setLeftEyeURL(leftEyeURL)
oculusRift.setRightEyeURL(rightEyeURL)
    
oculusRift.leftCameraAngle = 0
oculusRift.leftCameraDy = 5
rift.rightCameraDy = -5
#// call this once you've updated the affine stuff?
oculusRift.updateAffine()

oculusRift.initContext()

oculusRift.logOrientation()

# TODO: fix this script up.
