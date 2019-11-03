#########################################
# Deeplearning4j.py
# description: A wrapper service for the Deeplearning4j framework.
# categories: ai
# more info @: http://myrobotlab.org/service/Deeplearning4j
#########################################

# start the service
deeplearning4j = Runtime.start('deeplearning4j','Deeplearning4j')

# load the VGG16 model from the zoo
deeplearning4j.loadVGG16()

# run an image file through the model and get the classifications / confidence
classifications = deeplearning4j.classifyImageFileVGG16("myimage.jpg")

# print them out... it's a dictionary/map
print classifications
