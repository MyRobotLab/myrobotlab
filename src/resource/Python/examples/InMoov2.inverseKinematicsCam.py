inversekinematics = Runtime.createAndStart("inversekinematics", "InverseKinematics")
#insert number of Degrees of Freedom
dof = 2
oldDistance =0
distance =0
z = 0
oldZ = 0
arduino = Runtime.createAndStart("arduino","Arduino")
arduino.connect("COM8")
shoulder	= Runtime.createAndStart("shoulder","Servo")
bicep	= Runtime.createAndStart("bicep","Servo")
arduino.attach(shoulder.getName() , 13)
arduino.attach(bicep.getName(), 12)

shoulder.setInverted(True)
bicep.setInverted(True)

#set number of Degrees of Freedom
inversekinematics.setDOF(dof)
#insert informations about the structure : rods lenght
inversekinematics.setStructure(0,100)
inversekinematics.setStructure(1,110)

# create or get a handle to an OpenCV service
opencv = Runtime.create("opencv","OpenCV")
opencv.startService()
# reduce the size - face tracking doesn't need much detail
# the smaller the faster
opencv.addFilter("PyramidDown1", "PyramidDown")
# add the face detect filter
opencv.addFilter("FaceDetect1", "FaceDetect")
 
def input():
    global oldDistance
    global distance
    global oldZ
    global z
    #print 'found face at (x,y) ', msg_opencv_publishOpenCVData.data[0].x(), msg_opencv_publish.data[0].y()
    opencvData = msg_opencv_publishOpenCVData.data[0]
    if (opencvData.getBoundingBoxArray().size() > 0) :
     rect = opencvData.getBoundingBoxArray().get(0)
     posx = rect.x
     posy = rect.y
     
     w = rect.width
     h = rect.height
     print w
     distance = ((((0.15- w )/0.65)*210)+210)
     z = ((-1*(posy*100))+50)
     print 'z is ' , z
     print 'distance is ' , distance
     diffD = abs(oldDistance - distance)
     diffZ = abs(oldZ - z)
     if (diffD > 8 or diffZ >5):
         
      inversekinematics.setPoint(distance,0,z)
      #start the engine
      inversekinematics.compute()
      #print base angle
      base = inversekinematics.getBaseAngle()
      print 'Base Angle is :' , base

      shoulderAngle = int(90 + inversekinematics.getArmAngles(0))
      print shoulderAngle
      shoulder.moveTo(shoulderAngle)
  
      bicepAngle = int(inversekinematics.getArmAngles(1) - inversekinematics.getArmAngles(0))
      print bicepAngle
      bicep.moveTo(bicepAngle)
      oldDistance = distance
      return object

     
     return object

# create a message route from opencv to python so we can see the coordinate locations
opencv.addListener("publishOpenCVData", python.name, "input");

opencv.setCameraIndex(1)

opencv.capture()
