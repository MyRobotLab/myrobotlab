#########################################
# IntegratedMovement.py
# description: a 3D kinematics service supporting D-H parameters
# categories: robot, control
# more info @: http://myrobotlab.org/service/IntegratedMovement
#########################################

#integratedMovement service sample script

from org.myrobotlab.kinematics import DHLinkType
from org.myrobotlab.service.IntegratedMovement import Ai
from com.jme3.math import Vector3f
from java.lang import Math

#start InMoov

leftPort = "COM3"
rightPort = "COM4"
vleft = Runtime.createAndStart("vleft","VirtualArduino")
vright = Runtime.createAndStart("vright","VirtualArduino")
vleft.connect(leftPort)
vright.connect(rightPort)


i01 = Runtime.createAndStart("i01","InMoov")

i01.startAll(leftPort,rightPort)

#configure servo

i01.torso.topStom.map(80,100,92,118)
i01.torso.midStom.map(15,165,148,38)
i01.setTorsoVelocity(13.0,13.0,13.0)

i01.leftArm.bicep.map(5,60,5,80)
i01.leftArm.rotate.map(46,160,46,160)
i01.leftArm.shoulder.map(0,180,0,180)
i01.leftArm.omoplate.map(10,70,10,70)
i01.setArmVelocity("left",26,18,14,15)

i01.rightArm.bicep.map(5,60,5,80)
i01.rightArm.rotate.map(46,160,46,160)
i01.rightArm.shoulder.map(0,180,0,180)
i01.rightArm.omoplate.map(10,70,10,70)
i01.setArmVelocity("right",26,18,14,15)

#startIntegratedMovement Service

im = Runtime.createAndStart("im","IntegratedMovement")

#set the DH parameters for the ik service

im.setNewDHRobotArm("leftArm")
im.setDHLink("leftArm",i01.torso.midStom,113,90,0,-90)
im.setDHLink("leftArm",i01.torso.topStom,0,180,292,90)
im.setDHLink("leftArm","rightS",143,180,0,90) #this is a link not attached to a servo (fixed link)
im.setDHLink("leftArm",i01.leftArm.omoplate,0,-5.6,45,-90)
im.setDHLink("leftArm",i01.leftArm.shoulder,77,60,0,90)
im.setDHLink("leftArm",i01.leftArm.rotate,284,90,40,90)
im.setDHLink("leftArm",i01.leftArm.bicep,0,104.4,300,0)
im.setDHLink("leftArm",i01.leftHand.wrist,0,-90,0,0)
im.setDHLinkType("i01.leftHand.wrist",DHLinkType.REVOLUTE_ALPHA)
im.setDHLink("leftArm","wristup",0,-5,110,0)
im.setDHLink("leftArm","wristdown",0,0,105,45)
im.setDHLink("leftArm","finger",5,-90,5,0)

im.startEngine("leftArm")

im.setNewDHRobotArm("rightArm")
im.setDHLink("rightArm",i01.torso.midStom,113,90,0,-90)
im.setDHLink("rightArm",i01.torso.topStom,0,180,292,90)
im.setDHLink("rightArm","leftS",-143,180,0,-90) #this is a link not attached to a servo (fixed link)
im.setDHLink("rightArm",i01.rightArm.omoplate,0,-5.6,45,90)
im.setDHLink("rightArm",i01.rightArm.shoulder,-77,60,0,-90)
im.setDHLink("rightArm",i01.rightArm.rotate,-284,90,40,-90)
im.setDHLink("rightArm",i01.rightArm.bicep,0,104.4,300,0)
im.setDHLink("rightArm",i01.rightHand.wrist,0,-90,0,0)
im.setDHLinkType("i01.rightHand.wrist",DHLinkType.REVOLUTE_ALPHA)
im.setDHLink("rightArm","Rwristup",0,5,110,0)
im.setDHLink("rightArm","Rwristdown",0,0,105,-45)
im.setDHLink("rightArm","Rfinger",5,90,5,0)

im.startEngine("rightArm")

im.setNewDHRobotArm("kinect")
im.setDHLink("kinect",i01.torso.midStom,113,90,0,-90)
im.setDHLink("kinect",i01.torso.topStom,0,180,110,-90)
im.setDHLink("kinect","camera",0,90,10,90)

im.startEngine("kinect")

#this part define the DH link to be used as a physical object to be used in collision detection

im.clearObject()
im.addObject(0.0, 0.0, 0.0, 0.0, 0.0, -150.0, "base", 150.0, False) #(orix, oriy, oriz, endx, endy, endz, name, radius, showInJMonkey)
im.addObject("i01.torso.midStom", 150.0) #(name, radius)
im.addObject("i01.torso.topStom",10.0)
im.addObject("i01.leftArm.omoplate",10.0)
im.addObject("i01.rightArm.omoplate",10.0)
im.addObject("i01.leftArm.shoulder",50.0)
im.addObject("i01.rightArm.shoulder",50.0)
im.addObject("i01.leftArm.rotate",50.0)
im.addObject("i01.rightArm.rotate",50.0)
im.addObject("i01.leftArm.bicep",60.0)
im.addObject("i01.rightArm.bicep",60.0)
im.addObject("i01.leftHand.wrist",10.0)
im.addObject("i01.rightHand.wrist",10.0)
im.addObject("rightS",10.0)
im.addObject("leftS",10.0)
im.addObject("wristup",70.0)
im.addObject("wristdown",70.0)
im.addObject("Rwristup",70,0)
im.addObject("Rwristdown",70.0)

#this part set object to not consider each other in the collision detection

im.objectAddIgnore("i01.leftArm.bicep","wristup")
im.objectAddIgnore("i01.rightArm.bicep","Rwristup")
im.objectAddIgnore("leftS","rightS")
im.objectAddIgnore("i01.leftArm.omoplate","i01.leftArm.rotate")
im.objectAddIgnore("i01.rightArm.omoplate","i01.rightArm.rotate")
im.objectAddIgnore("rightS","i01.leftArm.shoulder")
im.objectAddIgnore("leftS","i01.rightArm.shoulder")

sleep(1)

#you can also add virtual item to display in the robot environement

im.addObject(0,700,300,0,700,150,"beer",30,True)

#set the JMonkey IntegratedMovement app

im.visualize()
im.jmeApp.addPart("ltorso","Models/ltorso.j3o",1,None,Vector3f(0,0,0), Vector3f.UNIT_X.mult(1),Math.toRadians(0))
im.jmeApp.addPart("i01.torso.midStom","Models/mtorso.j3o",1,None,Vector3f(0,0,0), Vector3f.UNIT_Y.mult(-1),Math.toRadians(-90))
im.jmeApp.addPart("i01.torso.topStom","Models/ttorso1.j3o",1,"i01.torso.midStom",Vector3f(0,105,0), Vector3f.UNIT_Z,Math.toRadians(-90))
im.jmeApp.addPart("rightS",None,1,"i01.torso.topStom",Vector3f(0,300,0), Vector3f.UNIT_Z,Math.toRadians(0))
im.jmeApp.addPart("i01.rightArm.omoplate","Models/Romoplate1.j3o",1,"rightS",Vector3f(-143,0,-17), Vector3f.UNIT_Y.mult(-1),Math.toRadians(-4))
im.jmeApp.addPart("i01.rightArm.shoulder","Models/Rshoulder1.j3o",1,"i01.rightArm.omoplate",Vector3f(-23,-45,0), Vector3f.UNIT_X.mult(-1),Math.toRadians(-32))
im.jmeApp.addPart("i01.rightArm.rotate","Models/rotate1.j3o",1,"i01.rightArm.shoulder",Vector3f(-57,-55,8), Vector3f.UNIT_Y.mult(-1),Math.toRadians(-90))
im.jmeApp.addPart("i01.rightArm.bicep","Models/Rbicep1.j3o",1,"i01.rightArm.rotate",Vector3f(5,-225,-32), Vector3f.UNIT_X.mult(-1),Math.toRadians(20))
im.jmeApp.addPart("leftS",None,1,"i01.torso.topStom",Vector3f(0,300,0), Vector3f.UNIT_Z,Math.toRadians(0))
im.jmeApp.addPart("i01.leftArm.omoplate","Models/Lomoplate1.j3o",1,"leftS",Vector3f(143,0,-15), Vector3f.UNIT_Z.mult(1),Math.toRadians(-6))
im.jmeApp.addPart("i01.leftArm.shoulder","Models/Lshoulder.j3o",1,"i01.leftArm.omoplate",Vector3f(17,-45,5), Vector3f.UNIT_X.mult(-1),Math.toRadians(-30))
im.jmeApp.addPart("i01.leftArm.rotate","Models/rotate1.j3o",1,"i01.leftArm.shoulder",Vector3f(65,-58,-3), Vector3f.UNIT_Y.mult(1),Math.toRadians(-90))
im.jmeApp.addPart("i01.leftArm.bicep","Models/Lbicep.j3o",1,"i01.leftArm.rotate",Vector3f(-14,-223,-28), Vector3f.UNIT_X.mult(-1),Math.toRadians(17))
im.jmeApp.addPart("i01.rightHand.wrist","Models/RWristFinger.j3o",1,"i01.rightArm.bicep",Vector3f(15,-290,-10), Vector3f.UNIT_Y.mult(-1),Math.toRadians(180))
im.jmeApp.addPart("i01.leftHand,wrist","Models/LWristFinger.j3o",1,"i01.leftArm.bicep",Vector3f(0,-290,-20), Vector3f.UNIT_Y.mult(1),Math.toRadians(180))
im.jmeApp.addPart("neck","Models/neck.j3o",1,"i01.torso.topStom",Vector3f(0,452.5,-45), Vector3f.UNIT_X.mult(-1),Math.toRadians(0))
im.jmeApp.addPart("neckroll",None,1,"neck",Vector3f(0,0,0), Vector3f.UNIT_Z.mult(1),Math.toRadians(2))
im.jmeApp.addPart("head","Models/head.j3o",1,"neckroll",Vector3f(0,10,20), Vector3f.UNIT_Y.mult(-1),Math.toRadians(0))
im.jmeApp.addPart("jaw","Models/jaw.j3o",1,"head",Vector3f(-5,63,-50), Vector3f.UNIT_X.mult(-1),Math.toRadians(0))






i01.torso.midStom.moveTo(91)
i01.torso.topStom.moveTo(91)
i01.leftArm.moveTo(6,91,31,11)
i01.rightArm.moveTo(6,91,31,11)
i01.leftHand.wrist.moveTo(91)
i01.rightHand.wrist.moveTo(91)

i01.rest()

im.removeAi("kinect",Ai.AVOID_COLLISION)

