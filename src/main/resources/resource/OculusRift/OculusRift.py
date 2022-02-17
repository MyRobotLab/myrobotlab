# start the service
oculusrift = Runtime.start("oculusrift","OculusRift")

leftEyeURL = "http://10.0.0.2:8080/?action=stream"
rightEyeURL = "http://10.0.0.2:8081/?action=stream"
    
oculusrift.setLeftEyeURL(leftEyeURL)
oculusrift.setRightEyeURL(rightEyeURL)
    
oculusrift.leftCameraAngle = 0
oculusrift.leftCameraDy = 5
rift.rightCameraDy = -5
#// call this once you've updated the affine stuff?
oculusrift.updateAffine()

oculusrift.initContext()

oculusrift.logOrientation()

# TODO: fix this script up.
