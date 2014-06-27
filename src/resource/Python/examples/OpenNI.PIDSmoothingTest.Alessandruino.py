openni = Runtime.createAndStart("openni","OpenNI")
openni.startUserTracking()
i01 = Runtime.createAndStart("i01","InMoov")
leftArm = i01.startLeftArm("COM8")


openni.addListener("publish",python.name,"input")

global actualhand
actualhand = 0

xpid = Runtime.createAndStart("xpid","PID")
xpid.setMode(1)
xpid.setOutputRange(-1, 1)
xpid.setPID(10.0, 0, 1.0)
xpid.setControllerDirection(0)


def input():
 skeleton = msg_openni_publish.data[0]
 hand = abs(skeleton.rightHand.x)
 print 'hand is', hand
 global actualhand
 xpid.setSetpoint(hand) 
 xpid.setInput(actualhand)
 xpid.compute()
 servox = xpid.getOutput()
 actualhand = (actualhand + servox)
 print 'servo is' , actualhand