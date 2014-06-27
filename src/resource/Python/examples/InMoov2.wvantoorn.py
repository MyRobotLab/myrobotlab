# this script is provided as a basic guide
# most parts can be run by uncommenting them
# InMoov now can be started in modular pieces

leftPort = "COM7"
rightPort = "COM9"

i01 = Runtime.createAndStart("i01", "InMoov")

rightArm = i01.startRightArm(rightPort)
mouth = i01.startMouth()
ear = i01.startEar()

# i01.systemCheck()

ear = i01.ear

# commands with i01.getName() use the InMoov service methods
ear.addCommand("attach", i01.getName(), "attach")
ear.addCommand("detach", i01.getName(), "detach")
ear.addCommand("rest", i01.getName(), "rest")

ear.addCommand("power down", i01.getName(), "powerDown")
ear.addCommand("power up", i01.getName(), "powerUp")

ear.addCommand("track", i01.getName(), "track")
ear.addCommand("freeze track", i01.getName(), "clearTrackingPoints")
ear.addCommand("how many fingers do you have", "python", "howmanyfingersdoihave")

ear.addComfirmations("yes","correct","ya") 
ear.addNegations("no","wrong","nope","nah")
 
ear.startListening()

def howmanyfingersdoihave():
     ear.lockOutAllGrammarExcept("voice control")
     sleep(1)
     i01.moveHead(49,74)
     i01.moveArm("left",75,83,79,24)
     i01.moveArm("right",65,82,71,24)
     i01.moveHand("left",74,140,150,157,168,92)
     i01.moveHand("right",89,80,98,120,114,0)
     sleep(2)
     i01.moveHand("right",0,80,98,120,114,0)
     i01.mouth.speakBlocking("ten")
    
     sleep(.1)
     i01.moveHand("right",0,0,98,120,114,0)
     i01.mouth.speakBlocking("nine")
    
     sleep(.1)
     i01.moveHand("right",0,0,0,120,114,0)
     i01.mouth.speakBlocking("eight")
    
     sleep(.1)
     i01.moveHand("right",0,0,0,0,114,0)
     i01.mouth.speakBlocking("seven")
    
     sleep(.1)
     i01.moveHand("right",0,0,0,0,0,0)
     i01.mouth.speakBlocking("six")
    
     sleep(.5)
     i01.setHeadSpeed(.70,.70)
     i01.moveHead(40,105)
     i01.moveArm("left",75,83,79,24)
     i01.moveArm("right",65,82,71,24)
     i01.moveHand("left",0,0,0,0,0,180)
     i01.moveHand("right",0,0,0,0,0,0)
     sleep(0.1)
     i01.mouth.speakBlocking("and five makes eleven")
    
     sleep(0.7)
     i01.setHeadSpeed(0.7,0.7)
     i01.moveHead(40,50)
     sleep(0.5)
     i01.setHeadSpeed(0.7,0.7)
     i01.moveHead(49,105)
     sleep(0.7)
     i01.setHeadSpeed(0.7,0.8)
     i01.moveHead(40,50)
     sleep(0.7)
     i01.setHeadSpeed(0.7,0.8)
     i01.moveHead(49,105)
     sleep(0.7)
     i01.setHeadSpeed(0.7,0.7)
     i01.moveHead(90,85)
     sleep(0.7)
     i01.mouth.speakBlocking("eleven")
     i01.moveArm("left",70,75,70,20)
     i01.moveArm("right",60,75,65,20)
     sleep(1)
     i01.mouth.speakBlocking("that doesn't seem right")
     sleep(2)
     i01.mouth.speakBlocking("I think I better try that again")
    
     i01.moveHead(40,105)
     i01.moveArm("left",75,83,79,24)
     i01.moveArm("right",65,82,71,24)
     i01.moveHand("left",140,168,168,168,158,90)
     i01.moveHand("right",87,138,109,168,158,25)
     sleep(2)
    
     i01.moveHand("left",10,140,168,168,158,90)
     i01.mouth.speakBlocking("one")
     sleep(.1)
    
    
     i01.moveHand("left",10,10,168,168,158,90)
     i01.mouth.speakBlocking("two")
     sleep(.1)
    
     i01.moveHand("left",10,10,10,168,158,90)
     i01.mouth.speakBlocking("three")
     sleep(.1)
     i01.moveHand("left",10,10,10,10,158,90)
    
     i01.mouth.speakBlocking("four")
     sleep(.1)
     i01.moveHand("left",10,10,10,10,10,90)
    
     i01.mouth.speakBlocking("five")
     sleep(.1)
     i01.setHeadSpeed(0.65,0.65)
     i01.moveHead(53,65)
     i01.moveArm("right",48,80,78,10)
     i01.setHandSpeed("left", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
     i01.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
     i01.moveHand("left",10,10,10,10,10,90)
     i01.moveHand("right",10,10,10,10,10,25)
     sleep(1)
     i01.mouth.speakBlocking("and five makes ten")
     sleep(.5)
     i01.mouth.speakBlocking("there that's better")
     i01.moveHead(95,85)
     i01.moveArm("left",75,83,79,24)
     i01.moveArm("right",40,70,70,10)
     sleep(0.5)
     i01.mouth.speakBlocking("i01 has ten fingers")
     sleep(0.5)
     i01.moveHead(90,90)
     i01.setHandSpeed("left", 0.8, 0.8, 0.8, 0.8, 0.8, 0.8)
     i01.setHandSpeed("right", 0.8, 0.8, 0.8, 0.8, 0.8, 0.8)
     i01.moveHand("left",140,140,140,140,140,60)
     i01.moveHand("right",140,140,140,140,140,60)
     sleep(1.0)
     i01.setArmSpeed("right", 1.0, 1.0, 1.0, 1.0)
     i01.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
     i01.moveArm("left",0,90,30,10)
     i01.moveArm("right",0,90,30,10)
     sleep(0.5)
     further()
     sleep(0.5)
 
     ear.clearLock()

