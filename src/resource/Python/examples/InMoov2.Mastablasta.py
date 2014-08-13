#### PLEASE READ!
#### Part of script Basis14
#### Right hand, right arm (biceps and rotation) and 2 servos for eyes X and Y connected
#### Plays some music and info files
#### Add your own music - earCommand and def of music files is disabled
#### Info files can be found here:
#### earth - http://www.datafilehost.com/d/a56ddcd7
#### energy - http://www.datafilehost.com/d/21f55f8c
#### robot - http://www.datafilehost.com/d/5c220b23
#### solar system - http://www.datafilehost.com/d/4dec36cb
#### sun - http://www.datafilehost.com/d/54ca7461
#### Enjoy and free feel to change! Change his name at line 198 and 200.
#### ->MASTABLASTA<-

import random
rightPort = "COM3"   #### Make sure you have the correct port selected!
leftPort = "COM8"    #### Make sure you have the correct port selected!
 
i01 = Runtime.createAndStart("i01", "InMoov")

i01.startMouth()

i01.mouth.setGoogleURI("http://thehackettfamily.org/Voice_api/api2.php?voice=Ryan&txt=")

helvar = 1
##############
i01.startEar()
################
i01.startHead(leftPort)

i01.head.eyeY.setMinMax(0,180)
i01.head.eyeX.setMinMax(0,180)
i01.head.eyeY.setRest(50)
i01.head.eyeX.setRest(80)
################
i01.startRightHand(rightPort)

i01.rightHand.thumb.setMinMax(10,160)
i01.rightHand.index.setMinMax(10,160)
i01.rightHand.majeure.setMinMax(40,170)
i01.rightHand.ringFinger.setMinMax(40,150)
i01.rightHand.pinky.setMinMax(0,150)
i01.rightHand.wrist.setMinMax(10,170)
i01.rightHand.thumb.map(0,180,55,135)
i01.rightHand.index.map(0,180,0,160)
i01.rightHand.majeure.map(0,180,50,170)
i01.rightHand.ringFinger.map(0,180,48,145)
i01.rightHand.pinky.map(0,180,45,146)
i01.rightHand.wrist.map(0,180,45,145)

i01.moveHand("right",10,40,0,30,0,90)

#################
i01.startRightArm(rightPort)

i01.rightArm.bicep.setMinMax(0,60)
i01.rightArm.rotate.setMinMax(65,150)
#i01.rightArm.shoulder.setMinMax(30,100)
#i01.rightArm.omoplate.setMinMax(10,75)
#################

i01.startEyesTracking(leftPort)
################

opencv = i01.startOpenCV()

i01.eyesTracking.startLKTracking()

i01.eyesTracking.xpid.setPID(12.0,12.0,0.1)
i01.eyesTracking.ypid.setPID(12.0,12.0,0.1)

def input():
    print 'python object is ', msg_clock_pulse
    pin = msg_i01_right_publishPin.data[0]
    print 'pin data is ', pin.pin, pin.value
    if (pin.value == 1):
      i01.powerUp()
      sleep(2)

ear = i01.ear
 
ear.addCommand("rest", "python", "rest")
ear.addCommand("ass hole", "python", "asshole")

ear.addCommand("attach right hand", "i01.rightHand", "attach")
ear.addCommand("disconnect right hand", "i01.rightHand", "detach")
ear.addCommand("attach everything", "i01", "attach")
ear.addCommand("disconnect everything", "i01", "detach")
ear.addCommand("attach right arm", "i01.rightArm", "attach")
ear.addCommand("disconnect right arm", "i01.rightArm", "detach")
ear.addCommand("open hand", "python", "handopen")
ear.addCommand("close hand", "python", "handclose")
ear.addCommand("stop listening", ear.getName(), "stopListening")

ear.addCommand("move right arm left", "python", "rightarmleft")
ear.addCommand("move right arm right", "python", "rightarmright")
ear.addCommand("move right arm up", "python", "rightarmup")
ear.addCommand("move right arm down", "python", "rightarmdown")
ear.addCommand("move right arm left and up", "python", "rightarmleftup")
ear.addCommand("move right arm right and up", "python", "rightarmrightup")
ear.addCommand("move right arm left and down", "python", "rightarmleftdown")
ear.addCommand("move right arm right down", "python", "rightarmrightdown")
ear.addCommand("move right arm center and down", "python", "rightarmcenterdown")
ear.addCommand("move right arm center and up", "python", "rightarmrcenterup")

#### eyes move

ear.addCommand("look straight", "python", "lookstraight")
ear.addCommand("look up", "python", "lookup")
ear.addCommand("look down", "python", "lookdown")
ear.addCommand("look left", "python", "lookleft")
ear.addCommand("look right", "python", "lookright")

#### music - add your own music here

#ear.addCommand("fire water burn", "python", "bloodone")
#ear.addCommand("song two", "python", "songtwo")
#ear.addCommand("ready steady go", "python", "readygo")
#ear.addCommand("sabotage", "python", "sabotage")
#ear.addCommand("so what you want", "python", "whatya")
#ear.addCommand("heroes", "python", "heroes")
#ear.addCommand("long train running", "python", "doobies")
#ear.addCommand("fresh feeling", "python", "eels")
#ear.addCommand("lose yourself", "python", "eminem")
#ear.addCommand("all my life", "python", "foo1")
#ear.addCommand("living in america", "python", "livingamerica")
#ear.addCommand("virtual insanity", "python", "insanity")
#ear.addCommand("breaking the law", "python", "priestlaw")

ear.addCommand("explain energy", "python", "energy")
ear.addCommand("explain sun", "python", "sun")
ear.addCommand("explain robot", "python", "robot")
ear.addCommand("explain earth", "python", "earth")
ear.addCommand("explain solar system", "python", "solarsystem")

#ear.addCommand("track humans", "python", "trackHumans")
#ear.addCommand("track point", "python", "trackPoint")
#ear.addCommand("stop tracking", "python", "stopTracking")

ear.addComfirmations("yes","correct","ya","yeah")
ear.addNegations("no","wrong","nope","nah")

ear.startListening("stop music | how do you do | what is your name | sorry | thanks | thank you | nice | relax | stop playback | music please")

ear.addListener("recognized", "python", "heard")
 
def heard(data):

    if (data == "stop music"):
            i01.mouth.audioFile.silence()

    if (data == "stop playback"):
            i01.mouth.audioFile.silence()

    if (data == "music please"):
            i01.mouth.speak("which song do you want to hear?")

    if (data == "how do you do"):
        if helvar <= 2:    
            i01.mouth.speak("I'm doing fine, thank you")
            global helvar
            helvar += 1
        elif helvar == 3:
            i01.mouth.speak("you are repeating yourself")
            sleep(2)
            global helvar
            helvar += 1
        elif helvar == 4:
            i01.mouth.speak("stop talking the same shit all the time")
            sleep(2)
            global helvar
            helvar += 1
        elif helvar == 5:
            i01.mouth.speak("i will stop talking to you if you ask me this again")
            sleep(4)
            global helvar
            helvar += 1

    if (data == "sorry"):
        x = (random.randint(1, 3))
        if x == 1:
            i01.mouth.speak("no problems")
        if x == 2:
            i01.mouth.speak("it doesn't matter")
        if x == 3:
            i01.mouth.speak("it's okay")

    if (data == "nice"):
        x = (random.randint(1, 2))
        if x == 1:
            i01.mouth.speak("I know")
        if x == 2:
            i01.mouth.speak("yeah isn't it")

    if (data == "what is your name"):
        x = (random.randint(1, 2))
        if x == 1:
            i01.mouth.speak("my name is Huckleberry Finn")
        if x == 2:
            i01.mouth.speak("call me Huckleberry Finn")            

    if (data == "hello"):
        hello()
        relax()    

    if (data == "thank you"):
        x = (random.randint(1, 3))
        if x == 1:
            i01.mouth.speak("you are welcome")
        if x == 2:
            i01.mouth.speak("my pleasure")
        if x == 3:
            i01.mouth.speak("it's okay")

    if (data == "thanks"):
        x = (random.randint(1, 2))
        if x == 1:
            i01.mouth.speak("it's okay")
        if x == 2:
            i01.mouth.speak("sure")

    if (data == "relax"):
        x = (random.randint(1, 3))
        if x == 1:
            i01.mouth.speak("thanks for the break")
            i01.moveHand("right",50,50,50,50,50,90)
            i01.moveArm("right",0,90,0,0)
        if x == 2:
            i01.mouth.speak("thank you, even a robot needs some rest")
            i01.moveHand("right",50,50,50,50,50,90)
            i01.moveArm("right",0,90,0,0)
        if x == 3:
            i01.mouth.speak("great, so I can have a smoke and take a leak")
            i01.moveHand("right",50,50,50,50,50,90)
            i01.moveArm("right",0,90,0,0)

def handopen():
  i01.moveHand("right",10,40,0,30,0,90)

def handclose():
  i01.moveHand("right",150,150,130,150,168,90)
 
def openrighthand():
  i01.moveHand("right",10,40,0,30,0,90)
 
def closerighthand():
  i01.moveHand("right",160,160,130,150,150,90)

def asshole():
  i01.moveHand("right",150,160,0,160,160,140)
  i01.moveArm("right",50,90,0,0)
  sleep(2)
  i01.mouth.speak("this is what you get for saying bad words")

def rightarmleft():
  i01.moveArm("right",0,65,0,0)

def rightarmright():
  i01.moveArm("right",0,150,0,0)

def rightarmup():
  i01.moveArm("right",60,90,0,0)

def rightarmdown():
  i01.moveArm("right",0,90,0,0)

def rightarmleftup():
  i01.moveArm("right",60,65,0,0)

def rightarmrightup():
  i01.moveArm("right",60,150,0,0)

def rightarmleftdown():
  i01.moveArm("right",0,65,0,0)

def rightarmrightdown():
  i01.moveArm("right",0,150,0,0)

def rightarmcenterdown():
  i01.moveArm("right",0,90,0,0)

def rightarmcenterup():
  i01.moveArm("right",60,90,0,0)

def lookstraight():
  i01.setHeadSpeed(0, 0, 0.9, 0.8, 0)
  i01.moveHead(0,0,80,50,0)

def lookup():
  i01.setHeadSpeed(0, 0, 0.9, 0.8, 0)
  i01.moveHead(0,80,80,80,0)

def lookdown():
  i01.setHeadSpeed(0, 0, 0.9, 0.8, 0)
  i01.moveHead(0,0,80,30,0)

def lookleft():
  i01.setHeadSpeed(0, 0, 0.9, 0.8, 0)
  i01.moveHead(0,0,160,50,0)

def lookright():
  i01.setHeadSpeed(0, 0, 0.9, 0.8, 0)
  i01.moveHead(0,0,20,50,0)

#def bloodone():
#  sleep(1)
#  i01.mouth.audioFile.playFile("G:/MUSIC/Singles/Blood Hound Gang - Fire Water Burn.mp3", False)

#def songtwo():
#  sleep(1)
#  i01.mouth.audioFile.playFile("G:/MUSIC/Singles/BlurSong2.mp3", False)

#def readygo():
#  sleep(1)
#  i01.mouth.audioFile.playFile("G:/MUSIC/Singles/Oakenfold - Ready Steady Go.mp3", False)

#def sabotage():
#  sleep(1)
#  i01.mouth.audioFile.playFile("G:/MUSIC/Singles/Beastie boys - SABOTAGE.mp3", False)

#def whatya():
#  sleep(1)
#  i01.mouth.audioFile.playFile("G:/MUSIC/Singles/Beastie Boys - So Watcha Want.mp3", False)

#def heroes():
#  sleep(1)
#  i01.mouth.audioFile.playFile("G:/MUSIC/Singles/David Bowie - Heroes.mp3", False)

#def doobies():
#  sleep(1)
#  i01.mouth.speak("let´s smoke a joint!")
#  sleep(1)
#  i01.mouth.audioFile.playFile("G:/MUSIC/Singles/Doobie Brothers - Long Train Running.mp3", False)

#def eels():
#  sleep(1)
#  i01.mouth.audioFile.playFile("G:/MUSIC/Singles/Eels  - Fresh Feeling.mp3", False)

#def eminem():
#  sleep(1)
#  i01.mouth.speak("here we go")
#  sleep(1)
#  i01.mouth.audioFile.playFile("G:/MUSIC/Singles/eminem 01 -lose yourself.mp3", False)

#def foo1():
#  sleep(1)
#  i01.mouth.audioFile.playFile("G:/MUSIC/Singles/Foo Fighters - all my life.mp3", False)

#def livingamerica():
#  sleep(1)
#  i01.mouth.speak("the good old james brown!")
#  sleep(1)
#  i01.mouth.audioFile.playFile("G:/MUSIC/Singles/James Brown - Living in America.mp3", False)

#def insanity():
#  sleep(1)
#  i01.mouth.audioFile.playFile("G:/MUSIC/Singles/Jamiroquai - Virtual Insanity.mp3", False)

#def priestlaw():
#  sleep(1)
#  i01.mouth.audioFile.playFile("G:/MUSIC/Singles/Judas Priest - Breaking the law.mp3", False)

###### info knowledge

def energy():
  sleep(2)
  i01.mouth.speakBlocking("I transfer you to my information database")
  sleep(1)
  i01.mouth.audioFile.playFile("G:/knowledge/energy.mp3", False)

def sun():
  sleep(2)
  i01.mouth.speakBlocking("I transfer you to my information database")
  sleep(1)
  i01.mouth.audioFile.playFile("G:/knowledge/Sun.mp3", False)

def robot():
  sleep(2)
  i01.mouth.speakBlocking("I transfer you to my information database")
  sleep(1)
  i01.mouth.audioFile.playFile("G:/knowledge/robot.mp3", False)

def earth():
  sleep(2)
  i01.mouth.speakBlocking("I transfer you to my information database")
  sleep(1)
  i01.mouth.audioFile.playFile("G:/knowledge/earth.mp3", False)

def solarsystem():
  sleep(2)
  i01.mouth.speakBlocking("I transfer you to my information database")
  sleep(1)
  i01.mouth.audioFile.playFile("G:/knowledge/solar system.mp3", False)

def powerdown():
        i01.powerDown()
        sleep(2)      
        ear.pauseListening()
        relax()
        i01.mouth.speakBlocking()
        sleep(2)
        rightSerialPort.digitalWrite(53, Arduino.LOW)
        leftSerialPort.digitalWrite(53, Arduino.LOW)
        ear.lockOutAllGrammarExcept("power up")
        sleep(2)
        ear.resumeListening()
 
def powerup():
        i01.powerUp()
        sleep(2)        
        ear.pauseListening()
        rightSerialPort.digitalWrite(53, Arduino.HIGH)
        leftSerialPort.digitalWrite(53, Arduino.HIGH)
        i01.mouth.speakBlocking("hello")
        relax()
        ear.clearLock()
        sleep(2)
        ear.resumeListening()