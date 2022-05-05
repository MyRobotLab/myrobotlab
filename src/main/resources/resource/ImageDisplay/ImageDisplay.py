#########################################
# ImageDisplay.py
# description: used as a general template
# more info @: http:#myrobotlab.org/service/ImageDisplay
#########################################
from time import sleep
import math

# Runtime.setConfig("default")

# start a image display service
display = runtime.start("display", "ImageDisplay")

# =========== Default Values ==================
# setting these are setting default values
# so that any new display is created they will
# have the following properties automatically set accordingly
#

# the display will always be on top
# display.setAlwaysOnTop(true)

# when a picture is told to go fullscreen
# and is not the same ratio as the screen dimensions
# this tells whether to scale and extend to the min
# or max extension
# display.setAutoscaleExtendsMax(true)

# if there is a background while fullscreen - set the color rgb
# display.setColor("#000000")

# if true will resize image (depending on setAutoscaleExtendsMax)
# display.setFullScreen(false)

# set which screen device to be displayed on
# display.setScreen(0)

# set the default location for images to display
# null values will mean image will be positioned in the center of the screen
# display.setLocation(null, null)

# set the default dimensions for images to display
# null values will be the dimensions of the original image
# display.setDimension(null, null)

# most basic display - an image file, can be relative or absolute file path
# displays are named - if you don't name them - they're name will be "default"
# this creates a display named default and display a snake.jpg
display.display("snake.jpg")
sleep(1)

# this creates a new display called "beetle" and loads it with beetle.jpg
# "default" display is still snake.jpg
display.display("beetle", "beetle.jpg")
sleep(1)

# the image display service can also display images from the web
# just supply the full url - they can be named as well - this one replaces the snake image
# since a name was not specified - its loaded into "default"
display.display(
    "https://upload.wikimedia.org/wikipedia/commons/thumb/8/87/InMoov_Wheel_1.jpg/220px-InMoov_Wheel_1.jpg")
sleep(1)

# animated gifs can be displayed as well - this is the earth
# in fullscreen mode
display.displayFullScreen(
    "earth", "https://upload.wikimedia.org/wikipedia/commons/thumb/2/2c/Rotating_earth_%28large%29.gif/300px-Rotating_earth_%28large%29.gif")
sleep(1)

# we can resize a picture
display.resize("earth", 600, 600)
sleep(1)

# and re-position it
display.move("earth", 800, 800)

# make another picture go fullscreen
display.displayFullScreen("robot", "https://upload.wikimedia.org/wikipedia/commons/thumb/9/9b/FANUC_6-axis_welding_robots.jpg/1280px-FANUC_6-axis_welding_robots.jpg")
sleep(1)

display.display("monkeys", "https://upload.wikimedia.org/wikipedia/commons/e/e8/Gabriel_Cornelius_von_Max%2C_1840-1915%2C_Monkeys_as_Judges_of_Art%2C_1889.jpg")
sleep(1)

display.displayFullScreen("robot", "https://upload.wikimedia.org/wikipedia/commons/thumb/9/9b/FANUC_6-axis_welding_robots.jpg/1280px-FANUC_6-axis_welding_robots.jpg")
sleep(1)

display.display("inmoov", "https://upload.wikimedia.org/wikipedia/commons/thumb/8/87/InMoov_Wheel_1.jpg/220px-InMoov_Wheel_1.jpg")
sleep(1)

display.display("mrl", "https://upload.wikimedia.org/wikipedia/commons/thumb/1/19/VirtualInMoov.jpg/220px-VirtualInMoov.jpg")
sleep(1)

for i in range(0, 100):
    display.move("monkeys", 20 + i, 20 + i)
    sleep(0.05)

display.resize("monkeys", 200, 200)
sleep(1)

display.move("monkeys", 30, 30)
sleep(1)

# now we can close some of the displays
display.close("monkeys")
sleep(1)

display.close("robot")
sleep(1)

x0 = 500
y0 = 500
r = 300
x = 0
y = 0

# move the inmoov image in a circle on the screen
for t in range(0, int(4 * math.pi)):
    x = int(r * math.cos(t) + x0)
    y = int(r * math.sin(t) + y0)
    display.move("inmoov", x, y)
    sleep(0.1)


# in this example we will search for images and display them
# start a google search and get the images back, then display them
google = runtime.start("google", "GoogleSearch")
images = google.imageSearch("monkey")
for img in images:
    display.displayFullScreen(img)
    # display.display(img)
    sleep(1)

# set defaults to be fullscree and autoscale extends max for all
# new displays
display.setFullScreen(True)
display.setAutoscaleExtendsMax(True)

# another example we'll use wikipedia service to search
# and attach the wikipedia to the display service
# it will automagically display when an image is found
wikipedia = runtime.start("wikipedia", "Wikipedia")
wikipedia.attach(display)
# display.attach(wikipedia)
images = wikipedia.imageSearch("bear")
sleep(2)
display.setFullScreen(False)
display.setAutoscaleExtendsMax(False)
display.displayFullScreen("https://upload.wikimedia.org/wikipedia/commons/thumb/5/51/Noto_Emoji_Pie_1f4e2.svg/512px-Noto_Emoji_Pie_1f4e2.svg.png?20190227024729")
sleep(1)

display.display("data/Emoji/512px/U+1F47D.png")
sleep(1)

display.display("https://raw.githubusercontent.com/googlefonts/noto-emoji/main/png/512/emoji_u1f62c.png")
sleep(1)
display.displayFullScreen("https://upload.wikimedia.org/wikipedia/commons/thumb/5/51/Noto_Emoji_Pie_1f4e2.svg/512px-Noto_Emoji_Pie_1f4e2.svg.png?20190227024729")
sleep(1)
display.display("dino", "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1c/Noto_Emoji_Pie_1f995.svg/512px-Noto_Emoji_Pie_1f995.svg.png?20190227143252")
sleep(1)

# save all displays in their current state
display.save()

sleep(5)
# close everything
display.closeAll()

# close everything and reset defaults
display.reset()

# 
# opencv selected image - or object identified image


# Display an image as it is. The string might be an internet source or path to an image on the computer.
display.display("asimo", "https://upload.wikimedia.org/wikipedia/commons/thumb/0/05/HONDA_ASIMO.jpg/800px-HONDA_ASIMO.jpg")
sleep(2)
# Closes all active images.
display.closeAll()

# Display an image scaled by a given multiplication factor.
display.displayScaled("https://upload.wikimedia.org/wikipedia/commons/thumb/c/c0/Asimo_look_new_design.jpg/800px-Asimo_look_new_design.jpg", 2)
sleep(2)
display.closeAll()

# Display an image faded faded by a given value between 0 and 1 and scaled by a given multiplication factor.
display.displayScaled("https://upload.wikimedia.org/wikipedia/commons/thumb/c/c0/Asimo_look_new_design.jpg/800px-Asimo_look_new_design.jpg", 0.1, 2)
sleep(2)
display.closeAll()

# Display an image in FullScreen Mode (Fullscreenmode can be terminated with a mouseclick.
display.displayFullScreen("https://upload.wikimedia.org/wikipedia/commons/f/fe/Escher_Cube.png")
sleep(2)
display.closeAll()

# Display an image in FullScreen Mode faded by a given value between 0 and 1. (Fullscreenmode can be terminated with a mouseclick.
display.displayFullScreen("https://upload.wikimedia.org/wikipedia/commons/f/fe/Escher_Cube.png", 0.1)
sleep(2)
display.closeAll()

# opencv example - shows how to display mutliple saved images
# with greater control in the image display service - although
# its only displaying 3 images here - they could be moved and a row
# of captured images based on classification could be reported
# start a image display service
display = runtime.start("display", "ImageDisplay")
cv = runtime.start('cv','OpenCV')
cv.capture()
sleep(5)
cv.attach(display)
sleep(4)
cv.saveImage()
sleep(1)
cv.saveImage()
sleep(1)
cv.saveImage()
