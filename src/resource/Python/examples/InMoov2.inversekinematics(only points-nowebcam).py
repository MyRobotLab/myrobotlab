#file : InMoov2.inversekinematics.py

inversekinematics = Runtime.createAndStart("inversekinematics", "InverseKinematics")
#insert number of Degrees of Freedom
dof = 2
leftPort= "COM7"

i01 = Runtime.createAndStart("i01", "InMoov")
leftArm = i01.startLeftArm(leftPort)
############################################################
#if needed we can tweak the default settings with these lines
i01.leftArm.shoulder.setMinMax(0,180)
i01.leftArm.bicep.setMinMax(0,90)
############################################################
#set number of Degrees of Freedom
inversekinematics.setDOF(dof)
#insert informations about the structure : rods lenght
inversekinematics.setStructure(0,290)
inversekinematics.setStructure(1,475)

for x in range (555,750): 
      print x
      inversekinematics.setPoint(x,0,0)
      #start the engine
      inversekinematics.compute()
      #print base angle
      base = inversekinematics.getBaseAngle()
      print 'Base Angle is :' , base

      leftArmShoulderAngle = int(90 + inversekinematics.getArmAngles(0))
      print 'Shoulder Angle is :' ,leftArmShoulderAngle
      i01.leftArm.shoulder.moveTo(leftArmShoulderAngle)
 
      leftArmBicepAngle = int(inversekinematics.getArmAngles(1) - inversekinematics.getArmAngles(0))
      print 'Bicep Angle is :' , leftArmBicepAngle
      i01.leftArm.bicep.moveTo(leftArmBicepAngle)

      sleep(0.1)
