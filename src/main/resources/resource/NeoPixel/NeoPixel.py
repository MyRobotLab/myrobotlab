#########################################
# NeoPixel.py
# more info @: http://myrobotlab.org/service/NeoPixel
#########################################
# Example of controlling a NeoPixel
# NeoPixel is a strip of RGB LEDs
# in this example we are using a 256 pixel strip
# and an Arduino Mega.
# The Mega is connected to the NeoPixel strip
# via pin 3
# The Mega is connected to the computer via USB
# Onboard animations are available
# as well as the ability to set individual pixels
# [Stop, Theater Chase Rainbow, Rainbow, Larson Scanner, Flash Random,
# Theater Chase, Rainbow Cycle, Ironman, Color Wipe]
# There are now pre defined flashes which can be used
# [warn, speaking, heartbeat, success, pir, error, info]

from time import sleep

port = "/dev/ttyACM72"
pin = 3
pixelCount = 256

# starting mega
mega = runtime.start("mega", "Arduino")
mega.connect(port)

# starting neopixle
neopixel = runtime.start("neopixel", "NeoPixel")
neopixel.setPin(pin)
neopixel.setPixelCount(pixelCount)

# attach the two services
neopixel.attach(mega)

# brightness 0-255
neopixel.setBrightness(128)

# fuschia - setColor(R, G, B)
neopixel.setColor(120, 10, 30)

# Fun with flashing
print(neopixel.getFlashNames())

for flash in neopixel.getFlashNames():
    print('using flash', flash)
    neopixel.flash(flash)

# clear all pixels    
neopixel.clear()


# 1 to 50 Hz default is 10
neopixel.setSpeed(10)

# Fun with animations
# get a list of animations
print(neopixel.getAnimations())

for animation in neopixel.getAnimations():
    print(animation)
    neopixel.playAnimation(animation)
    sleep(3)

# clear all pixels
neopixel.clear()

neopixel.fill("cyan")
sleep(1)
neopixel.fill("yellow")
sleep(1)
neopixel.fill("pink")
sleep(1)
neopixel.fill("orange")
sleep(1)
neopixel.fill("black")
sleep(1)
neopixel.fill("magenta")
sleep(1)
neopixel.fill("green")
sleep(1)
neopixel.fill("#FFFFEE")
sleep(1)
neopixel.fill("#FF0000")
sleep(1)
neopixel.fill("#00FF00")
sleep(1)
neopixel.fill("#0000FF")
sleep(1)
neopixel.fill("#cccccc")
sleep(1)
neopixel.fill("#cc7528")
sleep(1)
neopixel.fill("#123456")
sleep(1)
neopixel.fill("#654321")
sleep(1)
neopixel.fill("#000000")

# if you want voice modulation of a neopixel this is one
# way to do it
# mouth = runtime.start('mouth', 'Polly')
# audio = runtime.start('mouth.audioFile', 'AudioFile')
# audio.addListener('publishPeak', 'neopixel')
# mouth.speak('Is my voice modulating the neopixel?')

print('done')    
    