#########################################
# ImageDisplay.py
# description: used as a general template
# more info @: http://myrobotlab.org/service/ImageDisplay
#########################################
 
#Display an image as it is. The string might be an internet source or path to an image on the computer.
imagedisplay = Runtime.start("imagedisplay","ImageDisplay")
imagedisplay.display("https://upload.wikimedia.org/wikipedia/commons/thumb/0/05/HONDA_ASIMO.jpg/800px-HONDA_ASIMO.jpg")
sleep(2)
#Closes all active images.
imagedisplay.closeAll()

#Display an image while fading it in at the beginning.
imagedisplay.displayFadeIn("https://upload.wikimedia.org/wikipedia/en/thumb/9/9b/FANUC_6-axis_welding_robots.jpg/800px-FANUC_6-axis_welding_robots.jpg")
sleep(2)
imagedisplay.closeAll()

#Display an image faded by a given value between 0 and 1.
imagedisplay.display("https://upload.wikimedia.org/wikipedia/commons/thumb/2/20/Bio-inspired_Big_Dog_quadruped_robot_is_being_developed_as_a_mule_that_can_traverse_difficult_terrain.tiff/lossy-page1-461px-Bio-inspired_Big_Dog_quadruped_robot_is_being_developed_as_a_mule_that_can_traverse_difficult_terrain.tiff.jpg", 0.1)
sleep(2)
imagedisplay.closeAll()

#Display an image scaled by a given multiplication factor.
imagedisplay.displayScaled("https://upload.wikimedia.org/wikipedia/commons/thumb/c/c0/Asimo_look_new_design.jpg/800px-Asimo_look_new_design.jpg", 2)
sleep(2)
imagedisplay.closeAll()

#Display an image faded faded by a given value between 0 and 1 and scaled by a given multiplication factor.
imagedisplay.displayScaled("https://upload.wikimedia.org/wikipedia/commons/thumb/c/c0/Asimo_look_new_design.jpg/800px-Asimo_look_new_design.jpg", 0.1 ,2)
sleep(2)
imagedisplay.closeAll()

#Display an image in FullScreen Mode (Fullscreenmode can be terminated with a mouseclick.
imagedisplay.displayFullScreen("https://upload.wikimedia.org/wikipedia/commons/f/fe/Escher_Cube.png")
sleep(2)
imagedisplay.closeAll()

#Display an image in FullScreen Mode faded by a given value between 0 and 1. (Fullscreenmode can be terminated with a mouseclick.
imagedisplay.displayFullScreen("https://upload.wikimedia.org/wikipedia/commons/f/fe/Escher_Cube.png", 0.1)
sleep(2)
#Method to exit Fullscreen but keep the image.
imagedisplay.exitFS()
sleep(2)
imagedisplay.closeAll()

#Get the resolutions of the current Display.
print (imagedisplay.getResolutionOfW())
print (imagedisplay.getResolutionOfH())

