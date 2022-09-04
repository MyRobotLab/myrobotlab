#########################################
# NeoPixel.py
# more info @: http://myrobotlab.org/service/NeoPixel
#########################################
# new neopixel has some capability to have animations which can
# be custom created and run
# There are now 'service animations' and 'onboard animations'
#
# Service animations have some ability to be customized and saved,
#   each frame is sent over the serial line to the neopixel
#
# Onboard ones do not but are less chatty over the serial line
# 
# Animations
# stopAnimation       = 1
# colorWipe           = 2
# scanner             = 3
# theaterChase        = 4
# theaterChaseRainbow = 5
# rainbow             = 6
# rainbowCycle        = 7
# randomFlash         = 8
# ironman             = 9
# Runtime.setVirtual(True) # if you want no hardware
# port = "COM3"
port = "/dev/ttyACM0"
pin = 5
pixelCount = 8

# starting arduino
arduino = runtime.start("arduino","Arduino")
arduino.connect(port)

# starting neopixle
neopixel = runtime.start("neopixel","NeoPixel")
neopixel.setPin(pin)
neopixel.setPixelCount(pixelCount)

# attach the two services
neopixel.attach(arduino)

# fuschia - setColor(R, G, B)
neopixel.setColor(120, 10, 30)
# 1 to 50 Hz default is 10
neopixel.setSpeed(30) 

# start an animation
neopixel.playAnimation("Larson Scanner")
sleep(2)

# turquoise
neopixel.setColor(10, 120, 60)
sleep(2)

# start an animation
neopixel.playAnimation("Rainbow Cycle")
sleep(5)

neopixel.setColor(40, 20, 160)
neopixel.playAnimation("Color Wipe")
sleep(1)

neopixel.setColor(140, 20, 60)
sleep(1)

neopixel.clear()

# set individual pixels
# setPixel(address, R, G, B)
neopixel.setPixel(0, 40, 40, 0)
sleep(1)
neopixel.setPixel(1, 140, 40, 0)
sleep(1)
neopixel.setPixel(2, 40, 140, 0)
sleep(1)
neopixel.setPixel(2, 40, 0, 140)
sleep(1)

neopixel.clear()
neopixel.setColor(0, 40, 220)
neopixel.playAnimation("Ironman")
sleep(3)

# preset color and frequency values
neopixel.playIronman()
sleep(5)
neopixel.clear()
