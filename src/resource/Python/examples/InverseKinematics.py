inversekinematics = Runtime.createAndStart("inversekinematics", "InverseKinematics")
#insert number of Degrees of Freedom
dof = 3
#set number of Degrees of Freedom
inversekinematics.setDOF(dof)
#insert informations about the structure : rods lenght
inversekinematics.setStructure(0,100)
inversekinematics.setStructure(1,100)
inversekinematics.setStructure(2,100)
# insert coordinates of the point to reach (x,y,z)
inversekinematics.setPoint(200,200,200)
#start the engine
inversekinematics.compute()
#print base angle
base = inversekinematics.getBaseAngle()
print 'Base Angle is :' , base
#print arm angles
for i in range(dof):
  print 'Angle ',i,' is :', inversekinematics.getArmAngles(i)
