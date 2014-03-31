rightHand = runtime.createAndStart("rightHand","InMoovHand")
readDigitalPin = 8
serAtt = 1
rightHand.connect("COM12")
rightHand.startService() 
rightHand.close()
sleep(1)
rightHand.open()
sleep(1)
rightHand.openPinch()
sleep(1)
rightHand.closePinch()
sleep(1)
rightHand.rest()
 
ear = runtime.createAndStart("ear","Sphinx")
mouth = runtime.createAndStart("mouth", "Speech")
 
ear.attach(mouth)
ear.addCommand("count", "rightHand", "count")
ear.addCommand("victory", "rightHand", "victory")
ear.addCommand("hang ten", "rightHand", "hangTen")
ear.addCommand("devil horns", "rightHand", "devilHorns")
ear.addCommand("bird", "rightHand", "bird")
ear.addCommand("thumbs up", "rightHand", "thumbsUp")
 
ear.addCommand("close", "rightHand", "close")
ear.addCommand("close pinch", "rightHand", "closePinch")
ear.addCommand("open", "rightHand", "open")
ear.addCommand("open pinch", "rightHand", "openPinch")
ear.addCommand("attach", "rightHand", "attach")
ear.addCommand("detach", "rightHand", "detach")
 
ear.addCommand("manual", ear.getName(), "lockOutAllGrammarExcept", "voice control")
ear.addCommand("voice control", ear.getName(), "clearLock")
 
ear.addComfirmations("yes", "correct", "yeah", "ya")
ear.addNegations("no", "wrong", "nope", "nah")
 
# all commands MUST be before startListening
ear.startListening()



rightHand.arduino.setSampleRate(8000)
 
#start polling data from the digital pin
rightHand.arduino.digitalReadPollingStart(readDigitalPin)
#add python as listener of the arduino service, each time arduino publish the value of the pin
rightHand.arduino.addListener("publishPin", "python", "publishPin")
 
#define a function which is called every time arduino publish the value of the pin
def publishPin():
  global serAtt
  pin = msg_tracker_rightHand.arduino_publishPin.data[0]
  print pin.pin, pin.value,pin.type,pin.source
  #if an HIGH state is read, PIR is detecting something so start
  if (pin.value == 1):
   if not (serAtt==1):
    rightHand.attach()
    serAtt = 1
  #if a LOW state is read , stop
  elif (pin.value == 0):
   if not (serAtt == 0) :
    rightHand.detach()
    serAtt = 0