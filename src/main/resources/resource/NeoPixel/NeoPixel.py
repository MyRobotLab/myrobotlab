#########################################
# NeoPixel.py
# more info @: http://myrobotlab.org/service/NeoPixel
#########################################

# virtual = True
port = "COM3"
# optional but recommended neopixel connected on a dedicated arduino
rxtxPort = "Serial2"

# start optional virtual arduino service, used for internal test
if ('virtual' in globals() and virtual):
    virtualArduino = Runtime.start("virtualArduino", "VirtualArduino")
    virtualArduino.connect(port)
# end used for internal test

#Starting Arduino Service
arduino = Runtime.start("arduino","Arduino")
arduino.setBoardMega() #or arduino.setBoardUno()
arduino.connect(port)

#Starting NeoPixel Service
neopixel = Runtime.start("neopixel","NeoPixel")

#neopixel.attach(arduino, pin, number of pixel)
if ('virtual' in globals() and virtual):
  #Attach Neopixel to main arduino
  neopixel.attach(arduino, 2, 16)
else:
  #Starting optional RX/TX connected slave arduino and Attach Neopixel to slave arduino
  arduinoNano = Runtime.start("arduinoNano","Arduino")
  arduinoNano.setBoardNano() #or arduino.setBoardUno()
  arduinoNano.connect(arduino,rxtxPort)
  neopixel.attach(arduinoNano, 2, 16)


#Animations;
#"Color Wipe"
#"Larson Scanner"
#"Theater Chase"
#"Theater Chase Rainbow"
#"Rainbow"
#"Rainbow Cycle"
#"Flash Random"
#"Ironman"

#speed: 1-65535   1=full speed, 2=2x slower than 1, 10=10x slower than 1
#starting a animation
#neopixel.setAnimation("Animation Name", red, green, blue, speed)
neopixel.setAnimation("Theater Chase", 255, 0, 0, 1) #running Theater Chase with color red at full speed

sleep(10)
neopixel.animationStop()

#run an animation with python script
#turn off all the pixels
for pixel in range (1,neopixel.numPixel + 1):
  neopixel.setPixel(pixel, 0, 0, 0)  #setPixel(pixel, red, green, blue)
neopixel.writeMatrix() #send the pixel data to the Neopixel hardware 
for loop in range(0,10): #do 10 loop
  for pixel in range(1, neopixel.numPixel +1):
    neopixel.setPixel(pixel, 255, 0, 0) #set the pixel to red
    neopixel.writeMatrix()
    sleep(0.03) #give a bit of delay before next step
    neopixel.setPixel(pixel, 0, 0, 0) #turn off the pixel
neopixel.writeMatrix()